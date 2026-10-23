/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.enums;

public enum TokenRevokeReason {
    NEW_LOGIN,
    NEW_DEVICE,
    REUSE_DETECTED,
    DEVICE_MISMATCH,
    LOGOUT
}
