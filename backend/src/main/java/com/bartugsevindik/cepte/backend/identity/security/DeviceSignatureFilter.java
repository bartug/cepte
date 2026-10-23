/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.security;

import com.bartugsevindik.cepte.backend.identity.config.IdentityProperties;
import com.bartugsevindik.cepte.backend.identity.entity.Device;
import com.bartugsevindik.cepte.backend.identity.enums.DeviceStatus;
import com.bartugsevindik.cepte.backend.identity.repository.DeviceRepository;
import com.bartugsevindik.cepte.common.dto.ResponseMessage;
import com.bartugsevindik.cepte.common.helpers.ResponseHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * <h1>DeviceSignatureFilter</h1>
 * <p>Giriş, token yenileme ve (sonraki fazlarda) para çıkışı yapan isteklerin cihaz imzasını doğrular.</p>
 * <p>Replay koruması: zaman damgası ±5 dakika içinde olmalı, nonce bu sürede bir kez kullanılabilir.
 * Zaman damgası simülasyonda da sunucu saatine göre kontrol edilir; iOS imzalarken sunucu saatini kullanır.</p>
 *
 * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 * @since 23.10.2026 - CP-2
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceSignatureFilter extends OncePerRequestFilter {

    public static final String DEVICE_ID_HEADER = "X-Device-Id";
    public static final String TIMESTAMP_HEADER = "X-Timestamp";
    public static final String NONCE_HEADER = "X-Nonce";
    public static final String SIGNATURE_HEADER = "X-Signature";
    public static final String BIOMETRIC_SIGNATURE_HEADER = "X-Biometric-Signature";
    private static final String INVALID_SIGNATURE = "Cihaz imzası doğrulanamadı.";

    private final DeviceRepository deviceRepository;
    private final IdentityProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    protected boolean shouldNotFilter(@NotNull HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !properties.getSignature().getSignedPaths().contains(path);
    }

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain) throws ServletException, IOException {
        String deviceId = request.getHeader(DEVICE_ID_HEADER);
        String timestamp = request.getHeader(TIMESTAMP_HEADER);
        String nonce = request.getHeader(NONCE_HEADER);
        String signature = request.getHeader(SIGNATURE_HEADER);
        if (deviceId == null || timestamp == null || nonce == null || signature == null) {
            reject(response, ResponseHelper.unauthorized("Bu işlem cihaz imzası gerektirir."));
            return;
        }
        if (!isWithinTolerance(timestamp)) {
            reject(response, ResponseHelper.unauthorized("İstek zaman damgası geçersiz ya da süresi dolmuş."));
            return;
        }

        byte[] body = request.getInputStream().readNBytes(properties.getSignature().getMaxBodyBytes() + 1);
        if (body.length > properties.getSignature().getMaxBodyBytes()) {
            reject(response, ResponseHelper.badRequest("İstek gövdesi çok büyük."));
            return;
        }

        Optional<Device> device = findActiveDevice(deviceId);
        String path = request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : "");
        String stringToSign = DeviceSignature.stringToSign(request.getMethod(), path, timestamp, nonce, body);
        if (device.isEmpty() || !DeviceSignature.verify(device.get().getPublicKey(), stringToSign, signature)) {
            log.warn("Device signature rejected. deviceId={}, path={}", deviceId, path);
            reject(response, ResponseHelper.unauthorized(INVALID_SIGNATURE));
            return;
        }
        // Nonce imza doğrulandıktan sonra kaydedilir; imzasız istekler Redis'i dolduramaz
        if (!markNonceUsed(device.get().getId(), nonce)) {
            log.warn("Replayed device signature. deviceId={}, path={}", deviceId, path);
            reject(response, ResponseHelper.unauthorized("Bu istek daha önce kullanılmış."));
            return;
        }

        String biometricSignature = request.getHeader(BIOMETRIC_SIGNATURE_HEADER);
        boolean biometricVerified = biometricSignature != null && device.get().getBiometricPublicKey() != null
                && DeviceSignature.verify(device.get().getBiometricPublicKey(), stringToSign, biometricSignature);

        request.setAttribute(VerifiedDevice.REQUEST_ATTRIBUTE,
                new VerifiedDevice(device.get().getId(), device.get().getUserId(), biometricVerified));
        filterChain.doFilter(new CachedBodyHttpServletRequest(request, body), response);
    }

    private Optional<Device> findActiveDevice(String deviceId) {
        try {
            return deviceRepository.findByIdAndStatus(Long.valueOf(deviceId), DeviceStatus.ACTIVE);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private boolean isWithinTolerance(String timestamp) {
        try {
            Instant requestTime = Instant.ofEpochSecond(Long.parseLong(timestamp));
            Duration drift = Duration.between(requestTime, clock.instant()).abs();
            return drift.compareTo(properties.getSignature().getTolerance()) <= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Redis'e ulaşılamazsa istek geçer (fail-open); zaman damgası penceresi tekrar oynatmayı yine 5 dakikayla sınırlar.
     */
    private boolean markNonceUsed(Long deviceId, String nonce) {
        try {
            Boolean first = redisTemplate.opsForValue().setIfAbsent("sig:nonce:" + deviceId + ":" + nonce, "1",
                    properties.getSignature().getTolerance().multipliedBy(2));
            return !Boolean.FALSE.equals(first);
        } catch (DataAccessException e) {
            log.warn("Redis unavailable, nonce replay check skipped. deviceId={}", deviceId);
            return true;
        }
    }

    private void reject(HttpServletResponse response, ResponseMessage message) throws IOException {
        response.setStatus(message.getHttpStatusCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), message);
    }
}
