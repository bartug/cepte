/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.simulation;

import java.time.Instant;

/**
 * <h1>ClockAdvancedEvent</h1>
 * <p>Simülasyonda saat ileri sarıldığında yayınlanır. Vade ve tahsilat job'ları bu event'i dinleyip ileri sarılan
 * zamana kadar yapmaları gereken işi hemen yapar; cron'un bir sonraki tetiklenmesini beklemez.</p>
 *
 * @param from Önceki an
 * @param to   Yeni an
 * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 * @since 19.10.2026 - CP-1
 */
public record ClockAdvancedEvent(Instant from, Instant to) {
}
