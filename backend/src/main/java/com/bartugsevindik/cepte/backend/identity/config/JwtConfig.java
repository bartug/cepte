/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.config;

import com.bartugsevindik.cepte.backend.identity.security.DeviceRevocationValidator;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;

/**
 * Access token'lar ES256 ile imzalanır. Simetrik (HS256) yerine asimetrik: ileride başka bir servis token doğrulamak
 * isterse sadece public key'i alır, token üretemez.
 */
@Slf4j
@Configuration
public class JwtConfig {
    private static final String KEY_ID = "cepte-1";

    @Bean
    public KeyPair jwtKeyPair(IdentityProperties properties) throws GeneralSecurityException {
        IdentityProperties.Jwt jwt = properties.getJwt();
        if (isBlank(jwt.getPrivateKey()) || isBlank(jwt.getPublicKey())) {
            log.warn("JWT signing key not configured, generated an ephemeral key. Access tokens become invalid on restart.");
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            return generator.generateKeyPair();
        }
        KeyFactory factory = KeyFactory.getInstance("EC");
        return new KeyPair(
                factory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(jwt.getPublicKey()))),
                factory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(jwt.getPrivateKey()))));
    }

    @Bean
    public JwtEncoder jwtEncoder(KeyPair jwtKeyPair) {
        ECKey key = new ECKey.Builder(Curve.P_256, (ECPublicKey) jwtKeyPair.getPublic())
                .privateKey((ECPrivateKey) jwtKeyPair.getPrivate())
                .keyID(KEY_ID)
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }

    @Bean
    public JwtDecoder jwtDecoder(KeyPair jwtKeyPair, Clock clock, IdentityProperties properties,
                                 DeviceRevocationValidator deviceRevocationValidator) {
        ECKey key = new ECKey.Builder(Curve.P_256, (ECPublicKey) jwtKeyPair.getPublic()).keyID(KEY_ID).build();
        DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.ES256, new ImmutableJWKSet<>(new JWKSet(key))));
        // Nimbus süre kontrolünü sistem saatiyle yapar; ileri sarılmış simülasyon saatinde süresi dolan token geçerli
        // görünürdü. Kontrol aşağıdaki validator'da Clock bean'i ile yapılır.
        processor.setJWTClaimsSetVerifier((claims, context) -> {
        });

        JwtTimestampValidator timestampValidator = new JwtTimestampValidator(Duration.ofSeconds(30));
        timestampValidator.setClock(clock);

        NimbusJwtDecoder decoder = new NimbusJwtDecoder(processor);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestampValidator,
                new JwtIssuerValidator(properties.getToken().getIssuer()), deviceRevocationValidator));
        return decoder;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
