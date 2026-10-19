/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.appconfig.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Uygulamanın açılışta okuduğu konfigürasyon")
public class AppConfigDTO {

    @Schema(description = "Sunucu saati. Simülasyonda ileri sarılmış olabilir; uygulama tarih gösterirken bunu kullanır.",
            example = "2026-10-19T09:00:00Z")
    private Instant serverTime;

    @Schema(description = "İş tarihi hesaplarının yapıldığı saat dilimi", example = "Europe/Istanbul")
    private String timeZone;

    @Schema(description = "Saat simülatörden mi geliyor", example = "true")
    private Boolean simulatedClock;

    @Schema(description = "Bu sürümün altındaki uygulamalar güncellemeye zorlanır", example = "1.0.0")
    private String minimumAppVersion;

    private List<MiniAppDTO> miniApps;

    @Schema(description = "Feature flag'ler", example = "{\"QR_PAYMENT\": false, \"CREDIT\": false}")
    private Map<String, Boolean> features;
}
