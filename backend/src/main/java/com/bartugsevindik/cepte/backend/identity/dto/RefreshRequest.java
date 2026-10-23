/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Token yenileme")
public record RefreshRequest(
        @Schema(description = "Refresh token")
        @NotBlank
        @Size(max = 100)
        String refreshToken
) {
}
