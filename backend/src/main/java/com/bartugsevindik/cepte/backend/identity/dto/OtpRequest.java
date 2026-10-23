/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Doğrulama kodu isteği")
public record OtpRequest(
        @Schema(description = "E.164 formatında Türkiye cep telefonu", example = "+905321234567")
        @NotBlank
        @Pattern(regexp = "^\\+905\\d{9}$", message = "+905XXXXXXXXX formatında olmalı")
        String phone
) {
}
