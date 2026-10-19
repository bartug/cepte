/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.appconfig.entity;

import com.bartugsevindik.cepte.backend.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * <h1>FeatureFlag</h1>
 * <p>Uygulamadaki bir özelliğin mağaza sürümü beklemeden açılıp kapatılması. Yarım kalmış bir özellik kapalı olarak
 * yayına alınabilir.</p>
 *
 * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 * @since 19.10.2026 - CP-1
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(schema = "appconfig", name = "feature_flag")
public class FeatureFlag extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "description", length = 200)
    private String description;
}
