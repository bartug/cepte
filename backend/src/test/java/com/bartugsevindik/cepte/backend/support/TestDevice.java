/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.support;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

/**
 * iPhone'daki Secure Enclave anahtarının testteki karşılığı: P-256 anahtar çifti, SHA256withECDSA imza (DER).
 * iOS'ta CryptoKit'in {@code derRepresentation} çıktısıyla aynı format.
 */
public final class TestDevice {

    private final KeyPair deviceKey = generate();
    private final KeyPair biometricKey = generate();

    public String publicKey() {
        return Base64.getEncoder().encodeToString(deviceKey.getPublic().getEncoded());
    }

    public String biometricPublicKey() {
        return Base64.getEncoder().encodeToString(biometricKey.getPublic().getEncoded());
    }

    public String sign(String stringToSign) {
        return sign(deviceKey, stringToSign);
    }

    public String signBiometric(String stringToSign) {
        return sign(biometricKey, stringToSign);
    }

    private static String sign(KeyPair keyPair, String data) {
        try {
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(keyPair.getPrivate());
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static KeyPair generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
