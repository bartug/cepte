/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "application.identity")
public class IdentityProperties {

    @Valid
    private Otp otp = new Otp();

    @Valid
    private Pin pin = new Pin();

    @Valid
    private Token token = new Token();

    @Valid
    private Jwt jwt = new Jwt();

    @Valid
    private Signature signature = new Signature();

    @Getter
    @Setter
    public static class Otp {
        @NotNull
        private Duration ttl = Duration.ofMinutes(3);
        @Positive
        private int maxAttempts = 5;
        @NotNull
        private Duration resendCooldown = Duration.ofSeconds(60);
        // Pencere başına en fazla gönderim
        @Positive
        private int maxPerPhone = 3;
        @Positive
        private int maxPerIp = 10;
        @NotNull
        private Duration window = Duration.ofMinutes(10);
        // Kod doğrulandıktan sonra kayıt / cihaz bağlama için verilen süre
        @NotNull
        private Duration verificationTtl = Duration.ofMinutes(10);
        // Kod hash'i bu secret ile HMAC'lenir; 6 haneli kodun hash'i secret'sız birkaç saniyede kırılırdı
        @NotBlank
        private String hashSecret = "local-dev-only-otp-hash-secret";
    }

    @Getter
    @Setter
    public static class Pin {
        @Positive
        private int maxAttempts = 5;
        @NotNull
        private Duration lockDuration = Duration.ofMinutes(30);
        @NotNull
        private Duration maxLockDuration = Duration.ofHours(24);
    }

    @Getter
    @Setter
    public static class Token {
        @NotNull
        private Duration accessTtl = Duration.ofMinutes(15);
        @NotNull
        private Duration refreshTtl = Duration.ofDays(30);
        @NotBlank
        private String issuer = "cepte";
    }

    @Getter
    @Setter
    public static class Jwt {
        // Base64(PKCS#8) / Base64(X.509), EC P-256. Boşsa açılışta geçici bir anahtar üretilir (sadece lokal)
        private String privateKey;
        private String publicKey;
    }

    @Getter
    @Setter
    public static class Signature {
        @NotNull
        private Duration tolerance = Duration.ofMinutes(5);
        @Positive
        private int maxBodyBytes = 64 * 1024;
        // Cihaz imzası zorunlu olan uçlar; para çıkışı yapan uçlar sonraki fazlarda eklenir
        @NotNull
        private List<String> signedPaths = List.of("/v1/auth/login", "/v1/auth/login/biometric", "/v1/auth/refresh",
                "/v1/auth/biometric");
    }
}
