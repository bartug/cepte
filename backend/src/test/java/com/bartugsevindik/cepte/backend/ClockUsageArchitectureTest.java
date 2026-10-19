/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Zaman sadece enjekte edilen {@link java.time.Clock} üzerinden okunur (ADR-004). Parametresiz {@code now()}
 * sistem saatini okur; simülasyonda ileri sarılan zamanı görmez ve testler sessizce yanlış sonuç üretir.
 */
class ClockUsageArchitectureTest {

    private static final Set<String> TIME_TYPES = Set.of(
            "java.time.Instant", "java.time.LocalDate", "java.time.LocalDateTime", "java.time.LocalTime",
            "java.time.ZonedDateTime", "java.time.OffsetDateTime", "java.time.YearMonth", "java.time.Year");

    private static final DescribedPredicate<JavaMethodCall> SYSTEM_TIME_CALL = new DescribedPredicate<>("sistem saatini okuyan çağrı") {
        @Override
        public boolean test(JavaMethodCall call) {
            String owner = call.getTargetOwner().getName();
            String name = call.getName();
            boolean parameterlessNow = TIME_TYPES.contains(owner) && name.equals("now")
                    && call.getTarget().getRawParameterTypes().isEmpty();
            boolean systemMillis = owner.equals("java.lang.System") && (name.equals("currentTimeMillis"));
            boolean systemClock = owner.equals("java.time.Clock") && (name.equals("systemUTC") || name.equals("systemDefaultZone"));
            return parameterlessNow || systemMillis || systemClock;
        }
    };

    @Test
    void zamanSadeceClockBeanindenOkunur() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.bartugsevindik.cepte");

        noClasses().should().callMethodWhere(SYSTEM_TIME_CALL)
                .because("zaman enjekte edilen Clock'tan alınmalı (ADR-004)")
                .check(classes);
    }
}
