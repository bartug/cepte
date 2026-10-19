/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.sim.clock.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

@Schema(description = "Simülatörün saati")
public record SimClockDTO(
        @Schema(description = "Şu anki an (UTC)", example = "2026-11-19T09:00:00Z") Instant now,
        @Schema(description = "İstanbul saatiyle", example = "2026-11-19T12:00:00") LocalDateTime localNow,
        @Schema(description = "Gerçek saatten farkı (ISO-8601)", example = "PT744H") Duration offset,
        @Schema(description = "Sadece ileri sarmada dolu: backend'e bildirildi mi. Backend kapalıysa açılışta kendisi çeker.", example = "true") Boolean backendNotified
) {
}
