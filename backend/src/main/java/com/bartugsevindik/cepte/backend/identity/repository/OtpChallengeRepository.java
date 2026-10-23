/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.repository;

import com.bartugsevindik.cepte.backend.identity.entity.OtpChallenge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from OtpChallenge c where c.challengeId = :challengeId")
    Optional<OtpChallenge> findByChallengeIdForUpdate(UUID challengeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from OtpChallenge c where c.verificationHash = :verificationHash")
    Optional<OtpChallenge> findByVerificationHashForUpdate(String verificationHash);

    // Redis'e ulaşılamadığında rate limit bu sorgularla uygulanır
    long countByPhoneAndCreatedDateAfter(String phone, Instant after);

    long countByIpAndCreatedDateAfter(String ip, Instant after);

    Optional<OtpChallenge> findFirstByPhoneOrderByCreatedDateDesc(String phone);
}
