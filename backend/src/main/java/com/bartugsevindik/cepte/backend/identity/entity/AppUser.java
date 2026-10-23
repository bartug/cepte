/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.entity;

import com.bartugsevindik.cepte.backend.identity.enums.UserStatus;
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
 * <h1>AppUser</h1>
 * <p>Uygulamaya giriş yapan hesap. Hukuki kişi bilgisi (TCKN, ad) burada tutulmaz; customer modülündedir.</p>
 *
 * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 * @since 23.10.2026 - CP-2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(schema = "identity", name = "app_user")
public class AppUser extends BaseEntity {

    @Column(name = "phone", nullable = false, unique = true, length = 13)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "pin_hash", nullable = false, length = 200)
    private String pinHash;

    @Column(name = "failed_pin_attempts", nullable = false)
    private Integer failedPinAttempts;

    @Column(name = "pin_locked_until")
    private Instant pinLockedUntil;

    // Her kilitlenmede süre ikiye katlanır
    @Column(name = "pin_lock_count", nullable = false)
    private Integer pinLockCount;
}
