/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.simulation.service.impl;

import com.bartugsevindik.cepte.backend.simulation.ClockAdvancedEvent;
import com.bartugsevindik.cepte.backend.simulation.dto.ClockDTO;
import com.bartugsevindik.cepte.backend.simulation.service.SimulationClockService;
import com.bartugsevindik.cepte.common.time.ControllableClock;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "application.clock", name = "mode", havingValue = "SIMULATED")
public class SimulationClockServiceImpl implements SimulationClockService {

    private final Clock clock;
    private final RestClient simulatorRestClient;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * <h1>Simülatör Saatine Eşitleme</h1>
     * <p>Saat gerçekten ilerlediyse {@link ClockAdvancedEvent} yayınlanır.</p>
     *
     * @param now Simülatördeki an
     * @return Backend'in yeni saati
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 19.10.2026 - CP-1
     */
    @Override
    public synchronized ClockDTO applySimulatorTime(@NotNull Instant now) {
        ControllableClock controllable = (ControllableClock) clock;
        Instant before = controllable.instant();
        Instant after = controllable.moveTo(now);
        // Gerçek saat zaten akıyor; birkaç saniyelik ağ gecikmesi ileri sarma sayılmaz
        if (after.isAfter(before.plusSeconds(5))) {
            log.warn("Clock advanced by simulator. from={}, to={}", before, after);
            eventPublisher.publishEvent(new ClockAdvancedEvent(before, after));
        }
        return new ClockDTO(after, controllable.getOffset());
    }

    /**
     * <h1>Simülatörden Saat Çekme</h1>
     * <p>Simülatör kapalıysa backend gerçek saatle açılır; simülatör açıldığında saati kendisi gönderir.</p>
     *
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 19.10.2026 - CP-1
     */
    @Override
    @EventListener(ApplicationReadyEvent.class)
    public void pullFromSimulator() {
        try {
            JsonNode response = simulatorRestClient.get().uri("/sim/clock").retrieve().body(JsonNode.class);
            Instant simulatorNow = Instant.parse(response.at("/object/now").asText());
            applySimulatorTime(simulatorNow);
            log.info("Clock synchronized with simulator. now={}", clock.instant());
        } catch (RestClientException | IllegalArgumentException | NullPointerException e) {
            log.warn("Simulator unreachable, starting with real time. cause={}", e.getMessage());
        }
    }
}
