/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity;

import org.jetbrains.annotations.NotNull;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/**
 * <h1>CurrentUser</h1>
 * <p>Access token'dan okunan oturum sahibi. Diğer modüllerin controller'ları
 * {@code @AuthenticationPrincipal Jwt jwt} ile alıp {@link #from(Jwt)} ile çevirir.</p>
 *
 * @param userId   Kullanıcı
 * @param deviceId Token'ın verildiği cihaz
 * @param familyId Refresh token ailesi; çıkışta bu aile iptal edilir
 * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 * @since 23.10.2026 - CP-2
 */
public record CurrentUser(Long userId, Long deviceId, UUID familyId) {

    public static final String DEVICE_CLAIM = "did";
    public static final String FAMILY_CLAIM = "fam";

    public static CurrentUser from(@NotNull Jwt jwt) {
        return new CurrentUser(Long.valueOf(jwt.getSubject()),
                Long.valueOf(jwt.getClaimAsString(DEVICE_CLAIM)),
                UUID.fromString(jwt.getClaimAsString(FAMILY_CLAIM)));
    }
}
