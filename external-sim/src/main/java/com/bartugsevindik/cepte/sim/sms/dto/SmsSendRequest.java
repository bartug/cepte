/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.sim.sms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "SMS gönderim isteği (backend'den gelir)")
public record SmsSendRequest(
        @Schema(description = "Alıcı", example = "+905321234567") @NotBlank String phone,
        @Schema(description = "Mesaj", example = "Cepte doğrulama kodun: 482915.") @NotBlank @Size(max = 500) String text
) {
}
