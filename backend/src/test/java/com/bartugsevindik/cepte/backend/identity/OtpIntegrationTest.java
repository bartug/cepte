/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity;

import com.bartugsevindik.cepte.backend.support.AbstractIntegrationTest;
import com.bartugsevindik.cepte.backend.support.TestDevice;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OtpIntegrationTest extends AbstractIntegrationTest {

    @Test
    void kayitliVeKayitsizNumarayaAyniCevapDoner() throws Exception {
        String registered = randomPhone();
        registerNewUser(registered, new TestDevice());
        clock().advance(Duration.ofMinutes(2));

        JsonNode first = json(postJson("/v1/auth/otp", "{\"phone\":\"" + registered + "\"}").andReturn().getResponse());
        JsonNode second = json(postJson("/v1/auth/otp", "{\"phone\":\"" + randomPhone() + "\"}").andReturn().getResponse());

        assertThat(first.get("message")).isEqualTo(second.get("message"));
        assertThat(first.get("object").get("expiresInSeconds")).isEqualTo(second.get("object").get("expiresInSeconds"));
        assertThat(first.get("object").get("resendAfterSeconds")).isEqualTo(second.get("object").get("resendAfterSeconds"));
        assertThat(first.get("object").size()).isEqualTo(second.get("object").size());
    }

    @Test
    void kodDogrulanincaNumaraninKayitliOlupOlmadigiSoylenir() throws Exception {
        String phone = randomPhone();
        UUID challenge = requestOtp(phone);

        verifyOtp(challenge, sms.lastCode(phone))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.object.registered").value(false))
                .andExpect(jsonPath("$.object.verificationToken").isNotEmpty());
    }

    @Test
    void besHataliDenemedenSonraDogruKodDaReddedilir() throws Exception {
        String phone = randomPhone();
        UUID challenge = requestOtp(phone);
        String code = sms.lastCode(phone);
        String wrong = code.equals("000000") ? "111111" : "000000";

        verifyOtp(challenge, wrong).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Kod hatalı. Kalan deneme hakkı: 4"));
        for (int i = 0; i < 3; i++) {
            verifyOtp(challenge, wrong).andExpect(status().isUnauthorized());
        }
        verifyOtp(challenge, wrong).andExpect(jsonPath("$.message").value("Çok fazla hatalı deneme yapıldı. Yeni kod iste."));

        verifyOtp(challenge, code).andExpect(status().isUnauthorized());
    }

    @Test
    void suresiDolanKodReddedilir() throws Exception {
        String phone = randomPhone();
        UUID challenge = requestOtp(phone);

        clock().advance(Duration.ofMinutes(4));

        verifyOtp(challenge, sms.lastCode(phone)).andExpect(status().isUnauthorized());
    }

    @Test
    void altmisSaniyeDolmadanYeniKodGonderilmez() throws Exception {
        String phone = randomPhone();
        requestOtp(phone);

        postJson("/v1/auth/otp", "{\"phone\":\"" + phone + "\"}")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.message", containsString("saniye bekle")));

        clock().advance(Duration.ofSeconds(61));
        postJson("/v1/auth/otp", "{\"phone\":\"" + phone + "\"}").andExpect(status().isOk());
        assertThat(sms.count(phone)).isEqualTo(2);
    }

    @Test
    void numaraBasinaPencereLimitiUygulanir() throws Exception {
        String phone = randomPhone();
        // Pencere sınırında başlamamak için yeni pencerenin başına git
        long window = 600;
        clock().advance(Duration.ofSeconds(window - clock().instant().getEpochSecond() % window + 1));
        for (int i = 0; i < 3; i++) {
            postJson("/v1/auth/otp", "{\"phone\":\"" + phone + "\"}").andExpect(status().isOk());
            clock().advance(Duration.ofSeconds(61));
        }

        postJson("/v1/auth/otp", "{\"phone\":\"" + phone + "\"}").andExpect(status().isTooManyRequests());
    }

    @Test
    void biletTekKullanimlik() throws Exception {
        String ticket = verifiedTicket(randomPhone());
        register(ticket, new TestDevice(), PIN).andExpect(status().isCreated());

        register(ticket, new TestDevice(), PIN).andExpect(status().isUnauthorized());
    }

    @Test
    void zayifPinReddedilirBiletYanmaz() throws Exception {
        String ticket = verifiedTicket(randomPhone());
        for (String weak : new String[]{"111111", "123456", "654321", "121212", "123123"}) {
            register(ticket, new TestDevice(), weak).andExpect(status().isBadRequest());
        }

        register(ticket, new TestDevice(), PIN).andExpect(status().isCreated());
    }

    @Test
    void hataliTelefonFormatiReddedilir() throws Exception {
        postJson("/v1/auth/otp", "{\"phone\":\"05321234567\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("phone: +905XXXXXXXXX formatında olmalı"));
    }
}
