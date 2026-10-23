/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.service;

import com.bartugsevindik.cepte.backend.identity.enums.AuthEventType;
import org.springframework.stereotype.Service;

@Service
public interface AuthEventService {

    /**
     * <h1>Giriş Olayı Kaydetme</h1>
     * <p>Kendi transaction'ında yazılır; çağıran işlem hata verip geri alınsa da (yanlış PIN gibi) olay kalır.</p>
     *
     * @param type     Olay
     * @param userId   Kullanıcı, biliniyorsa
     * @param deviceId Cihaz, biliniyorsa
     * @param phone    Telefon, biliniyorsa
     * @param detail   Açıklama
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    void record(AuthEventType type, Long userId, Long deviceId, String phone, String detail);
}
