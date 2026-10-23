/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.service.impl;

import com.bartugsevindik.cepte.backend.identity.config.IdentityProperties;
import com.bartugsevindik.cepte.backend.identity.entity.AppUser;
import com.bartugsevindik.cepte.backend.identity.enums.AuthEventType;
import com.bartugsevindik.cepte.backend.identity.repository.AppUserRepository;
import com.bartugsevindik.cepte.backend.identity.service.AuthEventService;
import com.bartugsevindik.cepte.backend.identity.service.PinService;
import com.bartugsevindik.cepte.common.exception.BadRequestException;
import com.bartugsevindik.cepte.common.exception.LockedException;
import com.bartugsevindik.cepte.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class PinServiceImpl implements PinService {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder pinEncoder;
    private final AuthEventService authEventService;
    private final IdentityProperties properties;
    private final Clock clock;

    /**
     * <h1>PIN Gücü Kontrolü</h1>
     *
     * @param pin PIN
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Override
    public void validateStrength(@NotNull String pin) {
        if (!pin.matches("\\d{6}")) {
            throw new BadRequestException("PIN 6 haneli olmalı.");
        }
        if (isRepeating(pin) || isSequential(pin)) {
            throw new BadRequestException("PIN tahmin edilmesi kolay olmamalı (111111, 123456, 121212 gibi).");
        }
    }

    /**
     * <h1>PIN Hash'leme</h1>
     *
     * @param pin PIN
     * @return Argon2 hash
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Override
    public String hash(@NotNull String pin) {
        return pinEncoder.encode(pin);
    }

    /**
     * <h1>PIN Doğrulama</h1>
     * <p>5 yanlış denemede PIN kilitlenir. İlk kilit 30 dakika, her yeni kilitte süre ikiye katlanır (en fazla 24 saat).
     * Doğru PIN sayacı sıfırlar ama kilit sayısını sıfırlamaz; kilit sayısı kalıcı bir risk sinyali.</p>
     *
     * @param userId   Kullanıcı
     * @param deviceId Denemenin yapıldığı cihaz, biliniyorsa
     * @param pin      PIN
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = {UnauthorizedException.class, LockedException.class})
    public void verify(@NotNull Long userId, Long deviceId, @NotNull String pin) {
        AppUser user = appUserRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UnauthorizedException("PIN hatalı."));
        Instant now = Instant.now(clock);
        if (user.getPinLockedUntil() != null && now.isBefore(user.getPinLockedUntil())) {
            throw new LockedException(lockedMessage(user.getPinLockedUntil()));
        }

        if (pinEncoder.matches(pin, user.getPinHash())) {
            user.setFailedPinAttempts(0);
            user.setPinLockedUntil(null);
            return;
        }

        int attempts = user.getFailedPinAttempts() + 1;
        authEventService.record(AuthEventType.LOGIN_FAILED, userId, deviceId, user.getPhone(), "pin attempt=" + attempts);
        if (attempts >= properties.getPin().getMaxAttempts()) {
            int lockCount = user.getPinLockCount() + 1;
            Instant lockedUntil = now.plus(lockDuration(lockCount));
            user.setFailedPinAttempts(0);
            user.setPinLockCount(lockCount);
            user.setPinLockedUntil(lockedUntil);
            authEventService.record(AuthEventType.PIN_LOCKED, userId, deviceId, user.getPhone(), "until=" + lockedUntil);
            log.warn("PIN locked. userId={}, lockCount={}, until={}", userId, lockCount, lockedUntil);
            throw new LockedException(lockedMessage(lockedUntil));
        }
        user.setFailedPinAttempts(attempts);
        throw new UnauthorizedException("PIN hatalı. Kalan deneme hakkı: " + (properties.getPin().getMaxAttempts() - attempts));
    }

    private Duration lockDuration(int lockCount) {
        Duration duration = properties.getPin().getLockDuration().multipliedBy(1L << Math.min(lockCount - 1, 10));
        return duration.compareTo(properties.getPin().getMaxLockDuration()) > 0 ? properties.getPin().getMaxLockDuration() : duration;
    }

    private String lockedMessage(Instant lockedUntil) {
        return "PIN çok fazla hatalı girildi. " + TIME.format(lockedUntil.atZone(clock.getZone()))
                + " sonrasında tekrar dene.";
    }

    // 111111, 121212, 123123
    private static boolean isRepeating(String pin) {
        return pin.chars().distinct().count() == 1
                || pin.equals(pin.substring(0, 2).repeat(3))
                || pin.equals(pin.substring(0, 3).repeat(2));
    }

    // 123456, 654321, 345678
    private static boolean isSequential(String pin) {
        int step = pin.charAt(1) - pin.charAt(0);
        if (Math.abs(step) != 1) {
            return false;
        }
        for (int i = 2; i < pin.length(); i++) {
            if (pin.charAt(i) - pin.charAt(i - 1) != step) {
                return false;
            }
        }
        return true;
    }
}
