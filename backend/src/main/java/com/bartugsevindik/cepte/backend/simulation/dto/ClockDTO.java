/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.simulation.dto;

import java.time.Duration;
import java.time.Instant;

/**
 * @param now    Backend'in şu anki zamanı
 * @param offset Gerçek saatten farkı
 */
public record ClockDTO(Instant now, Duration offset) {
}
