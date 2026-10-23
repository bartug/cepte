/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

@Schema(description = "Doğrulama kodu")
public record OtpVerifyRequest(
        @Schema(description = "Kod isteğinde dönen id", example = "3f1c7c1e-5e7b-4a5f-9b0e-2f5c1f8d9a10")
        @NotNull
        UUID challengeId,

        @Schema(description = "SMS ile gelen 6 haneli kod", example = "482915")
        @NotBlank
        @Pattern(regexp = "^\\d{6}$", message = "6 haneli olmalı")
        String code
) {
}
