/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.service.impl;

import com.bartugsevindik.cepte.backend.identity.config.IdentityProperties;
import com.bartugsevindik.cepte.backend.identity.dto.OtpChallengeDTO;
import com.bartugsevindik.cepte.backend.identity.dto.OtpVerificationDTO;
import com.bartugsevindik.cepte.backend.identity.entity.OtpChallenge;
import com.bartugsevindik.cepte.backend.identity.enums.AuthEventType;
import com.bartugsevindik.cepte.backend.identity.enums.OtpStatus;
import com.bartugsevindik.cepte.backend.identity.repository.AppUserRepository;
import com.bartugsevindik.cepte.backend.identity.repository.OtpChallengeRepository;
import com.bartugsevindik.cepte.backend.identity.security.Secrets;
import com.bartugsevindik.cepte.backend.identity.service.AuthEventService;
import com.bartugsevindik.cepte.backend.identity.service.OtpService;
import com.bartugsevindik.cepte.backend.notification.SmsSender;
import com.bartugsevindik.cepte.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
    private static final int CODE_LENGTH = 6;
    private static final String INVALID_CODE = "Kod hatalı ya da süresi dolmuş. Yeni kod iste.";

    private final OtpChallengeRepository otpChallengeRepository;
    private final AppUserRepository appUserRepository;
    private final OtpRateLimiter rateLimiter;
    private final SmsSender smsSender;
    private final AuthEventService authEventService;
    private final IdentityProperties properties;
    private final Clock clock;

    /**
     * <h1>Doğrulama Kodu Gönderme</h1>
     * <p>SMS gönderilemezse kayıt geri alınır ve gönderim sayılmaz.</p>
     *
     * @param phone E.164 telefon
     * @param ip    İstemci IP'si
     * @return Kod bilgisi
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Override
    @Transactional
    public OtpChallengeDTO requestOtp(@NotNull String phone, String ip) {
        rateLimiter.check(phone, ip);

        IdentityProperties.Otp otp = properties.getOtp();
        UUID challengeId = UUID.randomUUID();
        String code = Secrets.randomDigits(CODE_LENGTH);
        otpChallengeRepository.save(OtpChallenge.builder()
                .challengeId(challengeId)
                .phone(phone)
                .ip(ip)
                .codeHash(codeHash(challengeId, code))
                .attempts(0)
                .status(OtpStatus.PENDING)
                .expiresAt(Instant.now(clock).plus(otp.getTtl()))
                .build());

        smsSender.send(phone, "Cepte doğrulama kodun: " + code + ". Bu kodu kimseyle paylaşma; Cepte çalışanları kodunu asla sormaz.");
        rateLimiter.recordSent(phone, ip);
        authEventService.record(AuthEventType.OTP_SENT, null, null, phone, null);
        return new OtpChallengeDTO(challengeId, otp.getTtl().toSeconds(), otp.getResendCooldown().toSeconds());
    }

    /**
     * <h1>Doğrulama Kodu Kontrolü</h1>
     * <p>Yanlış denemeler hata dönse de sayılır (noRollbackFor). En fazla 5 deneme; sonra yeni kod gerekir.
     * 6 haneli bir kodun 10^6 ihtimali var, deneme sınırı olmadan dakikalar içinde bulunurdu.</p>
     *
     * @param challengeId Kod isteğinin id'si
     * @param code        Kullanıcının girdiği kod
     * @return Tek kullanımlık doğrulama bileti
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Override
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public OtpVerificationDTO verifyOtp(@NotNull UUID challengeId, @NotNull String code) {
        OtpChallenge challenge = otpChallengeRepository.findByChallengeIdForUpdate(challengeId)
                .orElseThrow(() -> new UnauthorizedException(INVALID_CODE));
        Instant now = Instant.now(clock);
        if (challenge.getStatus() != OtpStatus.PENDING) {
            throw new UnauthorizedException(INVALID_CODE);
        }
        if (!now.isBefore(challenge.getExpiresAt())) {
            challenge.setStatus(OtpStatus.EXPIRED);
            throw new UnauthorizedException(INVALID_CODE);
        }

        if (!Secrets.constantTimeEquals(challenge.getCodeHash(), codeHash(challengeId, code))) {
            int attempts = challenge.getAttempts() + 1;
            challenge.setAttempts(attempts);
            authEventService.record(AuthEventType.OTP_FAILED, null, null, challenge.getPhone(), "attempt=" + attempts);
            int remaining = properties.getOtp().getMaxAttempts() - attempts;
            if (remaining <= 0) {
                challenge.setStatus(OtpStatus.EXHAUSTED);
                throw new UnauthorizedException("Çok fazla hatalı deneme yapıldı. Yeni kod iste.");
            }
            throw new UnauthorizedException("Kod hatalı. Kalan deneme hakkı: " + remaining);
        }

        String verificationToken = Secrets.randomToken();
        challenge.setStatus(OtpStatus.VERIFIED);
        challenge.setVerifiedAt(now);
        challenge.setVerificationHash(Secrets.sha256Hex(verificationToken));
        challenge.setVerificationExpiresAt(now.plus(properties.getOtp().getVerificationTtl()));
        authEventService.record(AuthEventType.OTP_VERIFIED, null, null, challenge.getPhone(), null);

        // Telefonun sahibi olduğu kanıtlandı; numaranın kayıtlı olduğunu söylemek artık bilgi sızdırmak değil
        boolean registered = appUserRepository.existsByPhone(challenge.getPhone());
        return new OtpVerificationDTO(verificationToken, registered, properties.getOtp().getVerificationTtl().toSeconds());
    }

    /**
     * <h1>Doğrulama Biletini Kullanma</h1>
     *
     * @param verificationToken Bilet
     * @return Doğrulanmış telefon
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public String consumeVerification(@NotNull String verificationToken) {
        OtpChallenge challenge = otpChallengeRepository.findByVerificationHashForUpdate(Secrets.sha256Hex(verificationToken))
                .filter(c -> c.getStatus() == OtpStatus.VERIFIED)
                .filter(c -> Instant.now(clock).isBefore(c.getVerificationExpiresAt()))
                .orElseThrow(() -> new UnauthorizedException("Doğrulama süresi doldu. Tekrar kod iste."));
        challenge.setStatus(OtpStatus.CONSUMED);
        challenge.setConsumedAt(Instant.now(clock));
        return challenge.getPhone();
    }

    private String codeHash(UUID challengeId, String code) {
        return Secrets.hmacSha256Hex(properties.getOtp().getHashSecret(), challengeId + ":" + code);
    }
}
