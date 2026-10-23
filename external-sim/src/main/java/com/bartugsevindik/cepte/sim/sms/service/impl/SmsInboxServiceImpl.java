/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.sim.sms.service.impl;

import com.bartugsevindik.cepte.sim.sms.dto.SmsDTO;
import com.bartugsevindik.cepte.sim.sms.dto.SmsSendRequest;
import com.bartugsevindik.cepte.sim.sms.service.SmsInboxService;
import com.bartugsevindik.cepte.common.time.ControllableClock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsInboxServiceImpl implements SmsInboxService {
    private static final int CAPACITY = 500;
    private static final Pattern CODE = Pattern.compile("\\b(\\d{6})\\b");

    private final ControllableClock clock;
    private final Deque<SmsDTO> inbox = new ArrayDeque<>();

    /**
     * <h1>SMS Alma</h1>
     *
     * @param request Alıcı ve mesaj
     * @return Kaydedilen SMS
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Override
    public synchronized SmsDTO receive(@NotNull SmsSendRequest request) {
        Matcher matcher = CODE.matcher(request.text());
        SmsDTO sms = new SmsDTO(request.phone(), request.text(), matcher.find() ? matcher.group(1) : null, clock.instant());
        inbox.addFirst(sms);
        if (inbox.size() > CAPACITY) {
            inbox.removeLast();
        }
        log.info("SMS received. phone={}, text={}", request.phone(), request.text());
        return sms;
    }

    /**
     * <h1>Gelen Kutusunu Listeleme</h1>
     *
     * @param phone Sadece bu numaraya gidenler, boşsa hepsi
     * @param limit En fazla kaç mesaj
     * @return En yeni mesaj başta
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 23.10.2026 - CP-2
     */
    @Override
    public synchronized List<SmsDTO> list(String phone, int limit) {
        return inbox.stream()
                .filter(sms -> phone == null || phone.isBlank() || sms.phone().equals(phone))
                .limit(limit)
                .toList();
    }
}
