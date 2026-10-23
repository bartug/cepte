/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.enums;

/**
 * Giriş denetim izindeki olaylar. Şüpheli bir hesap hareketinde "ne oldu" sorusunun cevabı bu kayıtlardadır.
 */
public enum AuthEventType {
    OTP_SENT,
    OTP_FAILED,
    OTP_VERIFIED,
    REGISTERED,
    DEVICE_BOUND,
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    PIN_LOCKED,
    REFRESH_REUSE_DETECTED,
    REFRESH_DEVICE_MISMATCH,
    BIOMETRIC_ENABLED,
    BIOMETRIC_DISABLED,
    LOGOUT
}
