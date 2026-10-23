/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.notification;

public class SmsDeliveryException extends RuntimeException {

    public SmsDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
