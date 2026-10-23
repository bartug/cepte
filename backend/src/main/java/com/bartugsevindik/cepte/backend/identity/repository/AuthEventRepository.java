/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.repository;

import com.bartugsevindik.cepte.backend.identity.entity.AuthEvent;
import com.bartugsevindik.cepte.backend.identity.enums.AuthEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuthEventRepository extends JpaRepository<AuthEvent, Long> {

    List<AuthEvent> findAllByUserIdOrderByIdAsc(Long userId);

    boolean existsByUserIdAndType(Long userId, AuthEventType type);
}
