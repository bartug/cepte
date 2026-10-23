/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.controller;

import com.bartugsevindik.cepte.backend.identity.CurrentUser;
import com.bartugsevindik.cepte.backend.identity.dto.MeDTO;
import com.bartugsevindik.cepte.backend.identity.service.AuthService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h1>MeController</h1>
 * <p>Oturum sahibinin bilgileri.</p>
 *
 * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 * @since 23.10.2026 - CP-2
 */
@Tag(name = "Kimlik ve Oturum")
@RestController
@RequestMapping("/v1/me")
@RequiredArgsConstructor
public class MeController {
    private final AuthService authService;

    /**
     * <h1>Oturum Sahibini Getir</h1>
     *
     * @param jwt Access token
     * @return Kullanıcı ve cihaz bilgisi
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Operation(summary = "Oturum Sahibini Getir")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Kullanıcı getirildi.",
                    content = @Content(schema = @Schema(implementation = MeDTO.class))),
            @ApiResponse(responseCode = "401", description = "Oturum geçersiz.")
    })
    @GetMapping
    public ResponseEntity<ResponseMessage> getMe(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ResponseHelper.success("Kullanıcı getirildi.", authService.getMe(CurrentUser.from(jwt))));
    }
}
