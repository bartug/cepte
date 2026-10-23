/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableJpaRepositories(basePackages = {"com.bartugsevindik.cepte.backend.identity.repository"})
public class IdentityModuleConfig {

    /**
     * Argon2id: bellek yoğun olduğu için GPU ile toplu deneme bcrypt'e göre çok daha pahalı.
     * 6 haneli PIN'in sadece 10^6 ihtimali var; hash'in yavaş olması burada kritik.
     */
    @Bean
    public PasswordEncoder pinEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
