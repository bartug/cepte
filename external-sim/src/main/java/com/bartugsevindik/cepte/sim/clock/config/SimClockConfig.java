/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.sim.clock.config;

import com.bartugsevindik.cepte.common.time.ControllableClock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

@Configuration
public class SimClockConfig {

    // Simülatörün saati her zaman kontrol edilebilir; zamanın sahibi burası
    @Bean
    public ControllableClock clock(SimProperties properties) {
        return new ControllableClock(Clock.system(properties.getZone()));
    }

    @Bean
    public RestClient backendRestClient(RestClient.Builder builder, SimProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        return builder.clone()
                .baseUrl(properties.getBackendUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
