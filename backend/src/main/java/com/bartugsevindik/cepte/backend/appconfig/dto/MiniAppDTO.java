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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Mini uygulama")
public class MiniAppDTO {

    @Schema(description = "Mini uygulama kodu. iOS'taki kayıtla eşleşir.", example = "FOOD")
    private String code;

    @Schema(description = "Ana sayfada görünen ad", example = "Yemek")
    private String title;

    @Schema(description = "SF Symbols ikon adı", example = "fork.knife")
    private String icon;

    @Schema(description = "Açılabilmesi için gereken KYC seviyesi", example = "0")
    private Integer requiredKycLevel;

    @Schema(description = "Kapalıysa ana sayfada görünmez", example = "true")
    private Boolean enabled;

    @Schema(description = "Ana sayfadaki sırası", example = "1")
    private Integer sortOrder;
}
