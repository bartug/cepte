/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Gönderilen doğrulama kodu. Numaranın kayıtlı olup olmadığı burada söylenmez.")
public record OtpChallengeDTO(
        @Schema(description = "Kod doğrulamasında gönderilecek id") UUID challengeId,
        @Schema(description = "Kodun geçerlilik süresi", example = "180") long expiresInSeconds,
        @Schema(description = "Yeni kod istemek için beklenecek süre", example = "60") long resendAfterSeconds
) {
}
