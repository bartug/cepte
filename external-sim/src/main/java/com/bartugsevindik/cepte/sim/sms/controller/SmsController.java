/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.sim.sms.controller;

import com.bartugsevindik.cepte.sim.sms.dto.SmsDTO;
import com.bartugsevindik.cepte.sim.sms.dto.SmsSendRequest;
import com.bartugsevindik.cepte.sim.sms.service.SmsInboxService;
import com.bartugsevindik.cepte.common.dto.ResponseMessage;
import com.bartugsevindik.cepte.common.helpers.ResponseHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h1>SmsController</h1>
 * <p>SMS ağ geçidinin sahte hali. Backend gönderir, geliştirici ve testler gelen kutusundan okur.</p>
 *
 * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 * @since 23.10.2026 - CP-2
 */
@Tag(name = "SMS Ağ Geçidi", description = "Gönderilen SMS'ler ve OTP kodları")
@RestController
@RequestMapping("/sim/sms")
@RequiredArgsConstructor
public class SmsController {
    private final SmsInboxService smsInboxService;

    /**
     * <h1>SMS Gönder</h1>
     *
     * @param request Alıcı ve mesaj
     * @return Kaydedilen SMS
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Operation(summary = "SMS Gönder", description = "Backend'in SMS sağlayıcısı olarak çağırdığı uç.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "SMS alındı.", content = @Content(schema = @Schema(implementation = SmsDTO.class)))
    })
    @PostMapping
    public ResponseEntity<ResponseMessage> send(@Valid @RequestBody SmsSendRequest request) {
        return ResponseEntity.ok(ResponseHelper.success("SMS alındı.", smsInboxService.receive(request)));
    }

    /**
     * <h1>Gelen Kutusunu Getir</h1>
     *
     * @param phone Numara filtresi
     * @param limit En fazla kaç mesaj
     * @return En yeni mesaj başta
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Operation(summary = "Gelen Kutusunu Getir", description = "OTP kodunu okumak için. En yeni mesaj başta.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mesajlar getirildi.",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SmsDTO.class))))
    })
    @GetMapping
    public ResponseEntity<ResponseMessage> list(@Parameter(description = "Numara, örn. +905321234567") @RequestParam(required = false) String phone,
                                                @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ResponseHelper.success("Mesajlar getirildi.", smsInboxService.list(phone, Math.min(limit, 100))));
    }
}
