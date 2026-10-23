/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.entity;

import com.bartugsevindik.cepte.backend.identity.enums.TokenRevokeReason;
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
import java.util.UUID;

/**
 * <h1>RefreshToken</h1>
 * <p>Her yenilemede eski token kullanılmış işaretlenir, aynı ailede yenisi üretilir. Kullanılmış bir token tekrar
 * gelirse token kopyalanmıştır; ailenin tamamı iptal edilir.</p>
 *
 * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 * @since 23.10.2026 - CP-2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(schema = "identity", name = "refresh_token")
public class RefreshToken extends BaseEntity {

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    // Token 256 bit rastgele; SHA-256 hash'i yeterli, salt gerekmez
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "revoke_reason", length = 30)
    private TokenRevokeReason revokeReason;
}
