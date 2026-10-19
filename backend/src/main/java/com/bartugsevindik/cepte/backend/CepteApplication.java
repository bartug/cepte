/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend;

import com.bartugsevindik.cepte.common.exception.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
// common modülü bu paketin dışında; component scan'e girmediği için handler açıkça eklenir
@Import(GlobalExceptionHandler.class)
public class CepteApplication {

    public static void main(String[] args) {
        SpringApplication.run(CepteApplication.class, args);
    }
}
