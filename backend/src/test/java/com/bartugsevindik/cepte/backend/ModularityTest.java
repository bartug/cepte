/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Bir modül diğerinin iç paketine (entity, repository, service.impl) erişirse ya da döngüsel bağımlılık oluşursa
 * build kırılır. Modül diyagramları target/spring-modulith-docs altına yazılır.
 */
class ModularityTest {

    private final ApplicationModules modules = ApplicationModules.of(CepteApplication.class);

    @Test
    void modulSinirlariKorunur() {
        modules.verify();
    }

    @Test
    void modulDokumantasyonuUretilir() {
        new Documenter(modules).writeModulesAsPlantUml().writeIndividualModulesAsPlantUml();
    }
}
