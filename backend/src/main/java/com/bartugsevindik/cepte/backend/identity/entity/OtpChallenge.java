/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.entity;

import com.bartugsevindik.cepte.backend.identity.enums.OtpStatus;
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
 * <h1>OtpChallenge</h1>
 * <p>Telefona gönderilen tek kullanımlık kod. Kod doğrulanınca kısa ömürlü bir doğrulama bileti üretilir;
 * kayıt ya da cihaz bağlama bu biletle yapılır. Kod ve bilet sadece hash olarak saklanır.</p>
 *
 * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 * @since 23.10.2026 - CP-2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(schema = "identity", name = "otp_challenge")
public class OtpChallenge extends BaseEntity {

    @Column(name = "challenge_id", nullable = false, unique = true)
    private UUID challengeId;

    @Column(name = "phone", nullable = false, length = 13)
    private String phone;

    @Column(name = "ip", length = 45)
    private String ip;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "attempts", nullable = false)
    private Integer attempts;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OtpStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verification_hash", unique = true, length = 64)
    private String verificationHash;

    @Column(name = "verification_expires_at")
    private Instant verificationExpiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;
}
