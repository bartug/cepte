/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.shared.config;

import com.bartugsevindik.cepte.common.time.ControllableClock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Kodda zaman sadece bu bean'den alınır. {@code Instant.now()} gibi çağrılar ArchUnit testiyle yasaklı.
 */
@Slf4j
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(ClockProperties properties) {
        Clock system = Clock.system(properties.getZone());
        if (properties.getMode() == ClockProperties.Mode.SIMULATED) {
            log.warn("Simulated clock is active, time can be advanced by external-sim. zone={}", properties.getZone());
            return new ControllableClock(system);
        }
        return system;
    }
}
