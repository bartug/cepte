/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.simulation;

import com.bartugsevindik.cepte.backend.simulation.controller.SimulationClockController;
import com.bartugsevindik.cepte.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RecordApplicationEvents
class SimulationClockIntegrationTest extends AbstractIntegrationTest {

    private static final String TOKEN = "local-dev-only-internal-token";

    @Autowired
    private Clock clock;

    @Autowired
    private ApplicationEvents events;

    @Test
    void simulatorSaatiIleriSarincaBackendDeIlerler() throws Exception {
        Instant target = clock.instant().plus(Duration.ofDays(31));

        sync(TOKEN, target).andExpect(status().isOk());

        assertThat(clock.instant()).isAfterOrEqualTo(target);
        assertThat(events.stream(ClockAdvancedEvent.class)).hasSize(1);
    }

    @Test
    void gecmisZamanGelirseSaatGeriGitmez() throws Exception {
        Instant now = clock.instant();

        sync(TOKEN, now.minus(Duration.ofDays(3))).andExpect(status().isOk());

        assertThat(clock.instant()).isAfterOrEqualTo(now);
        assertThat(events.stream(ClockAdvancedEvent.class)).isEmpty();
    }

    @Test
    void yanlisTokenReddedilir() throws Exception {
        Instant now = clock.instant();

        sync("wrong", now.plus(Duration.ofDays(1))).andExpect(status().isUnauthorized());

        assertThat(clock.instant()).isBefore(now.plus(Duration.ofHours(1)));
    }

    private org.springframework.test.web.servlet.ResultActions sync(String token, Instant now) throws Exception {
        return mockMvc.perform(post("/internal/simulation/clock")
                .header(SimulationClockController.INTERNAL_TOKEN_HEADER, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"now\": \"" + now + "\"}"));
    }
}
