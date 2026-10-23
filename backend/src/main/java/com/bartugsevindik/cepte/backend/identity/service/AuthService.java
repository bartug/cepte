/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.service;

import com.bartugsevindik.cepte.backend.identity.CurrentUser;
import com.bartugsevindik.cepte.backend.identity.dto.AuthTokensDTO;
import com.bartugsevindik.cepte.backend.identity.dto.BiometricEnableRequest;
import com.bartugsevindik.cepte.backend.identity.dto.DeviceBindRequest;
import com.bartugsevindik.cepte.backend.identity.dto.MeDTO;
import com.bartugsevindik.cepte.backend.identity.dto.RegisterRequest;
import com.bartugsevindik.cepte.backend.identity.security.VerifiedDevice;
import org.springframework.stereotype.Service;

@Service
public interface AuthService {

    /**
     * <h1>Kayıt</h1>
     *
     * @param request Doğrulama bileti, PIN ve cihaz anahtarı
     * @return Oturum
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    AuthTokensDTO register(RegisterRequest request);

    /**
     * <h1>Yeni Cihaz Bağlama</h1>
     * <p>Kayıtlı kullanıcı yeni bir cihazdan girer. Eski cihazlar kapatılır (tek aktif cihaz).</p>
     *
     * @param request Doğrulama bileti, PIN ve cihaz anahtarı
     * @return Oturum
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    AuthTokensDTO bindDevice(DeviceBindRequest request);

    /**
     * <h1>PIN ile Giriş</h1>
     *
     * @param device İmzası doğrulanmış cihaz
     * @param pin    PIN
     * @return Oturum
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    AuthTokensDTO loginWithPin(VerifiedDevice device, String pin);

    /**
     * <h1>Face ID ile Giriş</h1>
     *
     * @param device İmzası doğrulanmış cihaz
     * @return Oturum
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    AuthTokensDTO loginWithBiometric(VerifiedDevice device);

    /**
     * <h1>Token Yenileme</h1>
     *
     * @param device       İmzası doğrulanmış cihaz
     * @param refreshToken Refresh token
     * @return Yeni token'lar
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    AuthTokensDTO refresh(VerifiedDevice device, String refreshToken);

    /**
     * <h1>Face ID ile Girişi Açma</h1>
     *
     * @param user    Oturum sahibi
     * @param device  İmzası doğrulanmış cihaz
     * @param request Biyometrik anahtar
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    void enableBiometric(CurrentUser user, VerifiedDevice device, BiometricEnableRequest request);

    /**
     * <h1>Face ID ile Girişi Kapatma</h1>
     *
     * @param user Oturum sahibi
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    void disableBiometric(CurrentUser user);

    /**
     * <h1>Çıkış</h1>
     *
     * @param user Oturum sahibi
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    void logout(CurrentUser user);

    /**
     * <h1>Oturum Sahibi Bilgisi</h1>
     *
     * @param user Oturum sahibi
     * @return Kullanıcı ve cihaz bilgisi
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    MeDTO getMe(CurrentUser user);
}
