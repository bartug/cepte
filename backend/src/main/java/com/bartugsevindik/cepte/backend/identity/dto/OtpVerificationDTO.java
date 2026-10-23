/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Doğrulanmış telefon")
public record OtpVerificationDTO(
        @Schema(description = "Kayıt ya da cihaz bağlamada kullanılacak tek kullanımlık bilet") String verificationToken,
        @Schema(description = "Numara kayıtlı mı. true ise PIN ile cihaz bağlanır, false ise kayıt olunur", example = "false") boolean registered,
        @Schema(description = "Biletin geçerlilik süresi", example = "600") long expiresInSeconds
) {
}
