/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.sim.clock;

import com.bartugsevindik.cepte.common.time.ControllableClock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Backend yok; bildirim kapalı porta gidip düşer, sarma yine geçerli olmalı
@SpringBootTest(properties = "application.sim.backend-url=http://localhost:1")
@AutoConfigureMockMvc
class SimClockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ControllableClock clock;

    @Test
    void gunIleIleriSarilir() throws Exception {
        Duration before = clock.getOffset();

        advance("{\"days\": 31}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.object.backendNotified").value(false));

        assertThat(clock.getOffset().minus(before)).isEqualTo(Duration.ofDays(31));
    }

    @Test
    void hedefAnaSarilir() throws Exception {
        LocalDateTime target = LocalDateTime.ofInstant(clock.instant(), ZoneId.of("Europe/Istanbul")).plusYears(1).withNano(0);

        advance("{\"until\": \"" + target + "\"}").andExpect(status().isOk());

        assertThat(LocalDateTime.ofInstant(clock.instant(), ZoneId.of("Europe/Istanbul"))).isAfterOrEqualTo(target);
    }

    @Test
    void gecmisHedefReddedilir() throws Exception {
        advance("{\"until\": \"2020-01-01T00:00:00\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Hedef an geçmişte olamaz. Saat geri sarılamaz."));
    }

    @Test
    void sureVeHedefBirlikteVerilemez() throws Exception {
        advance("{\"days\": 1, \"until\": \"2030-01-01T00:00:00\"}").andExpect(status().isBadRequest());
    }

    @Test
    void saatOkunur() throws Exception {
        mockMvc.perform(get("/sim/clock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.object.now").isNotEmpty())
                .andExpect(jsonPath("$.object.offset").isNotEmpty())
                .andExpect(jsonPath("$.object.backendNotified").doesNotExist());
    }

    private org.springframework.test.web.servlet.ResultActions advance(String body) throws Exception {
        return mockMvc.perform(post("/sim/clock/advance").contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
