/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.appconfig;

import com.bartugsevindik.cepte.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AppConfigIntegrationTest extends AbstractIntegrationTest {

    @AfterEach
    void restore() throws Exception {
        toggle("/v1/admin/app/mini-apps/TAXI", true);
        toggle("/v1/admin/app/features/CREDIT", false);
    }

    @Test
    void miniAppListesiSiraylaDoner() throws Exception {
        mockMvc.perform(get("/v1/app/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.object.miniApps[*].code", contains("FOOD", "TAXI", "MARKET")))
                .andExpect(jsonPath("$.object.miniApps[2].enabled").value(false))
                .andExpect(jsonPath("$.object.features.CREDIT").value(false))
                .andExpect(jsonPath("$.object.timeZone").value("Europe/Istanbul"))
                .andExpect(jsonPath("$.object.simulatedClock").value(true))
                .andExpect(jsonPath("$.object.serverTime", notNullValue()));
    }

    @Test
    void kapatilanMiniAppKonfigurasyondaKapaliGorunur() throws Exception {
        toggle("/v1/admin/app/mini-apps/TAXI", false);

        mockMvc.perform(get("/v1/app/config"))
                .andExpect(jsonPath("$.object.miniApps[1].code").value("TAXI"))
                .andExpect(jsonPath("$.object.miniApps[1].enabled").value(false));
    }

    @Test
    void featureFlagAcilir() throws Exception {
        toggle("/v1/admin/app/features/CREDIT", true);

        mockMvc.perform(get("/v1/app/config"))
                .andExpect(jsonPath("$.object.features.CREDIT").value(true));
    }

    @Test
    void olmayanMiniApp404() throws Exception {
        mockMvc.perform(put("/v1/admin/app/mini-apps/CINEMA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void enabledZorunlu() throws Exception {
        mockMvc.perform(put("/v1/admin/app/features/CREDIT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("enabled: must not be null"));
    }

    private void toggle(String path, boolean enabled) throws Exception {
        mockMvc.perform(put(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": " + enabled + "}"))
                .andExpect(status().isOk());
    }
}
