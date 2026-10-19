/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.simulation.controller;

import com.bartugsevindik.cepte.backend.simulation.config.SimulationProperties;
import com.bartugsevindik.cepte.backend.simulation.dto.ClockDTO;
import com.bartugsevindik.cepte.backend.simulation.dto.ClockSyncRequest;
import com.bartugsevindik.cepte.backend.simulation.service.SimulationClockService;
import com.bartugsevindik.cepte.common.dto.ResponseMessage;
import com.bartugsevindik.cepte.common.helpers.ResponseHelper;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * <h1>SimulationClockController</h1>
 * <p>external-sim saati ileri sardığında bu uca yeni zamanı bildirir.</p>
 *
 * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 * @since 19.10.2026 - CP-1
 */
@Hidden
@RestController
@RequestMapping("/internal/simulation")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "application.clock", name = "mode", havingValue = "SIMULATED")
public class SimulationClockController {
    public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final SimulationClockService simulationClockService;
    private final SimulationProperties properties;

    /**
     * <h1>Saati Eşitle</h1>
     *
     * @param token   İç servis token'ı
     * @param request Simülatördeki an
     * @return Backend'in yeni saati
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 19.10.2026 - CP-1
     */
    @PostMapping("/clock")
    public ResponseEntity<ResponseMessage> syncClock(@RequestHeader(INTERNAL_TOKEN_HEADER) String token,
                                                     @Valid @RequestBody ClockSyncRequest request) {
        if (!MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                properties.getInternalToken().getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ResponseHelper.unauthorized("Geçersiz token."));
        }
        ClockDTO dto = simulationClockService.applySimulatorTime(request.now());
        return ResponseEntity.ok(ResponseHelper.success("Saat eşitlendi.", dto));
    }
}
