/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.notification;

/**
 * <h1>SmsSender</h1>
 * <p>SMS gönderimi. Gönderim başarısız olursa exception fırlatır; OTP gibi akışlar kullanıcıya hata gösterebilsin.</p>
 *
 * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 * @since 23.10.2026 - CP-2
 */
public interface SmsSender {

    /**
     * <h1>SMS Gönderme</h1>
     *
     * @param phone E.164 formatında telefon (+905321234567)
     * @param text  Mesaj
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    void send(String phone, String text);
}
