/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity;

import com.bartugsevindik.cepte.backend.identity.enums.AuthEventType;
import com.bartugsevindik.cepte.backend.identity.repository.AuthEventRepository;
import com.bartugsevindik.cepte.backend.support.AbstractIntegrationTest;
import com.bartugsevindik.cepte.backend.support.TestDevice;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SessionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AuthEventRepository authEventRepository;

    @Test
    void kayittanSonraAccessTokenIleKullaniciOkunur() throws Exception {
        String phone = randomPhone();
        Session session = registerNewUser(phone, new TestDevice());

        mockMvc.perform(get("/v1/me").header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.object.userId").value(session.userId()))
                .andExpect(jsonPath("$.object.maskedPhone").value("+90 " + phone.substring(3, 6) + " *** ** " + phone.substring(11)))
                .andExpect(jsonPath("$.object.biometricEnabled").value(false));
    }

    @Test
    void tokenSuzIstekReddedilir() throws Exception {
        mockMvc.perform(get("/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Oturum geçersiz ya da süresi dolmuş."));
    }

    @Test
    void accessTokenOnBesDakikaSonraGecersiz() throws Exception {
        Session session = registerNewUser(randomPhone(), new TestDevice());

        clock().advance(Duration.ofMinutes(16));

        mockMvc.perform(get("/v1/me").header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenHerKullanimdaDegisir() throws Exception {
        TestDevice device = new TestDevice();
        Session session = registerNewUser(randomPhone(), device);

        Session refreshed = session(refresh(device, session.deviceId(), session.refreshToken())
                .andExpect(status().isOk()).andReturn().getResponse());

        assertThat(refreshed.refreshToken()).isNotEqualTo(session.refreshToken());
        refresh(device, session.deviceId(), refreshed.refreshToken()).andExpect(status().isOk());
    }

    @Test
    void kullanilmisRefreshTokenGelinceAileKapanir() throws Exception {
        TestDevice device = new TestDevice();
        Session session = registerNewUser(randomPhone(), device);
        Session refreshed = session(refresh(device, session.deviceId(), session.refreshToken()).andReturn().getResponse());

        // Saldırgan eski token'ı kullanıyor
        refresh(device, session.deviceId(), session.refreshToken())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Oturum güvenlik nedeniyle sonlandırıldı. Tekrar giriş yap."));

        // Gerçek kullanıcının yeni token'ı da artık geçersiz
        refresh(device, session.deviceId(), refreshed.refreshToken()).andExpect(status().isUnauthorized());
        assertThat(authEventRepository.existsByUserIdAndType(session.userId(), AuthEventType.REFRESH_REUSE_DETECTED)).isTrue();
    }

    @Test
    void refreshTokenBaskaCihazinAnahtariylaKullanilamaz() throws Exception {
        TestDevice device = new TestDevice();
        Session session = registerNewUser(randomPhone(), device);

        refresh(new TestDevice(), session.deviceId(), session.refreshToken()).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Cihaz imzası doğrulanamadı."));
    }

    @Test
    void imzasizRefreshReddedilir() throws Exception {
        Session session = registerNewUser(randomPhone(), new TestDevice());

        postJson("/v1/auth/refresh", "{\"refreshToken\":\"" + session.refreshToken() + "\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Bu işlem cihaz imzası gerektirir."));
    }

    @Test
    void ayniImzaIkinciKezKullanilamaz() throws Exception {
        TestDevice device = new TestDevice();
        Session session = registerNewUser(randomPhone(), device);
        String body = "{\"pin\":\"" + PIN + "\"}";
        long timestamp = clock().instant().getEpochSecond();
        String nonce = UUID.randomUUID().toString();

        mockMvc.perform(signed(post("/v1/auth/login"), "POST", "/v1/auth/login", device, session.deviceId(), body, timestamp, nonce, false))
                .andExpect(status().isOk());
        mockMvc.perform(signed(post("/v1/auth/login"), "POST", "/v1/auth/login", device, session.deviceId(), body, timestamp, nonce, false))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Bu istek daha önce kullanılmış."));
    }

    @Test
    void zamanDamgasiPencereDisindaysaReddedilir() throws Exception {
        TestDevice device = new TestDevice();
        Session session = registerNewUser(randomPhone(), device);
        long sixMinutesAgo = clock().instant().minus(Duration.ofMinutes(6)).getEpochSecond();

        mockMvc.perform(signed(post("/v1/auth/login"), "POST", "/v1/auth/login", device, session.deviceId(),
                        "{\"pin\":\"" + PIN + "\"}", sixMinutesAgo, UUID.randomUUID().toString(), false))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void imzalandiktanSonraDegistirilenGovdeReddedilir() throws Exception {
        TestDevice device = new TestDevice();
        Session session = registerNewUser(randomPhone(), device);
        var request = signed(post("/v1/auth/login"), "POST", "/v1/auth/login", device, session.deviceId(), "{\"pin\":\"" + PIN + "\"}");

        mockMvc.perform(request.content("{\"pin\":\"999999\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    void besYanlisPinSonrasiKilitlenir() throws Exception {
        TestDevice device = new TestDevice();
        Session session = registerNewUser(randomPhone(), device);

        for (int i = 4; i >= 1; i--) {
            pinLogin(device, session.deviceId(), "999999")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("PIN hatalı. Kalan deneme hakkı: " + i));
        }
        pinLogin(device, session.deviceId(), "999999").andExpect(status().isLocked());
        // Kilitliyken doğru PIN de kabul edilmez
        pinLogin(device, session.deviceId(), PIN).andExpect(status().isLocked());

        clock().advance(Duration.ofMinutes(31));
        pinLogin(device, session.deviceId(), PIN).andExpect(status().isOk());
    }

    @Test
    void yeniCihazBaglanincaEskiCihazKapanir() throws Exception {
        String phone = randomPhone();
        TestDevice oldDevice = new TestDevice();
        Session old = registerNewUser(phone, oldDevice);
        clock().advance(Duration.ofSeconds(61));

        TestDevice newDevice = new TestDevice();
        String ticket = verifiedTicket(phone);
        Session current = session(bindDevice(ticket, newDevice, PIN).andExpect(status().isOk()).andReturn().getResponse());

        assertThat(current.userId()).isEqualTo(old.userId());
        assertThat(current.deviceId()).isNotEqualTo(old.deviceId());
        // Eski cihazın access token'ı süresi dolmadan geçersiz, refresh'i ve imzası da
        mockMvc.perform(get("/v1/me").header("Authorization", "Bearer " + old.accessToken())).andExpect(status().isUnauthorized());
        refresh(oldDevice, old.deviceId(), old.refreshToken()).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v1/me").header("Authorization", "Bearer " + current.accessToken())).andExpect(status().isOk());
    }

    @Test
    void simKopyalayanPiniBilmedenCihazBaglayamaz() throws Exception {
        String phone = randomPhone();
        Session victim = registerNewUser(phone, new TestDevice());
        clock().advance(Duration.ofSeconds(61));

        String ticket = verifiedTicket(phone);
        bindDevice(ticket, new TestDevice(), "999999").andExpect(status().isUnauthorized());

        mockMvc.perform(get("/v1/me").header("Authorization", "Bearer " + victim.accessToken())).andExpect(status().isOk());
    }

    @Test
    void kayitsizNumarayaCihazBaglanamaz() throws Exception {
        bindDevice(verifiedTicket(randomPhone()), new TestDevice(), PIN).andExpect(status().isNotFound());
    }

    @Test
    void faceIdAcilincaBiyometrikImzaIleGirilir() throws Exception {
        TestDevice device = new TestDevice();
        Session session = registerNewUser(randomPhone(), device);
        String enableBody = "{\"publicKey\":\"" + device.biometricPublicKey() + "\"}";

        mockMvc.perform(signed(put("/v1/auth/biometric"), "PUT", "/v1/auth/biometric", device, session.deviceId(), enableBody)
                        .header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isOk());

        mockMvc.perform(signed(post("/v1/auth/login/biometric"), "POST", "/v1/auth/login/biometric", device, session.deviceId(),
                        "", clock().instant().getEpochSecond(), UUID.randomUUID().toString(), true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.object.accessToken").isNotEmpty());
        // Biyometrik imza olmadan sadece cihaz imzası yetmez
        mockMvc.perform(signed(post("/v1/auth/login/biometric"), "POST", "/v1/auth/login/biometric", device, session.deviceId(), ""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cikistanSonraRefreshTokenGecersiz() throws Exception {
        TestDevice device = new TestDevice();
        Session session = registerNewUser(randomPhone(), device);

        mockMvc.perform(post("/v1/auth/logout").header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isOk());

        refresh(device, session.deviceId(), session.refreshToken()).andExpect(status().isUnauthorized());
        pinLogin(device, session.deviceId(), PIN).andExpect(status().isOk());
    }

    @Test
    void denetimIziDegistirilemez() throws Exception {
        Session session = registerNewUser(randomPhone(), new TestDevice());
        var event = authEventRepository.findAllByUserIdOrderByIdAsc(session.userId()).getFirst();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> authEventRepository.delete(event))
                .hasRootCauseMessage("ERROR: auth_event kayıtları değiştirilemez\n  Where: PL/pgSQL function identity.prevent_auth_event_change() line 3 at RAISE");
    }

    private org.springframework.test.web.servlet.ResultActions pinLogin(TestDevice device, Long deviceId, String pin) throws Exception {
        return mockMvc.perform(signed(post("/v1/auth/login"), "POST", "/v1/auth/login", device, deviceId, "{\"pin\":\"" + pin + "\"}"));
    }
}
