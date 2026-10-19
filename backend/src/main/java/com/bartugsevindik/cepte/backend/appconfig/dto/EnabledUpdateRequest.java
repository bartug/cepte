/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.appconfig.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Açma / kapama isteği")
public record EnabledUpdateRequest(
        @Schema(description = "Açık mı", example = "false")
        @NotNull
        Boolean enabled
) {
}
