/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.service.impl;

import com.bartugsevindik.cepte.backend.identity.CurrentUser;
import com.bartugsevindik.cepte.backend.identity.config.IdentityProperties;
import com.bartugsevindik.cepte.backend.identity.dto.AuthTokensDTO;
import com.bartugsevindik.cepte.backend.identity.entity.RefreshToken;
import com.bartugsevindik.cepte.backend.identity.enums.AuthEventType;
import com.bartugsevindik.cepte.backend.identity.enums.TokenRevokeReason;
import com.bartugsevindik.cepte.backend.identity.repository.RefreshTokenRepository;
import com.bartugsevindik.cepte.backend.identity.security.Secrets;
import com.bartugsevindik.cepte.backend.identity.service.AuthEventService;
import com.bartugsevindik.cepte.backend.identity.service.TokenService;
import com.bartugsevindik.cepte.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtEncoder jwtEncoder;
    private final AuthEventService authEventService;
    private final IdentityProperties properties;
    private final Clock clock;

    /**
     * <h1>Oturum Açma</h1>
     *
     * @param userId   Kullanıcı
     * @param deviceId Cihaz
     * @return Token'lar
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Override
    @Transactional
    public AuthTokensDTO issue(@NotNull Long userId, @NotNull Long deviceId) {
        refreshTokenRepository.revokeDevice(deviceId, TokenRevokeReason.NEW_LOGIN, Instant.now(clock));
        return createTokens(userId, deviceId, UUID.randomUUID(), null);
    }

    /**
     * <h1>Token Yenileme</h1>
     * <p>Kopyalanmış token tespitinde aile iptali hata dönse de kalıcı olmalı (noRollbackFor).
     * Token başka bir cihazdan geldiyse de aile kapatılır: token çalınmış demektir.</p>
     *
     * @param refreshToken Refresh token
     * @param deviceId     İsteği imzalayan cihaz
     * @return Yeni token'lar
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Override
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthTokensDTO rotate(@NotNull String refreshToken, @NotNull Long deviceId) {
        RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(Secrets.sha256Hex(refreshToken))
                .orElseThrow(() -> new UnauthorizedException("Oturum bulunamadı. Tekrar giriş yap."));
        Instant now = Instant.now(clock);

        if (current.getRevokedAt() != null) {
            throw new UnauthorizedException("Oturum sonlandırılmış. Tekrar giriş yap.");
        }
        if (current.getUsedAt() != null) {
            refreshTokenRepository.revokeFamily(current.getFamilyId(), TokenRevokeReason.REUSE_DETECTED, now);
            authEventService.record(AuthEventType.REFRESH_REUSE_DETECTED, current.getUserId(), deviceId, null,
                    "family=" + current.getFamilyId());
            log.warn("Refresh token reuse detected, family revoked. userId={}, family={}", current.getUserId(), current.getFamilyId());
            throw new UnauthorizedException("Oturum güvenlik nedeniyle sonlandırıldı. Tekrar giriş yap.");
        }
        if (!current.getDeviceId().equals(deviceId)) {
            refreshTokenRepository.revokeFamily(current.getFamilyId(), TokenRevokeReason.DEVICE_MISMATCH, now);
            authEventService.record(AuthEventType.REFRESH_DEVICE_MISMATCH, current.getUserId(), deviceId, null,
                    "tokenDevice=" + current.getDeviceId());
            throw new UnauthorizedException("Oturum güvenlik nedeniyle sonlandırıldı. Tekrar giriş yap.");
        }
        if (!now.isBefore(current.getExpiresAt())) {
            throw new UnauthorizedException("Oturumun süresi doldu. Tekrar giriş yap.");
        }

        current.setUsedAt(now);
        return createTokens(current.getUserId(), current.getDeviceId(), current.getFamilyId(), current.getId());
    }

    /**
     * <h1>Oturum Ailesini Kapatma</h1>
     *
     * @param familyId Aile
     * @param reason   Sebep
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Override
    @Transactional
    public void revokeFamily(@NotNull UUID familyId, @NotNull TokenRevokeReason reason) {
        refreshTokenRepository.revokeFamily(familyId, reason, Instant.now(clock));
    }

    /**
     * <h1>Cihazın Tüm Oturumlarını Kapatma</h1>
     *
     * @param deviceId Cihaz
     * @param reason   Sebep
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Override
    @Transactional
    public void revokeDevice(@NotNull Long deviceId, @NotNull TokenRevokeReason reason) {
        refreshTokenRepository.revokeDevice(deviceId, reason, Instant.now(clock));
    }

    private AuthTokensDTO createTokens(Long userId, Long deviceId, UUID familyId, Long parentId) {
        Instant now = Instant.now(clock);
        IdentityProperties.Token token = properties.getToken();
        String refreshToken = Secrets.randomToken();
        Instant refreshExpiresAt = now.plus(token.getRefreshTtl());
        refreshTokenRepository.save(RefreshToken.builder()
                .familyId(familyId)
                .tokenHash(Secrets.sha256Hex(refreshToken))
                .userId(userId)
                .deviceId(deviceId)
                .parentId(parentId)
                .expiresAt(refreshExpiresAt)
                .build());

        Instant accessExpiresAt = now.plus(token.getAccessTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(token.getIssuer())
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(accessExpiresAt)
                .id(UUID.randomUUID().toString())
                .claim(CurrentUser.DEVICE_CLAIM, deviceId.toString())
                .claim(CurrentUser.FAMILY_CLAIM, familyId.toString())
                .build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.ES256).build(), claims))
                .getTokenValue();
        return new AuthTokensDTO(userId, deviceId, accessToken, accessExpiresAt, refreshToken, refreshExpiresAt);
    }
}
