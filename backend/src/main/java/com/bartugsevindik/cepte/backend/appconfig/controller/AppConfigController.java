/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.appconfig.controller;

import com.bartugsevindik.cepte.backend.appconfig.dto.AppConfigDTO;
import com.bartugsevindik.cepte.backend.appconfig.service.AppConfigService;
import com.bartugsevindik.cepte.common.dto.ResponseMessage;
import com.bartugsevindik.cepte.common.helpers.ResponseHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h1>AppConfigController</h1>
 * <p>Mobil uygulamanın açılışta çağırdığı konfigürasyon ucu. Oturum gerektirmez; giriş ekranı da mini app
 * listesine ve minimum sürüme ihtiyaç duyar.</p>
 *
 * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 * @since 19.10.2026 - CP-1
 */
@Tag(name = "Uygulama Konfigürasyonu", description = "Mini uygulamalar, feature flag'ler ve sunucu saati")
@RestController
@RequestMapping("/v1/app")
@RequiredArgsConstructor
public class AppConfigController {
    private final AppConfigService appConfigService;

    /**
     * <h1>Uygulama Konfigürasyonunu Getir</h1>
     *
     * @return Konfigürasyon
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 19.10.2026 - CP-1
     */
    @Operation(summary = "Uygulama Konfigürasyonunu Getir",
               description = "Uygulama her açılışta ve ön plana her gelişinde çağırır. Mini uygulama listesi, feature flag'ler, minimum sürüm ve sunucu saati döner.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Konfigürasyon getirildi.",
                    content = @Content(schema = @Schema(implementation = AppConfigDTO.class))
            )
    })
    @GetMapping("/config")
    public ResponseEntity<ResponseMessage> getAppConfig() {
        return ResponseEntity.ok(ResponseHelper.success("Konfigürasyon getirildi.", appConfigService.getAppConfig()));
    }
}
