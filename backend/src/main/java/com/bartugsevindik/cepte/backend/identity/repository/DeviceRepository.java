/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.identity.repository;

import com.bartugsevindik.cepte.backend.identity.entity.Device;
import com.bartugsevindik.cepte.backend.identity.enums.DeviceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByIdAndStatus(Long id, DeviceStatus status);

    List<Device> findAllByUserIdAndStatus(Long userId, DeviceStatus status);

    boolean existsByIdAndStatus(Long id, DeviceStatus status);
}
