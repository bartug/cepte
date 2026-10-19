/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.appconfig.repository;

import com.bartugsevindik.cepte.backend.appconfig.entity.MiniApp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MiniAppRepository extends JpaRepository<MiniApp, Long> {

    List<MiniApp> findAllByOrderBySortOrderAsc();

    Optional<MiniApp> findByCode(String code);
}
