/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.repository;

import com.bartugsevindik.cepte.backend.identity.entity.RefreshToken;
import com.bartugsevindik.cepte.backend.identity.enums.TokenRevokeReason;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // Aynı token ile gelen iki paralel yenileme isteğinden biri bekler, ikincisi "kullanılmış" görür
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshToken t where t.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash);

    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :now, t.revokeReason = :reason where t.familyId = :familyId and t.revokedAt is null")
    int revokeFamily(UUID familyId, TokenRevokeReason reason, Instant now);

    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :now, t.revokeReason = :reason where t.deviceId = :deviceId and t.revokedAt is null")
    int revokeDevice(Long deviceId, TokenRevokeReason reason, Instant now);
}
