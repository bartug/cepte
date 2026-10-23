/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.notification.sms;

import com.bartugsevindik.cepte.backend.notification.SmsDeliveryException;
import com.bartugsevindik.cepte.backend.notification.SmsSender;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * <h1>SimulatorSmsSender</h1>
 * <p>SMS'i external-sim'in SMS ağ geçidine gönderir. Gerçek bir sağlayıcı eklendiğinde aynı arayüzün yeni bir
 * implementasyonu yazılır.</p>
 *
 * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 * @since 23.10.2026 - CP-2
 */
@Slf4j
@Component
public class SimulatorSmsSender implements SmsSender {

    private final RestClient restClient;

    public SimulatorSmsSender(@Qualifier("smsProviderRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void send(@NotNull String phone, @NotNull String text) {
        try {
            restClient.post()
                    .uri("/sim/sms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("phone", phone, "text", text))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new SmsDeliveryException("SMS gönderilemedi.", e);
        }
    }
}
