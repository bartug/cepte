/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity;

import java.time.Instant;

/**
 * Yeni kullanıcı kaydında yayınlanır. customer modülü (CP-3) müşteri kaydını buna göre hazırlar.
 *
 * @param userId     Kullanıcı
 * @param phone      E.164 telefon
 * @param occurredAt Kayıt anı
 */
public record UserRegisteredEvent(Long userId, String phone, Instant occurredAt) {
}
