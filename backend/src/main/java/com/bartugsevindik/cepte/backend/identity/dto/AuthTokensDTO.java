/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Oturum")
public record AuthTokensDTO(
        @Schema(description = "Kullanıcı") Long userId,
        @Schema(description = "Bu cihaz. İmzalı isteklerde X-Device-Id olarak gönderilir") Long deviceId,
        @Schema(description = "Access token (JWT, ES256)") String accessToken,
        @Schema(description = "Access token'ın bitişi") Instant accessTokenExpiresAt,
        @Schema(description = "Refresh token. Tek kullanımlık; her yenilemede yenisi döner") String refreshToken,
        @Schema(description = "Refresh token'ın bitişi") Instant refreshTokenExpiresAt
) {
}
