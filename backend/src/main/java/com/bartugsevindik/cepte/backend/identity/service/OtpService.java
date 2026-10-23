/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.service;

import com.bartugsevindik.cepte.backend.identity.dto.OtpChallengeDTO;
import com.bartugsevindik.cepte.backend.identity.dto.OtpVerificationDTO;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface OtpService {

    /**
     * <h1>Doğrulama Kodu Gönderme</h1>
     * <p>Numara kayıtlı olsun ya da olmasın aynı cevap döner.</p>
     *
     * @param phone E.164 telefon
     * @param ip    İstemci IP'si
     * @return Kod bilgisi
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    OtpChallengeDTO requestOtp(String phone, String ip);

    /**
     * <h1>Doğrulama Kodu Kontrolü</h1>
     *
     * @param challengeId Kod isteğinin id'si
     * @param code        Kullanıcının girdiği kod
     * @return Tek kullanımlık doğrulama bileti
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    OtpVerificationDTO verifyOtp(UUID challengeId, String code);

    /**
     * <h1>Doğrulama Biletini Kullanma</h1>
     * <p>Çağıranın transaction'ına katılır; kayıt yarıda kalırsa bilet de kullanılmamış sayılır.</p>
     *
     * @param verificationToken Bilet
     * @return Doğrulanmış telefon
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    String consumeVerification(String verificationToken);
}
