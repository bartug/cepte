/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.sim;

import com.bartugsevindik.cepte.common.exception.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Import;

/**
 * <h1>ExternalSimApplication</h1>
 * <p>Gerçek dünyadaki kurumların (NVİ, MASAK, KKB, PSP, FAST, anlaşmalı bankalar) sahte hali ve zaman makinesi.
 * Backend bu uygulamayı gerçek bir dış sistem gibi HTTP ile çağırır.</p>
 *
 * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 * @since 19.10.2026 - CP-1
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@Import(GlobalExceptionHandler.class)
public class ExternalSimApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExternalSimApplication.class, args);
    }
}
