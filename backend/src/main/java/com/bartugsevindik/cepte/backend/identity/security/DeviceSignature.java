/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.security;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;

/**
 * <h1>DeviceSignature</h1>
 * <p>Cihazın Secure Enclave anahtarıyla attığı imzayı doğrular. İmzalanan metin:</p>
 * <pre>
 * METHOD \n
 * PATH(?QUERY) \n
 * TIMESTAMP (epoch saniye) \n
 * NONCE \n
 * hex(SHA-256(body))
 * </pre>
 * <p>payment-switch'teki terminal imzasıyla aynı yapı; fark anahtarın asimetrik olması. Backend'de sadece public key
 * durduğu için veritabanı sızsa bile imza üretilemez.</p>
 * <p>iOS'taki RequestSigner ile birebir aynı olmalı.</p>
 *
 * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 * @since 23.10.2026 - CP-2
 */
public final class DeviceSignature {

    private static final String ALGORITHM = "SHA256withECDSA";

    private DeviceSignature() {
    }

    public static String stringToSign(@NotNull String method, @NotNull String pathWithQuery, @NotNull String timestamp,
                                      @NotNull String nonce, byte @NotNull [] body) {
        return String.join("\n", method.toUpperCase(), pathWithQuery, timestamp, nonce, sha256Hex(body));
    }

    /**
     * İmza bozuk, anahtar okunamıyor ya da imza tutmuyorsa {@code false}; hangisi olduğu dışarıya söylenmez.
     */
    public static boolean verify(@NotNull String publicKeyBase64, @NotNull String stringToSign, String signatureBase64) {
        if (signatureBase64 == null) {
            return false;
        }
        try {
            Signature verifier = Signature.getInstance(ALGORITHM);
            verifier.initVerify(parsePublicKey(publicKeyBase64));
            verifier.update(stringToSign.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * <h1>Public Key Okuma</h1>
     * <p>Base64(DER SubjectPublicKeyInfo) formatında P-256 anahtar bekler. iOS'ta
     * {@code P256.Signing.PublicKey.derRepresentation} bu formatı üretir.</p>
     *
     * @param publicKeyBase64 Anahtar
     * @return Public key
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    public static PublicKey parsePublicKey(@NotNull String publicKeyBase64) throws GeneralSecurityException {
        PublicKey key = KeyFactory.getInstance("EC")
                .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64)));
        if (!(key instanceof ECPublicKey ec) || ec.getParams().getCurve().getField().getFieldSize() != 256) {
            throw new GeneralSecurityException("P-256 anahtar bekleniyor");
        }
        return key;
    }

    private static String sha256Hex(byte[] body) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
