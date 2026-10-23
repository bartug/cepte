/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Yeni kullanıcı kaydı")
public record RegisterRequest(
        @Schema(description = "OTP doğrulamasında dönen bilet")
        @NotBlank
        String verificationToken,

        @Schema(description = "6 haneli PIN", example = "482915")
        @NotBlank
        @Pattern(regexp = "^\\d{6}$", message = "6 haneli olmalı")
        String pin,

        @Schema(description = "Cihaz anahtarının public key'i: Base64(DER SubjectPublicKeyInfo), P-256")
        @NotBlank
        @Size(max = 500)
        String devicePublicKey,

        @Schema(description = "Cihaz adı", example = "iPhone 17 Pro")
        @NotBlank
        @Size(max = 100)
        String deviceName
) {
}
