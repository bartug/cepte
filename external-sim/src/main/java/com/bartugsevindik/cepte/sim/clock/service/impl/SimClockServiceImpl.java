/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.sim.clock.service.impl;

import com.bartugsevindik.cepte.sim.clock.config.SimProperties;
import com.bartugsevindik.cepte.sim.clock.dto.ClockAdvanceRequest;
import com.bartugsevindik.cepte.sim.clock.dto.SimClockDTO;
import com.bartugsevindik.cepte.sim.clock.service.SimClockService;
import com.bartugsevindik.cepte.common.exception.BadRequestException;
import com.bartugsevindik.cepte.common.time.ControllableClock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimClockServiceImpl implements SimClockService {
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final ControllableClock clock;
    private final RestClient backendRestClient;
    private final SimProperties properties;

    /**
     * <h1>Saati Getirme</h1>
     *
     * @return Simülatörün saati
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 19.10.2026 - CP-1
     */
    @Override
    public SimClockDTO getClock() {
        return toDTO(null);
    }

    /**
     * <h1>Zamanı İleri Sarma</h1>
     * <p>Backend'e ulaşılamazsa sarma yine geçerlidir; backend açılırken saati buradan çeker.</p>
     *
     * @param request Süre ya da hedef an
     * @return Yeni saat
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 19.10.2026 - CP-1
     */
    @Override
    public synchronized SimClockDTO advance(@NotNull ClockAdvanceRequest request) {
        Instant before = clock.instant();
        if (request.until() != null) {
            Instant target = request.untilInstant(properties.getZone());
            if (target.isBefore(before)) {
                throw new BadRequestException("Hedef an geçmişte olamaz. Saat geri sarılamaz.");
            }
            clock.moveTo(target);
        } else {
            clock.advance(request.toDuration());
        }
        log.warn("Clock advanced. from={}, to={}", before, clock.instant());
        return toDTO(notifyBackend());
    }

    private boolean notifyBackend() {
        try {
            backendRestClient.post()
                    .uri("/internal/simulation/clock")
                    .header(INTERNAL_TOKEN_HEADER, properties.getInternalToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("now", clock.instant().toString()))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.warn("Backend could not be notified, it will pull the clock on startup. cause={}", e.getMessage());
            return false;
        }
    }

    private SimClockDTO toDTO(Boolean backendNotified) {
        Instant now = clock.instant();
        return new SimClockDTO(now, LocalDateTime.ofInstant(now, properties.getZone()), clock.getOffset(), backendNotified);
    }
}
