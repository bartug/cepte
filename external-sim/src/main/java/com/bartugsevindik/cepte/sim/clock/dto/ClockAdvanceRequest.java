/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.sim.clock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Schema(description = "Zamanı ileri sarma isteği. Süre ya da hedef an verilir, ikisi birden verilmez.")
public record ClockAdvanceRequest(
        @Schema(description = "Gün", example = "31") @PositiveOrZero Integer days,
        @Schema(description = "Saat", example = "0") @PositiveOrZero Integer hours,
        @Schema(description = "Dakika", example = "0") @PositiveOrZero Integer minutes,
        @Schema(description = "Hedef an, İstanbul saatiyle", example = "2026-12-15T09:00:00") LocalDateTime until
) {

    @Schema(hidden = true)
    @AssertTrue(message = "Süre ya da hedef andan sadece biri verilmeli.")
    public boolean isValid() {
        boolean hasDuration = days != null || hours != null || minutes != null;
        return hasDuration != (until != null);
    }

    public Duration toDuration() {
        return Duration.ofDays(days == null ? 0 : days)
                .plusHours(hours == null ? 0 : hours)
                .plusMinutes(minutes == null ? 0 : minutes);
    }

    public Instant untilInstant(ZoneId zone) {
        return until.atZone(zone).toInstant();
    }
}
