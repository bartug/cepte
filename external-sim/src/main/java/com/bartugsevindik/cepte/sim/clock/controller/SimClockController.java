/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.sim.clock.controller;

import com.bartugsevindik.cepte.sim.clock.dto.ClockAdvanceRequest;
import com.bartugsevindik.cepte.sim.clock.dto.SimClockDTO;
import com.bartugsevindik.cepte.sim.clock.service.SimClockService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h1>SimClockController</h1>
 * <p>Zaman makinesi. Vade, gecikme ve tahsilat senaryoları için zaman ileri sarılır.</p>
 *
 * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 * @since 19.10.2026 - CP-1
 */
@Tag(name = "Zaman Makinesi", description = "Simülasyon saatini okuma ve ileri sarma")
@RestController
@RequestMapping("/sim/clock")
@RequiredArgsConstructor
public class SimClockController {
    private final SimClockService simClockService;

    /**
     * <h1>Saati Getir</h1>
     *
     * @return Simülatörün saati
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 19.10.2026 - CP-1
     */
    @Operation(summary = "Saati Getir", description = "Backend açılışta saati buradan çeker.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Saat getirildi.",
                    content = @Content(schema = @Schema(implementation = SimClockDTO.class))
            )
    })
    @GetMapping
    public ResponseEntity<ResponseMessage> getClock() {
        return ResponseEntity.ok(ResponseHelper.success("Saat getirildi.", simClockService.getClock()));
    }

    /**
     * <h1>Zamanı İleri Sar</h1>
     *
     * @param request Süre ya da hedef an
     * @return Yeni saat
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 19.10.2026 - CP-1
     */
    @Operation(summary = "Zamanı İleri Sar",
               description = "Süre (gün/saat/dakika) ya da İstanbul saatiyle hedef an verilir. Geri sarma yoktur. Yeni zaman backend'e bildirilir.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Zaman ileri sarıldı.",
                    content = @Content(schema = @Schema(implementation = SimClockDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Hem süre hem hedef verildi ya da hedef geçmişte.")
    })
    @PostMapping("/advance")
    public ResponseEntity<ResponseMessage> advance(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ClockAdvanceRequest.class),
                            examples = {
                                    @ExampleObject(name = "31 gün", value = """
                                            {
                                              "days": 31
                                            }
                                            """),
                                    @ExampleObject(name = "Belirli an", value = """
                                            {
                                              "until": "2026-12-15T09:00:00"
                                            }
                                            """)
                            }
                    )
            )
            @Valid @RequestBody ClockAdvanceRequest request) {
        return ResponseEntity.ok(ResponseHelper.success("Zaman ileri sarıldı.", simClockService.advance(request)));
    }
}
