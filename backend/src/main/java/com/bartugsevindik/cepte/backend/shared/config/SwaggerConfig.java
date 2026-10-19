/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Cepte API")
                        .description("""
                                Cepte iOS uygulamasının konuştuğu backend.

                                - `/v1/**`: mobil uygulama uç noktaları
                                - `/v1/admin/**`: operasyon uç noktaları
                                - `/internal/**`: simülatör ve iç servis çağrıları (Swagger'da gizli)
                                """)
                        .version("1.0")
                        .contact(new Contact().name("Bartuğ Sevindik").email("bartug.sevindik@taksidepos.com")));
    }
}
