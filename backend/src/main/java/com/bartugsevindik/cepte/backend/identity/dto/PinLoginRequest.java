/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "PIN ile giriş")
public record PinLoginRequest(
        @Schema(description = "6 haneli PIN", example = "482915")
        @NotBlank
        @Pattern(regexp = "^\\d{6}$", message = "6 haneli olmalı")
        String pin
) {
}
