/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Biyometrik giriş anahtarı")
public record BiometricEnableRequest(
        @Schema(description = "Face ID ile korunan anahtarın public key'i: Base64(DER SubjectPublicKeyInfo), P-256")
        @NotBlank
        @Size(max = 500)
        String publicKey
) {
}
