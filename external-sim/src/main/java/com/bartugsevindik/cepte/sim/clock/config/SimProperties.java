/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.sim.clock.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.ZoneId;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "application.sim")
public class SimProperties {

    @NotBlank
    private String backendUrl = "http://localhost:8080";

    @NotBlank
    private String internalToken = "local-dev-only-internal-token";

    @NotNull
    private ZoneId zone = ZoneId.of("Europe/Istanbul");
}
