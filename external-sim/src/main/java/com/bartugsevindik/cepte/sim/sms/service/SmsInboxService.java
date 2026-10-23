/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.sim.sms.service;

import com.bartugsevindik.cepte.sim.sms.dto.SmsDTO;
import com.bartugsevindik.cepte.sim.sms.dto.SmsSendRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface SmsInboxService {

    /**
     * <h1>SMS Alma</h1>
     * <p>Gerçek bir operatöre gitmez; gelen kutusuna eklenir.</p>
     *
     * @param request Alıcı ve mesaj
     * @return Kaydedilen SMS
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    SmsDTO receive(SmsSendRequest request);

    /**
     * <h1>Gelen Kutusunu Listeleme</h1>
     *
     * @param phone Sadece bu numaraya gidenler, boşsa hepsi
     * @param limit En fazla kaç mesaj
     * @return En yeni mesaj başta
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    List<SmsDTO> list(String phone, int limit);
}
