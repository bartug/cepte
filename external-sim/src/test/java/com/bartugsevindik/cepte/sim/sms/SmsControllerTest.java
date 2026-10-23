/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.sim.sms;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "application.sim.backend-url=http://localhost:1")
@AutoConfigureMockMvc
class SmsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void gonderilenSmsKoduylaGelenKutusundaGorunur() throws Exception {
        send("+905320000001", "Cepte doğrulama kodun: 482915. Bu kodu kimseyle paylaşma.");
        send("+905320000002", "Başka bir numara 111222");

        mockMvc.perform(get("/sim/sms").param("phone", "+905320000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.object.length()").value(1))
                .andExpect(jsonPath("$.object[0].code").value("482915"));
    }

    @Test
    void enYeniMesajBasta() throws Exception {
        send("+905320000003", "İlk 100001");
        send("+905320000003", "İkinci 100002");

        mockMvc.perform(get("/sim/sms").param("phone", "+905320000003"))
                .andExpect(jsonPath("$.object[0].code").value("100002"));
    }

    private void send(String phone, String text) throws Exception {
        mockMvc.perform(post("/sim/sms").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"text\":\"" + text + "\"}"))
                .andExpect(status().isOk());
    }
}
