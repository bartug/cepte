/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.security;

import com.bartugsevindik.cepte.backend.identity.CurrentUser;
import com.bartugsevindik.cepte.backend.identity.enums.DeviceStatus;
import com.bartugsevindik.cepte.backend.identity.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * <h1>DeviceRevocationValidator</h1>
 * <p>Access token'ın verildiği cihaz hâlâ aktif mi. Yeni cihaz bağlanınca eski cihazdaki access token'lar süresi
 * dolmasını (15 dk) beklemeden geçersiz olur.</p>
 * <p>Her istekte primary key ile tek satır okur. Trafik arttığında kısa ömürlü bir cache ile değiştirilir.</p>
 *
 * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 * @since 23.10.2026 - CP-2
 */
@Component
@RequiredArgsConstructor
public class DeviceRevocationValidator implements OAuth2TokenValidator<Jwt> {

    private final DeviceRepository deviceRepository;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String deviceId = jwt.getClaimAsString(CurrentUser.DEVICE_CLAIM);
        if (deviceId != null && deviceRepository.existsByIdAndStatus(Long.valueOf(deviceId), DeviceStatus.ACTIVE)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Cihaz artık aktif değil.", null));
    }
}
