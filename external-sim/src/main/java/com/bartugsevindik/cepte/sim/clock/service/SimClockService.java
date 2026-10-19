/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.sim.clock.service;

import com.bartugsevindik.cepte.sim.clock.dto.ClockAdvanceRequest;
import com.bartugsevindik.cepte.sim.clock.dto.SimClockDTO;
import org.springframework.stereotype.Service;

@Service
public interface SimClockService {

    /**
     * <h1>Saati Getirme</h1>
     *
     * @return Simülatörün saati
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 19.10.2026 - CP-1
     */
    SimClockDTO getClock();

    /**
     * <h1>Zamanı İleri Sarma</h1>
     * <p>Simülatörün saatini ilerletir ve backend'e bildirir.</p>
     *
     * @param request Süre ya da hedef an
     * @return Yeni saat
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 19.10.2026 - CP-1
     */
    SimClockDTO advance(ClockAdvanceRequest request);
}
