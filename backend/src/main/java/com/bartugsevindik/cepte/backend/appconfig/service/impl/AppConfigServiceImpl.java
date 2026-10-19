/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.appconfig.service.impl;

import com.bartugsevindik.cepte.backend.appconfig.config.AppConfigProperties;
import com.bartugsevindik.cepte.backend.appconfig.dto.AppConfigDTO;
import com.bartugsevindik.cepte.backend.appconfig.dto.MiniAppDTO;
import com.bartugsevindik.cepte.backend.appconfig.entity.FeatureFlag;
import com.bartugsevindik.cepte.backend.appconfig.entity.MiniApp;
import com.bartugsevindik.cepte.backend.appconfig.mapper.MiniAppMapper;
import com.bartugsevindik.cepte.backend.appconfig.repository.FeatureFlagRepository;
import com.bartugsevindik.cepte.backend.appconfig.repository.MiniAppRepository;
import com.bartugsevindik.cepte.backend.appconfig.service.AppConfigService;
import com.bartugsevindik.cepte.common.exception.NotFoundException;
import com.bartugsevindik.cepte.common.time.ControllableClock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppConfigServiceImpl implements AppConfigService {

    private final MiniAppRepository miniAppRepository;
    private final FeatureFlagRepository featureFlagRepository;
    private final MiniAppMapper miniAppMapper;
    private final AppConfigProperties properties;
    private final Clock clock;

    /**
     * <h1>Uygulama Konfigürasyonu Getirme</h1>
     * <p>Kapalı mini uygulamalar da listede döner; uygulama, açık bir ekranda duran kullanıcıyı kapanan mini
     * uygulamadan nazikçe çıkarabilsin diye.</p>
     *
     * @return Konfigürasyon
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 19.10.2026 - CP-1
     */
    @Override
    @Transactional(readOnly = true)
    public AppConfigDTO getAppConfig() {
        Map<String, Boolean> features = featureFlagRepository.findAll().stream()
                .collect(Collectors.toMap(FeatureFlag::getCode, FeatureFlag::getEnabled, (a, b) -> a, TreeMap::new));
        return AppConfigDTO.builder()
                .serverTime(Instant.now(clock))
                .timeZone(clock.getZone().getId())
                .simulatedClock(clock instanceof ControllableClock)
                .minimumAppVersion(properties.getMinimumVersion())
                .miniApps(miniAppMapper.toDTOList(miniAppRepository.findAllByOrderBySortOrderAsc()))
                .features(features)
                .build();
    }

    /**
     * <h1>Mini Uygulama Açma / Kapama</h1>
     *
     * @param code    Mini uygulama kodu
     * @param enabled Açık mı
     * @return Güncel mini uygulama
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 19.10.2026 - CP-1
     */
    @Override
    @Transactional
    public MiniAppDTO updateMiniAppEnabled(@NotNull String code, boolean enabled) {
        MiniApp miniApp = miniAppRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Mini uygulama bulunamadı: " + code));
        miniApp.setEnabled(enabled);
        log.info("Mini app toggled. code={}, enabled={}", code, enabled);
        return miniAppMapper.toDTO(miniApp);
    }

    /**
     * <h1>Feature Flag Açma / Kapama</h1>
     *
     * @param code    Feature flag kodu
     * @param enabled Açık mı
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 19.10.2026 - CP-1
     */
    @Override
    @Transactional
    public void updateFeatureEnabled(@NotNull String code, boolean enabled) {
        FeatureFlag flag = featureFlagRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Feature flag bulunamadı: " + code));
        flag.setEnabled(enabled);
        log.info("Feature flag toggled. code={}, enabled={}", code, enabled);
    }
}
