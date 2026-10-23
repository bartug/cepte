/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.sim.sms.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Simülatörün gelen kutusundaki SMS")
public record SmsDTO(
        @Schema(description = "Alıcı", example = "+905321234567") String phone,
        @Schema(description = "Mesaj") String text,
        @Schema(description = "Mesajdaki 6 haneli kod, varsa", example = "482915") String code,
        @Schema(description = "Simülatör saatiyle gönderim anı") Instant sentAt
) {
}
