/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.appconfig.service;

import com.bartugsevindik.cepte.backend.appconfig.dto.AppConfigDTO;
import com.bartugsevindik.cepte.backend.appconfig.dto.MiniAppDTO;
import org.springframework.stereotype.Service;

@Service
public interface AppConfigService {

    /**
     * <h1>Uygulama Konfigürasyonu Getirme</h1>
     * <p>Mini uygulamalar, feature flag'ler, minimum sürüm ve sunucu saati.</p>
     *
     * @return Konfigürasyon
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 19.10.2026 - CP-1
     */
    AppConfigDTO getAppConfig();

    /**
     * <h1>Mini Uygulama Açma / Kapama</h1>
     *
     * @param code    Mini uygulama kodu
     * @param enabled Açık mı
     * @return Güncel mini uygulama
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 19.10.2026 - CP-1
     */
    MiniAppDTO updateMiniAppEnabled(String code, boolean enabled);

    /**
     * <h1>Feature Flag Açma / Kapama</h1>
     *
     * @param code    Feature flag kodu
     * @param enabled Açık mı
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 19.10.2026 - CP-1
     */
    void updateFeatureEnabled(String code, boolean enabled);
}
