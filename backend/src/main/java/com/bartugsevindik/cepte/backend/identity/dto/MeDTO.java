/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Oturum sahibi")
public record MeDTO(
        @Schema(description = "Kullanıcı") Long userId,
        @Schema(description = "Maskeli telefon", example = "+90 532 *** ** 67") String maskedPhone,
        @Schema(description = "Bu cihaz") Long deviceId,
        @Schema(description = "Cihaz adı", example = "iPhone 17 Pro") String deviceName,
        @Schema(description = "Face ID ile giriş açık mı") boolean biometricEnabled,
        @Schema(description = "Kayıt tarihi") Instant registeredAt
) {
}
