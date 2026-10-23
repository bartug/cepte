/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.controller;

import com.bartugsevindik.cepte.backend.identity.CurrentUser;
import com.bartugsevindik.cepte.backend.identity.dto.AuthTokensDTO;
import com.bartugsevindik.cepte.backend.identity.dto.BiometricEnableRequest;
import com.bartugsevindik.cepte.backend.identity.dto.DeviceBindRequest;
import com.bartugsevindik.cepte.backend.identity.dto.OtpChallengeDTO;
import com.bartugsevindik.cepte.backend.identity.dto.OtpRequest;
import com.bartugsevindik.cepte.backend.identity.dto.OtpVerificationDTO;
import com.bartugsevindik.cepte.backend.identity.dto.OtpVerifyRequest;
import com.bartugsevindik.cepte.backend.identity.dto.PinLoginRequest;
import com.bartugsevindik.cepte.backend.identity.dto.RefreshRequest;
import com.bartugsevindik.cepte.backend.identity.dto.RegisterRequest;
import com.bartugsevindik.cepte.backend.identity.security.VerifiedDevice;
import com.bartugsevindik.cepte.backend.identity.service.AuthService;
import com.bartugsevindik.cepte.backend.identity.service.OtpService;
import com.bartugsevindik.cepte.common.dto.ResponseMessage;
import com.bartugsevindik.cepte.common.helpers.ResponseHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h1>AuthController</h1>
 * <p>Kayıt, cihaz bağlama, giriş ve oturum yenileme. Akış: docs/09-kimlik-ve-oturum.md</p>
 * <p>{@code /login}, {@code /login/biometric}, {@code /refresh} ve {@code /biometric} uçları cihaz imzası ister
 * (X-Device-Id, X-Timestamp, X-Nonce, X-Signature).</p>
 *
 * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 * @since 23.10.2026 - CP-2
 */
@Tag(name = "Kimlik ve Oturum", description = "OTP, kayıt, cihaz bağlama, PIN / Face ID ile giriş, token yenileme")
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final OtpService otpService;
    private final AuthService authService;

    /**
     * <h1>Doğrulama Kodu Gönder</h1>
     *
     * @param request     Telefon
     * @param httpRequest IP için
     * @return Kod bilgisi
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Operation(summary = "Doğrulama Kodu Gönder",
               description = "Telefona 6 haneli kod gönderir. Numara kayıtlı olsun olmasın aynı cevap döner. Aynı numaraya 60 saniyede bir, 10 dakikada en fazla 3 kod gönderilir.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Kod gönderildi.",
                    content = @Content(schema = @Schema(implementation = OtpChallengeDTO.class))),
            @ApiResponse(responseCode = "429", description = "Çok sık kod istendi. Retry-After header'ı bekleme süresini söyler.")
    })
    @PostMapping("/otp")
    public ResponseEntity<ResponseMessage> requestOtp(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = OtpRequest.class),
                    examples = @ExampleObject(value = """
                            {
                              "phone": "+905321234567"
                            }
                            """)))
            @Valid @RequestBody OtpRequest request, HttpServletRequest httpRequest) {
        OtpChallengeDTO dto = otpService.requestOtp(request.phone(), httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ResponseHelper.success("Doğrulama kodu gönderildi.", dto));
    }

    /**
     * <h1>Doğrulama Kodunu Kontrol Et</h1>
     *
     * @param request Kod
     * @return Doğrulama bileti ve numaranın kayıtlı olup olmadığı
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Operation(summary = "Doğrulama Kodunu Kontrol Et",
               description = "Kod doğruysa 10 dakika geçerli, tek kullanımlık bir bilet döner. Numara kayıtlıysa bilet cihaz bağlamada, değilse kayıtta kullanılır. 5 hatalı denemede kod geçersiz olur.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Telefon doğrulandı.",
                    content = @Content(schema = @Schema(implementation = OtpVerificationDTO.class))),
            @ApiResponse(responseCode = "401", description = "Kod hatalı ya da süresi dolmuş.")
    })
    @PostMapping("/otp/verify")
    public ResponseEntity<ResponseMessage> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        OtpVerificationDTO dto = otpService.verifyOtp(request.challengeId(), request.code());
        return ResponseEntity.ok(ResponseHelper.success("Telefon doğrulandı.", dto));
    }

    /**
     * <h1>Kayıt Ol</h1>
     *
     * @param request Bilet, PIN, cihaz anahtarı
     * @return Oturum
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Operation(summary = "Kayıt Ol", description = "Yeni kullanıcı ve ilk cihazı oluşturur, oturum açar.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Kayıt tamamlandı.",
                    content = @Content(schema = @Schema(implementation = AuthTokensDTO.class))),
            @ApiResponse(responseCode = "400", description = "PIN zayıf ya da cihaz anahtarı geçersiz."),
            @ApiResponse(responseCode = "401", description = "Bilet geçersiz ya da süresi dolmuş."),
            @ApiResponse(responseCode = "409", description = "Numara zaten kayıtlı.")
    })
    @PostMapping("/register")
    public ResponseEntity<ResponseMessage> register(@Valid @RequestBody RegisterRequest request) {
        AuthTokensDTO dto = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseHelper.created("Kayıt tamamlandı.", dto));
    }

    /**
     * <h1>Yeni Cihaz Bağla</h1>
     *
     * @param request Bilet, PIN, cihaz anahtarı
     * @return Oturum
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Operation(summary = "Yeni Cihaz Bağla",
               description = "Kayıtlı kullanıcı yeni cihazdan girer: OTP + PIN. Eski cihazın oturumları kapanır ve eski cihaz bağlantısı kaldırılır.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cihaz bağlandı.",
                    content = @Content(schema = @Schema(implementation = AuthTokensDTO.class))),
            @ApiResponse(responseCode = "401", description = "PIN hatalı ya da bilet geçersiz."),
            @ApiResponse(responseCode = "423", description = "PIN çok fazla hatalı girildi, geçici olarak kilitli.")
    })
    @PostMapping("/devices")
    public ResponseEntity<ResponseMessage> bindDevice(@Valid @RequestBody DeviceBindRequest request) {
        return ResponseEntity.ok(ResponseHelper.success("Cihaz bağlandı.", authService.bindDevice(request)));
    }

    /**
     * <h1>PIN ile Giriş Yap</h1>
     *
     * @param device  İmzası doğrulanmış cihaz
     * @param request PIN
     * @return Oturum
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Operation(summary = "PIN ile Giriş Yap", description = "Bağlı cihazda PIN ile oturum açar. Cihaz imzası zorunlu.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Giriş yapıldı.",
                    content = @Content(schema = @Schema(implementation = AuthTokensDTO.class))),
            @ApiResponse(responseCode = "401", description = "PIN hatalı ya da cihaz imzası geçersiz."),
            @ApiResponse(responseCode = "423", description = "PIN geçici olarak kilitli.")
    })
    @PostMapping("/login")
    public ResponseEntity<ResponseMessage> login(@Parameter(hidden = true) @RequestAttribute(VerifiedDevice.REQUEST_ATTRIBUTE) VerifiedDevice device,
                                                 @Valid @RequestBody PinLoginRequest request) {
        return ResponseEntity.ok(ResponseHelper.success("Giriş yapıldı.", authService.loginWithPin(device, request.pin())));
    }

    /**
     * <h1>Face ID ile Giriş Yap</h1>
     *
     * @param device İmzası doğrulanmış cihaz
     * @return Oturum
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Operation(summary = "Face ID ile Giriş Yap",
               description = "Cihaz imzasına ek olarak X-Biometric-Signature ister: aynı metin, Face ID ile açılan anahtarla imzalanır.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Giriş yapıldı.",
                    content = @Content(schema = @Schema(implementation = AuthTokensDTO.class))),
            @ApiResponse(responseCode = "401", description = "Biyometrik imza yok ya da geçersiz.")
    })
    @PostMapping("/login/biometric")
    public ResponseEntity<ResponseMessage> loginWithBiometric(@Parameter(hidden = true) @RequestAttribute(VerifiedDevice.REQUEST_ATTRIBUTE) VerifiedDevice device) {
        return ResponseEntity.ok(ResponseHelper.success("Giriş yapıldı.", authService.loginWithBiometric(device)));
    }

    /**
     * <h1>Oturumu Yenile</h1>
     *
     * @param device  İmzası doğrulanmış cihaz
     * @param request Refresh token
     * @return Yeni token'lar
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Operation(summary = "Oturumu Yenile",
               description = "Refresh token tek kullanımlıktır, her yenilemede yenisi döner. Kullanılmış bir token tekrar gönderilirse oturum ailesi tamamen kapatılır.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Oturum yenilendi.",
                    content = @Content(schema = @Schema(implementation = AuthTokensDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token geçersiz, kullanılmış ya da süresi dolmuş.")
    })
    @PostMapping("/refresh")
    public ResponseEntity<ResponseMessage> refresh(@Parameter(hidden = true) @RequestAttribute(VerifiedDevice.REQUEST_ATTRIBUTE) VerifiedDevice device,
                                                   @Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(ResponseHelper.success("Oturum yenilendi.", authService.refresh(device, request.refreshToken())));
    }

    /**
     * <h1>Face ID ile Girişi Aç</h1>
     *
     * @param jwt     Access token
     * @param device  İmzası doğrulanmış cihaz
     * @param request Biyometrik anahtar
     * @return Boş cevap
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Operation(summary = "Face ID ile Girişi Aç", description = "Face ID ile korunan anahtarın public key'ini kaydeder. Token ve cihaz imzası birlikte gerekir.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Face ID ile giriş açıldı."),
            @ApiResponse(responseCode = "401", description = "Oturum ya da imza geçersiz.")
    })
    @PutMapping("/biometric")
    public ResponseEntity<ResponseMessage> enableBiometric(@AuthenticationPrincipal Jwt jwt,
                                                           @Parameter(hidden = true) @RequestAttribute(VerifiedDevice.REQUEST_ATTRIBUTE) VerifiedDevice device,
                                                           @Valid @RequestBody BiometricEnableRequest request) {
        authService.enableBiometric(CurrentUser.from(jwt), device, request);
        return ResponseEntity.ok(ResponseHelper.success("Face ID ile giriş açıldı.", null));
    }

    /**
     * <h1>Face ID ile Girişi Kapat</h1>
     *
     * @param jwt Access token
     * @return Boş cevap
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Operation(summary = "Face ID ile Girişi Kapat")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Face ID ile giriş kapatıldı.")})
    @DeleteMapping("/biometric")
    public ResponseEntity<ResponseMessage> disableBiometric(@AuthenticationPrincipal Jwt jwt) {
        authService.disableBiometric(CurrentUser.from(jwt));
        return ResponseEntity.ok(ResponseHelper.success("Face ID ile giriş kapatıldı.", null));
    }

    /**
     * <h1>Çıkış Yap</h1>
     *
     * @param jwt Access token
     * @return Boş cevap
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Operation(summary = "Çıkış Yap", description = "Bu oturumu kapatır. Cihaz bağlı kalır; PIN ile tekrar girilebilir.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Çıkış yapıldı.")})
    @PostMapping("/logout")
    public ResponseEntity<ResponseMessage> logout(@AuthenticationPrincipal Jwt jwt) {
        authService.logout(CurrentUser.from(jwt));
        return ResponseEntity.ok(ResponseHelper.success("Çıkış yapıldı.", null));
    }
}
