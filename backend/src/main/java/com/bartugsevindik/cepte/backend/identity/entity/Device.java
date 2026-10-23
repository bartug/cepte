/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.entity;

import com.bartugsevindik.cepte.backend.identity.enums.DeviceStatus;
import com.bartugsevindik.cepte.backend.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * <h1>Device</h1>
 * <p>Kullanıcının bağladığı cihaz. Özel anahtar cihazın Secure Enclave'inde kalır; burada sadece public key var.
 * Veritabanı sızsa bile kimse cihaz adına imza atamaz.</p>
 * <p>Biyometrik anahtar ayrıdır: istek imzalayan anahtar her istekte Face ID sormasın diye kullanıcı onayı istemez,
 * biyometrik anahtar ise sadece Face ID ile açılır ve sadece girişte kullanılır.</p>
 *
 * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 * @since 23.10.2026 - CP-2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(schema = "identity", name = "device")
public class Device extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Base64(DER SubjectPublicKeyInfo), P-256
    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
    private String publicKey;

    @Column(name = "biometric_public_key", columnDefinition = "TEXT")
    private String biometricPublicKey;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeviceStatus status;

    @Column(name = "bound_at", nullable = false)
    private Instant boundAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;
}
