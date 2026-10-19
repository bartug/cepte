/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.common.time;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ControllableClockTest {

    private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");
    private static final Instant START = Instant.parse("2026-10-19T09:00:00Z");

    private final ControllableClock clock = new ControllableClock(Clock.fixed(START, ISTANBUL));

    @Test
    void ileriSarilincaYeniTarihGorulur() {
        clock.advance(Duration.ofDays(31));

        assertThat(LocalDate.now(clock)).isEqualTo(LocalDate.of(2026, 11, 19));
    }

    @Test
    void geriSarilamaz() {
        assertThatThrownBy(() -> clock.advance(Duration.ofDays(-1))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void gecmisBirAnaTasinmakSaatiDegistirmez() {
        clock.advance(Duration.ofHours(5));

        assertThat(clock.moveTo(START)).isEqualTo(START.plus(Duration.ofHours(5)));
    }

    @Test
    void farkliZoneIleAlinanSaatDeIleriSarmadanEtkilenir() {
        Clock utc = clock.withZone(ZoneId.of("UTC"));
        clock.advance(Duration.ofDays(1));

        assertThat(utc.instant()).isEqualTo(START.plus(Duration.ofDays(1)));
    }
}
