/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.appconfig.mapper;

import com.bartugsevindik.cepte.backend.appconfig.dto.MiniAppDTO;
import com.bartugsevindik.cepte.backend.appconfig.entity.MiniApp;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MiniAppMapper {

    MiniAppDTO toDTO(MiniApp miniApp);

    List<MiniAppDTO> toDTOList(List<MiniApp> miniApps);
}
