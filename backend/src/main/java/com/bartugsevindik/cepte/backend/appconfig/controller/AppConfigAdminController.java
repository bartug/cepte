/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.appconfig.controller;

import com.bartugsevindik.cepte.backend.appconfig.dto.EnabledUpdateRequest;
import com.bartugsevindik.cepte.backend.appconfig.dto.MiniAppDTO;
import com.bartugsevindik.cepte.backend.appconfig.service.AppConfigService;
import com.bartugsevindik.cepte.common.dto.ResponseMessage;
import com.bartugsevindik.cepte.common.helpers.ResponseHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h1>AppConfigAdminController</h1>
 * <p>Mini uygulama ve feature flag yönetimi. Backoffice kimlik doğrulaması CP-9'da eklenecek.</p>
 *
 * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 * @since 19.10.2026 - CP-1
 */
@Tag(name = "Uygulama Konfigürasyonu Yönetimi", description = "Mini uygulama ve feature flag açma / kapama")
@RestController
@RequestMapping("/v1/admin/app")
@RequiredArgsConstructor
public class AppConfigAdminController {
    private final AppConfigService appConfigService;

    /**
     * <h1>Mini Uygulamayı Aç / Kapat</h1>
     *
     * @param code    Mini uygulama kodu
     * @param request Açık mı
     * @return Güncel mini uygulama
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 19.10.2026 - CP-1
     */
    @Operation(summary = "Mini Uygulamayı Aç / Kapat",
               description = "Kapatılan mini uygulama, uygulamanın bir sonraki konfigürasyon okumasında ana sayfadan kalkar.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Mini uygulama güncellendi.",
                    content = @Content(schema = @Schema(implementation = MiniAppDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "Mini uygulama bulunamadı.")
    })
    @PutMapping("/mini-apps/{code}")
    public ResponseEntity<ResponseMessage> updateMiniApp(
            @PathVariable String code,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = EnabledUpdateRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "enabled": false
                                    }
                                    """)
                    )
            )
            @Valid @RequestBody EnabledUpdateRequest request) {
        MiniAppDTO dto = appConfigService.updateMiniAppEnabled(code, request.enabled());
        return ResponseEntity.ok(ResponseHelper.success("Mini uygulama güncellendi.", dto));
    }

    /**
     * <h1>Feature Flag'i Aç / Kapat</h1>
     *
     * @param code    Feature flag kodu
     * @param request Açık mı
     * @return Boş cevap
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 19.10.2026 - CP-1
     */
    @Operation(summary = "Feature Flag'i Aç / Kapat")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Feature flag güncellendi."),
            @ApiResponse(responseCode = "404", description = "Feature flag bulunamadı.")
    })
    @PutMapping("/features/{code}")
    public ResponseEntity<ResponseMessage> updateFeature(
            @PathVariable String code,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = EnabledUpdateRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "enabled": true
                                    }
                                    """)
                    )
            )
            @Valid @RequestBody EnabledUpdateRequest request) {
        appConfigService.updateFeatureEnabled(code, request.enabled());
        return ResponseEntity.ok(ResponseHelper.success("Feature flag güncellendi.", null));
    }
}
