/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.shared.config;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.ZoneId;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "application.clock")
public class ClockProperties {

    /**
     * SIMULATED: saat external-sim'deki zaman makinesine bağlı, ileri sarılabilir.
     */
    @NotNull
    private Mode mode = Mode.SYSTEM;

    // Vade, değer tarihi ve iş günü hesapları bu zone'a göre
    @NotNull
    private ZoneId zone = ZoneId.of("Europe/Istanbul");

    public enum Mode {
        SYSTEM,
        SIMULATED
    }
}
