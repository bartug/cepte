/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.service.impl;

import com.bartugsevindik.cepte.backend.identity.CurrentUser;
import com.bartugsevindik.cepte.backend.identity.DeviceBoundEvent;
import com.bartugsevindik.cepte.backend.identity.UserRegisteredEvent;
import com.bartugsevindik.cepte.backend.identity.dto.AuthTokensDTO;
import com.bartugsevindik.cepte.backend.identity.dto.BiometricEnableRequest;
import com.bartugsevindik.cepte.backend.identity.dto.DeviceBindRequest;
import com.bartugsevindik.cepte.backend.identity.dto.MeDTO;
import com.bartugsevindik.cepte.backend.identity.dto.RegisterRequest;
import com.bartugsevindik.cepte.backend.identity.entity.AppUser;
import com.bartugsevindik.cepte.backend.identity.entity.Device;
import com.bartugsevindik.cepte.backend.identity.enums.AuthEventType;
import com.bartugsevindik.cepte.backend.identity.enums.DeviceStatus;
import com.bartugsevindik.cepte.backend.identity.enums.TokenRevokeReason;
import com.bartugsevindik.cepte.backend.identity.enums.UserStatus;
import com.bartugsevindik.cepte.backend.identity.repository.AppUserRepository;
import com.bartugsevindik.cepte.backend.identity.repository.DeviceRepository;
import com.bartugsevindik.cepte.backend.identity.security.DeviceSignature;
import com.bartugsevindik.cepte.backend.identity.security.VerifiedDevice;
import com.bartugsevindik.cepte.backend.identity.service.AuthEventService;
import com.bartugsevindik.cepte.backend.identity.service.AuthService;
import com.bartugsevindik.cepte.backend.identity.service.OtpService;
import com.bartugsevindik.cepte.backend.identity.service.PinService;
import com.bartugsevindik.cepte.backend.identity.service.TokenService;
import com.bartugsevindik.cepte.common.exception.BadRequestException;
import com.bartugsevindik.cepte.common.exception.ConflictException;
import com.bartugsevindik.cepte.common.exception.ForbiddenException;
import com.bartugsevindik.cepte.common.exception.NotFoundException;
import com.bartugsevindik.cepte.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AppUserRepository appUserRepository;
    private final DeviceRepository deviceRepository;
    private final OtpService otpService;
    private final PinService pinService;
    private final TokenService tokenService;
    private final AuthEventService authEventService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /**
     * <h1>Kayıt</h1>
     * <p>PIN zayıfsa ya da anahtar geçersizse işlem geri alınır, bilet kullanılmamış sayılır; kullanıcı aynı biletle
     * tekrar deneyebilir.</p>
     *
     * @param request Doğrulama bileti, PIN ve cihaz anahtarı
     * @return Oturum
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Override
    @Transactional
    public AuthTokensDTO register(@NotNull RegisterRequest request) {
        String phone = otpService.consumeVerification(request.verificationToken());
        if (appUserRepository.existsByPhone(phone)) {
            throw new ConflictException("Bu numara zaten kayıtlı. Giriş yaparak devam et.");
        }
        pinService.validateStrength(request.pin());
        validatePublicKey(request.devicePublicKey());

        AppUser user = appUserRepository.save(AppUser.builder()
                .phone(phone)
                .status(UserStatus.ACTIVE)
                .pinHash(pinService.hash(request.pin()))
                .failedPinAttempts(0)
                .pinLockCount(0)
                .build());
        Device device = saveDevice(user.getId(), request.devicePublicKey(), request.deviceName());

        authEventService.record(AuthEventType.REGISTERED, user.getId(), device.getId(), phone, null);
        eventPublisher.publishEvent(new UserRegisteredEvent(user.getId(), phone, Instant.now(clock)));
        log.info("User registered. userId={}, deviceId={}", user.getId(), device.getId());
        return tokenService.issue(user.getId(), device.getId());
    }

    /**
     * <h1>Yeni Cihaz Bağlama</h1>
     * <p>SIM kopyalayan biri OTP'yi alabilir ama PIN'i bilmez. PIN hatalıysa bilet geri alınmaz ama deneme sayılır;
     * 5 hatada PIN kilitlenir.</p>
     *
     * @param request Doğrulama bileti, PIN ve cihaz anahtarı
     * @return Oturum
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Override
    @Transactional
    public AuthTokensDTO bindDevice(@NotNull DeviceBindRequest request) {
        String phone = otpService.consumeVerification(request.verificationToken());
        AppUser user = appUserRepository.findByPhone(phone)
                .orElseThrow(() -> new NotFoundException("Bu numarayla kayıtlı hesap yok. Kayıt olarak devam et."));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ForbiddenException("Hesap kullanıma kapalı. Müşteri hizmetleriyle iletişime geç.");
        }
        pinService.verify(user.getId(), null, request.pin());
        validatePublicKey(request.devicePublicKey());

        Instant now = Instant.now(clock);
        List<Long> revoked = new ArrayList<>();
        for (Device old : deviceRepository.findAllByUserIdAndStatus(user.getId(), DeviceStatus.ACTIVE)) {
            old.setStatus(DeviceStatus.REVOKED);
            old.setRevokedAt(now);
            tokenService.revokeDevice(old.getId(), TokenRevokeReason.NEW_DEVICE);
            revoked.add(old.getId());
        }
        // Kısmi unique index (kullanıcı başına tek ACTIVE) yeni kayıttan önce eski cihazların kapanmasını ister
        deviceRepository.flush();
        Device device = saveDevice(user.getId(), request.devicePublicKey(), request.deviceName());

        authEventService.record(AuthEventType.DEVICE_BOUND, user.getId(), device.getId(), phone, "revoked=" + revoked);
        eventPublisher.publishEvent(new DeviceBoundEvent(user.getId(), device.getId(), revoked, now));
        log.info("Device bound. userId={}, deviceId={}, revoked={}", user.getId(), device.getId(), revoked);
        return tokenService.issue(user.getId(), device.getId());
    }

    /**
     * <h1>PIN ile Giriş</h1>
     *
     * @param device İmzası doğrulanmış cihaz
     * @param pin    PIN
     * @return Oturum
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Override
    @Transactional
    public AuthTokensDTO loginWithPin(@NotNull VerifiedDevice device, @NotNull String pin) {
        pinService.verify(device.userId(), device.deviceId(), pin);
        authEventService.record(AuthEventType.LOGIN_SUCCESS, device.userId(), device.deviceId(), null, "method=PIN");
        return tokenService.issue(device.userId(), device.deviceId());
    }

    /**
     * <h1>Face ID ile Giriş</h1>
     * <p>Face ID sonucu backend'e gelmez; biyometrik anahtar sadece Face ID başarılıysa imza atar. Backend imzaya bakar.</p>
     *
     * @param device İmzası doğrulanmış cihaz
     * @return Oturum
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Override
    @Transactional
    public AuthTokensDTO loginWithBiometric(@NotNull VerifiedDevice device) {
        if (!device.biometricVerified()) {
            throw new UnauthorizedException("Face ID doğrulanamadı. PIN ile giriş yap.");
        }
        authEventService.record(AuthEventType.LOGIN_SUCCESS, device.userId(), device.deviceId(), null, "method=BIOMETRIC");
        return tokenService.issue(device.userId(), device.deviceId());
    }

    /**
     * <h1>Token Yenileme</h1>
     *
     * @param device       İmzası doğrulanmış cihaz
     * @param refreshToken Refresh token
     * @return Yeni token'lar
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Override
    public AuthTokensDTO refresh(@NotNull VerifiedDevice device, @NotNull String refreshToken) {
        return tokenService.rotate(refreshToken, device.deviceId());
    }

    /**
     * <h1>Face ID ile Girişi Açma</h1>
     * <p>İstek hem token'la hem cihaz imzasıyla gelir; ikisi aynı cihazı göstermeli.</p>
     *
     * @param user    Oturum sahibi
     * @param device  İmzası doğrulanmış cihaz
     * @param request Biyometrik anahtar
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Override
    @Transactional
    public void enableBiometric(@NotNull CurrentUser user, @NotNull VerifiedDevice device, @NotNull BiometricEnableRequest request) {
        if (!user.deviceId().equals(device.deviceId())) {
            throw new UnauthorizedException("Cihaz imzası oturumla uyuşmuyor.");
        }
        validatePublicKey(request.publicKey());
        Device entity = activeDevice(user.deviceId());
        entity.setBiometricPublicKey(request.publicKey());
        authEventService.record(AuthEventType.BIOMETRIC_ENABLED, user.userId(), user.deviceId(), null, null);
    }

    /**
     * <h1>Face ID ile Girişi Kapatma</h1>
     *
     * @param user Oturum sahibi
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Override
    @Transactional
    public void disableBiometric(@NotNull CurrentUser user) {
        activeDevice(user.deviceId()).setBiometricPublicKey(null);
        authEventService.record(AuthEventType.BIOMETRIC_DISABLED, user.userId(), user.deviceId(), null, null);
    }

    /**
     * <h1>Çıkış</h1>
     * <p>Sadece bu oturum ailesi kapanır; cihaz bağlı kalır, PIN ile tekrar girilebilir.</p>
     *
     * @param user Oturum sahibi
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Override
    @Transactional
    public void logout(@NotNull CurrentUser user) {
        tokenService.revokeFamily(user.familyId(), TokenRevokeReason.LOGOUT);
        authEventService.record(AuthEventType.LOGOUT, user.userId(), user.deviceId(), null, null);
    }

    /**
     * <h1>Oturum Sahibi Bilgisi</h1>
     *
     * @param user Oturum sahibi
     * @return Kullanıcı ve cihaz bilgisi
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Override
    @Transactional(readOnly = true)
    public MeDTO getMe(@NotNull CurrentUser user) {
        AppUser appUser = appUserRepository.findById(user.userId())
                .orElseThrow(() -> new NotFoundException("Kullanıcı bulunamadı."));
        Device device = activeDevice(user.deviceId());
        return new MeDTO(appUser.getId(), maskPhone(appUser.getPhone()), device.getId(), device.getName(),
                device.getBiometricPublicKey() != null, appUser.getCreatedDate());
    }

    private Device saveDevice(Long userId, String publicKey, String name) {
        return deviceRepository.save(Device.builder()
                .userId(userId)
                .publicKey(publicKey)
                .name(name)
                .status(DeviceStatus.ACTIVE)
                .boundAt(Instant.now(clock))
                .build());
    }

    private Device activeDevice(Long deviceId) {
        return deviceRepository.findByIdAndStatus(deviceId, DeviceStatus.ACTIVE)
                .orElseThrow(() -> new UnauthorizedException("Cihaz artık aktif değil."));
    }

    private static void validatePublicKey(String publicKey) {
        try {
            DeviceSignature.parsePublicKey(publicKey);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new BadRequestException("Cihaz anahtarı geçersiz.");
        }
    }

    // +905321234567 → +90 532 *** ** 67
    private static String maskPhone(String phone) {
        return "+90 " + phone.substring(3, 6) + " *** ** " + phone.substring(11);
    }
}
