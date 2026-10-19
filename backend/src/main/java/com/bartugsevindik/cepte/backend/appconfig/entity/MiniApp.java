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
 * <h1>MiniApp</h1>
 * <p>Süper uygulamanın içindeki bir dikey (yemek, taksi). Uygulama, kodunu tanımadığı mini app'leri göstermez;
 * yeni bir dikey önce iOS sürümüyle gelir, sonra buradan açılır.</p>
 *
 * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 * @since 19.10.2026 - CP-1
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(schema = "appconfig", name = "mini_app")
public class MiniApp extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "title", nullable = false, length = 50)
    private String title;

    // SF Symbols ikon adı, iOS doğrudan kullanır
    @Column(name = "icon", nullable = false, length = 50)
    private String icon;

    @Column(name = "required_kyc_level", nullable = false)
    private Integer requiredKycLevel;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}
