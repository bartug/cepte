/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.backend.simulation.service;

import com.bartugsevindik.cepte.backend.simulation.dto.ClockDTO;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public interface SimulationClockService {

    /**
     * <h1>Simülatör Saatine Eşitleme</h1>
     * <p>Simülatör saati ileri sardığında çağırır. Saat geri gitmez.</p>
     *
     * @param now Simülatördeki an
     * @return Backend'in yeni saati
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 19.10.2026 - CP-1
     */
    ClockDTO applySimulatorTime(Instant now);

    /**
     * <h1>Simülatörden Saat Çekme</h1>
     * <p>Açılışta çağrılır; backend kapalıyken ileri sarılan zaman kaçırılmasın.</p>
     *
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 19.10.2026 - CP-1
     */
    void pullFromSimulator();
}
