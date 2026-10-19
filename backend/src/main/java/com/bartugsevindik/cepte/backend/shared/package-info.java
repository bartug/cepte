/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

/**
 * Tüm modüllerin kullandığı altyapı: saat, JPA auditing, Swagger, ortak entity.
 * İş kuralı içermez; içine domain kodu girerse diğer modüllerin gizli bağımlılığı olur.
 */
@ApplicationModule(type = ApplicationModule.Type.OPEN)
package com.bartugsevindik.cepte.backend.shared;

import org.springframework.modulith.ApplicationModule;
