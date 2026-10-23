/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.service.impl;

import com.bartugsevindik.cepte.backend.identity.entity.AuthEvent;
import com.bartugsevindik.cepte.backend.identity.enums.AuthEventType;
import com.bartugsevindik.cepte.backend.identity.repository.AuthEventRepository;
import com.bartugsevindik.cepte.backend.identity.service.AuthEventService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthEventServiceImpl implements AuthEventService {

    private final AuthEventRepository authEventRepository;
    private final Clock clock;

    /**
     * <h1>Giriş Olayı Kaydetme</h1>
     *
     * @param type     Olay
     * @param userId   Kullanıcı, biliniyorsa
     * @param deviceId Cihaz, biliniyorsa
     * @param phone    Telefon, biliniyorsa
     * @param detail   Açıklama
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(@NotNull AuthEventType type, Long userId, Long deviceId, String phone, String detail) {
        authEventRepository.save(AuthEvent.builder()
                .type(type)
                .userId(userId)
                .deviceId(deviceId)
                .phone(phone)
                .detail(detail)
                .ip(currentIp())
                .occurredAt(Instant.now(clock))
                .build());
    }

    private static String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            return request.getRemoteAddr();
        }
        return null;
    }
}
