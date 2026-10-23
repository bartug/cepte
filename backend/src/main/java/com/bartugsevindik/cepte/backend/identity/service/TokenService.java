/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.service;

import com.bartugsevindik.cepte.backend.identity.dto.AuthTokensDTO;
import com.bartugsevindik.cepte.backend.identity.enums.TokenRevokeReason;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface TokenService {

    /**
     * <h1>Oturum Açma</h1>
     * <p>Yeni bir refresh token ailesi başlatır. Aynı cihazdaki önceki aileler kapatılır; bir cihazda tek oturum olur.</p>
     *
     * @param userId   Kullanıcı
     * @param deviceId Cihaz
     * @return Token'lar
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    AuthTokensDTO issue(Long userId, Long deviceId);

    /**
     * <h1>Token Yenileme</h1>
     * <p>Kullanılmış bir refresh token tekrar gelirse ailenin tamamı iptal edilir.</p>
     *
     * @param refreshToken Refresh token
     * @param deviceId     İsteği imzalayan cihaz
     * @return Yeni token'lar
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    AuthTokensDTO rotate(String refreshToken, Long deviceId);

    /**
     * <h1>Oturum Ailesini Kapatma</h1>
     *
     * @param familyId Aile
     * @param reason   Sebep
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    void revokeFamily(UUID familyId, TokenRevokeReason reason);

    /**
     * <h1>Cihazın Tüm Oturumlarını Kapatma</h1>
     *
     * @param deviceId Cihaz
     * @param reason   Sebep
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    void revokeDevice(Long deviceId, TokenRevokeReason reason);
}
