/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.service.impl;

import com.bartugsevindik.cepte.backend.identity.config.IdentityProperties;
import com.bartugsevindik.cepte.backend.identity.repository.OtpChallengeRepository;
import com.bartugsevindik.cepte.common.exception.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * <h1>OtpRateLimiter</h1>
 * <p>Her SMS para demek; numara başına ve IP başına gönderim sınırlanır (SMS pumping saldırısı).</p>
 * <p>Pencereler Redis TTL'ine değil uygulama saatine göre hesaplanır. TTL Redis'in kendi saatiyle işler;
 * simülasyonda zaman ileri sarıldığında Redis'teki sayaçlar dolmazdı. TTL sadece temizlik için.</p>
 * <p>Redis'e ulaşılamazsa aynı sınırlar otp_challenge tablosundan sayılarak uygulanır; SMS limiti fail-open olmaz.</p>
 *
 * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 * @since 23.10.2026 - CP-2
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OtpRateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final OtpChallengeRepository otpChallengeRepository;
    private final IdentityProperties properties;
    private final Clock clock;

    public void check(@NotNull String phone, String ip) {
        IdentityProperties.Otp otp = properties.getOtp();
        Instant now = clock.instant();
        try {
            String last = redisTemplate.opsForValue().get(lastSentKey(phone));
            checkCooldown(last == null ? null : Instant.parse(last), now);
            long windowEnd = windowEnd(now);
            checkLimit(count(phoneKey(phone, now)), otp.getMaxPerPhone(), windowEnd - now.getEpochSecond());
            if (ip != null) {
                checkLimit(count(ipKey(ip, now)), otp.getMaxPerIp(), windowEnd - now.getEpochSecond());
            }
        } catch (DataAccessException e) {
            log.warn("Redis unavailable, OTP rate limit falls back to database. cause={}", e.getMessage());
            Instant windowStart = now.minus(otp.getWindow());
            checkCooldown(otpChallengeRepository.findFirstByPhoneOrderByCreatedDateDesc(phone)
                    .map(c -> c.getCreatedDate()).orElse(null), now);
            checkLimit(otpChallengeRepository.countByPhoneAndCreatedDateAfter(phone, windowStart), otp.getMaxPerPhone(),
                    otp.getWindow().toSeconds());
            if (ip != null) {
                checkLimit(otpChallengeRepository.countByIpAndCreatedDateAfter(ip, windowStart), otp.getMaxPerIp(),
                        otp.getWindow().toSeconds());
            }
        }
    }

    public void recordSent(@NotNull String phone, String ip) {
        Instant now = clock.instant();
        Duration ttl = properties.getOtp().getWindow().multipliedBy(2);
        try {
            redisTemplate.opsForValue().set(lastSentKey(phone), now.toString(), ttl);
            increment(phoneKey(phone, now), ttl);
            if (ip != null) {
                increment(ipKey(ip, now), ttl);
            }
        } catch (DataAccessException e) {
            log.warn("Redis unavailable, OTP send not counted in Redis. cause={}", e.getMessage());
        }
    }

    private void checkCooldown(Instant lastSent, Instant now) {
        if (lastSent == null) {
            return;
        }
        long remaining = Duration.between(now, lastSent.plus(properties.getOtp().getResendCooldown())).toSeconds();
        if (remaining > 0) {
            throw new TooManyRequestsException("Yeni kod istemek için " + remaining + " saniye bekle.", remaining);
        }
    }

    private static void checkLimit(long count, int max, long retryAfterSeconds) {
        if (count >= max) {
            throw new TooManyRequestsException("Çok fazla kod istendi. Biraz sonra tekrar dene.", Math.max(retryAfterSeconds, 1));
        }
    }

    private long count(String key) {
        String value = redisTemplate.opsForValue().get(key);
        return value == null ? 0 : Long.parseLong(value);
    }

    private void increment(String key, Duration ttl) {
        Long value = redisTemplate.opsForValue().increment(key);
        if (value != null && value == 1) {
            redisTemplate.expire(key, ttl);
        }
    }

    private long windowEnd(Instant now) {
        long window = properties.getOtp().getWindow().toSeconds();
        return (now.getEpochSecond() / window + 1) * window;
    }

    private String phoneKey(String phone, Instant now) {
        return "otp:count:phone:" + phone + ":" + bucket(now);
    }

    private String ipKey(String ip, Instant now) {
        return "otp:count:ip:" + ip + ":" + bucket(now);
    }

    private long bucket(Instant now) {
        return now.getEpochSecond() / properties.getOtp().getWindow().toSeconds();
    }

    private static String lastSentKey(String phone) {
        return "otp:last:" + phone;
    }
}
