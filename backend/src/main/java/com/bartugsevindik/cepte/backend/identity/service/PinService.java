/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.service;

import org.springframework.stereotype.Service;

@Service
public interface PinService {

    /**
     * <h1>PIN Gücü Kontrolü</h1>
     * <p>111111, 123456, 121212 gibi tahmin edilmesi kolay PIN'ler reddedilir.</p>
     *
     * @param pin PIN
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    void validateStrength(String pin);

    /**
     * <h1>PIN Hash'leme</h1>
     *
     * @param pin PIN
     * @return Argon2 hash
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    String hash(String pin);

    /**
     * <h1>PIN Doğrulama</h1>
     * <p>Yanlış denemeler kendi transaction'ında sayılır; çağıran işlem geri alınsa da sayaç artar.</p>
     *
     * @param userId   Kullanıcı
     * @param deviceId Denemenin yapıldığı cihaz, biliniyorsa
     * @param pin      PIN
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    void verify(Long userId, Long deviceId, String pin);
}
