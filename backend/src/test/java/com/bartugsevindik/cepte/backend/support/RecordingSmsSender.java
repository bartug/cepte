/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.support;

import com.bartugsevindik.cepte.backend.notification.SmsSender;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Testlerde SMS simülatöre gitmez; burada toplanır, OTP kodu buradan okunur.
 */
public class RecordingSmsSender implements SmsSender {
    private static final Pattern CODE = Pattern.compile("\\b(\\d{6})\\b");

    private final Map<String, List<String>> messages = new ConcurrentHashMap<>();

    @Override
    public void send(String phone, String text) {
        messages.computeIfAbsent(phone, key -> new CopyOnWriteArrayList<>()).add(text);
    }

    public String lastCode(String phone) {
        List<String> received = messages.getOrDefault(phone, List.of());
        Matcher matcher = CODE.matcher(received.getLast());
        if (!matcher.find()) {
            throw new IllegalStateException("SMS'te kod yok: " + received.getLast());
        }
        return matcher.group(1);
    }

    public int count(String phone) {
        return messages.getOrDefault(phone, List.of()).size();
    }
}
