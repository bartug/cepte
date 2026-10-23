/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity;

import java.time.Instant;
import java.util.List;

/**
 * Kullanıcı yeni bir cihaz bağladığında yayınlanır. Eski cihazlara "hesabına yeni cihaz eklendi" bildirimi gider;
 * SIM kopyalama saldırısında gerçek kullanıcının haberdar olduğu tek kanal budur.
 *
 * @param userId           Kullanıcı
 * @param deviceId         Yeni cihaz
 * @param revokedDeviceIds Kapatılan eski cihazlar
 * @param occurredAt       Bağlanma anı
 */
public record DeviceBoundEvent(Long userId, Long deviceId, List<Long> revokedDeviceIds, Instant occurredAt) {
}
