/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.simulation.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "application.simulation")
public class SimulationProperties {

    @NotBlank
    private String baseUrl = "http://localhost:8091";

    // external-sim'in /internal çağrılarında gönderdiği token
    @NotBlank
    private String internalToken = "local-dev-only-internal-token";
}
