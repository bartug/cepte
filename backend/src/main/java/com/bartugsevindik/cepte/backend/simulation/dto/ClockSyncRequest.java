/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.simulation.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * @param now Simülatördeki an
 */
public record ClockSyncRequest(@NotNull Instant now) {
}
