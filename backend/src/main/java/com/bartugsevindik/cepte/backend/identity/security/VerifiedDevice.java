/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.security;

/**
 * İmzası doğrulanmış cihaz. Filter request attribute olarak koyar, controller {@code @RequestAttribute} ile alır.
 *
 * @param deviceId           Cihaz
 * @param userId             Cihazın sahibi
 * @param biometricVerified  Biyometrik anahtarla da imzalanmış mı (Face ID ile açılmış)
 */
public record VerifiedDevice(Long deviceId, Long userId, boolean biometricVerified) {

    public static final String REQUEST_ATTRIBUTE = "verifiedDevice";
}
