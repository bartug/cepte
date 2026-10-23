/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.support;

import com.bartugsevindik.cepte.backend.identity.security.DeviceSignature;
import com.bartugsevindik.cepte.backend.identity.security.DeviceSignatureFilter;
import com.bartugsevindik.cepte.common.time.ControllableClock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Container'lar tüm test sınıfları için bir kez açılır (singleton container). Simülatör testlerde yok; SMS
 * {@link RecordingSmsSender}'a gider, açılıştaki saat çekme isteği kapalı porta gidip düşer.
 */
// IP limiti testlerin hepsi aynı IP'den (127.0.0.1) geldiği için yükseltilir; numara limiti ayrıca test ediliyor
@SpringBootTest(properties = {"application.simulation.base-url=http://localhost:1",
        "spring.datasource.hikari.maximum-pool-size=5", "application.identity.otp.max-per-ip=10000"})
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.TestConfig.class)
public abstract class AbstractIntegrationTest {

    protected static final String PIN = "482915";

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @ServiceConnection(name = "redis")
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected RecordingSmsSender sms;

    @Autowired
    private Clock clock;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        RecordingSmsSender recordingSmsSender() {
            return new RecordingSmsSender();
        }
    }

    public record Session(Long userId, Long deviceId, String accessToken, String refreshToken) {
    }

    protected ControllableClock clock() {
        return (ControllableClock) clock;
    }

    protected static String randomPhone() {
        return "+905" + ThreadLocalRandom.current().nextLong(100_000_000L, 1_000_000_000L);
    }

    protected ResultActions postJson(String path, String body) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected UUID requestOtp(String phone) throws Exception {
        JsonNode response = json(postJson("/v1/auth/otp", "{\"phone\":\"" + phone + "\"}").andReturn().getResponse());
        return UUID.fromString(response.at("/object/challengeId").asText());
    }

    protected ResultActions verifyOtp(UUID challengeId, String code) throws Exception {
        return postJson("/v1/auth/otp/verify", "{\"challengeId\":\"" + challengeId + "\",\"code\":\"" + code + "\"}");
    }

    /**
     * Telefonu doğrular ve bileti döner.
     */
    protected String verifiedTicket(String phone) throws Exception {
        UUID challengeId = requestOtp(phone);
        return json(verifyOtp(challengeId, sms.lastCode(phone)).andReturn().getResponse()).at("/object/verificationToken").asText();
    }

    protected ResultActions register(String ticket, TestDevice device, String pin) throws Exception {
        return postJson("/v1/auth/register", deviceBody(ticket, device, pin));
    }

    protected ResultActions bindDevice(String ticket, TestDevice device, String pin) throws Exception {
        return postJson("/v1/auth/devices", deviceBody(ticket, device, pin));
    }

    protected Session registerNewUser(String phone, TestDevice device) throws Exception {
        return session(register(verifiedTicket(phone), device, PIN).andReturn().getResponse());
    }

    protected Session session(MockHttpServletResponse response) throws Exception {
        JsonNode object = json(response).get("object");
        return new Session(object.get("userId").asLong(), object.get("deviceId").asLong(),
                object.get("accessToken").asText(), object.get("refreshToken").asText());
    }

    /**
     * iOS'taki RequestSigner'ın yaptığı imza.
     */
    protected MockHttpServletRequestBuilder signed(MockHttpServletRequestBuilder builder, String method, String path,
                                                   TestDevice device, Long deviceId, String body) {
        return signed(builder, method, path, device, deviceId, body, clock.instant().getEpochSecond(), UUID.randomUUID().toString(), false);
    }

    protected MockHttpServletRequestBuilder signed(MockHttpServletRequestBuilder builder, String method, String path,
                                                   TestDevice device, Long deviceId, String body, long timestamp,
                                                   String nonce, boolean biometric) {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        String stringToSign = DeviceSignature.stringToSign(method, path, String.valueOf(timestamp), nonce, bytes);
        builder.contentType(MediaType.APPLICATION_JSON)
                .content(bytes)
                .header(DeviceSignatureFilter.DEVICE_ID_HEADER, deviceId)
                .header(DeviceSignatureFilter.TIMESTAMP_HEADER, timestamp)
                .header(DeviceSignatureFilter.NONCE_HEADER, nonce)
                .header(DeviceSignatureFilter.SIGNATURE_HEADER, device.sign(stringToSign));
        if (biometric) {
            builder.header(DeviceSignatureFilter.BIOMETRIC_SIGNATURE_HEADER, device.signBiometric(stringToSign));
        }
        return builder;
    }

    protected ResultActions refresh(TestDevice device, Long deviceId, String refreshToken) throws Exception {
        String body = "{\"refreshToken\":\"" + refreshToken + "\"}";
        return mockMvc.perform(signed(post("/v1/auth/refresh"), "POST", "/v1/auth/refresh", device, deviceId, body));
    }

    protected JsonNode json(MockHttpServletResponse response) throws Exception {
        return objectMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8));
    }

    private static String deviceBody(String ticket, TestDevice device, String pin) {
        return """
                {"verificationToken":"%s","pin":"%s","devicePublicKey":"%s","deviceName":"Test iPhone"}
                """.formatted(ticket, pin, device.publicKey());
    }
}
