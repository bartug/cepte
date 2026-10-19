/*
 * Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 *
 */

package com.bartugsevindik.cepte.common.time;

import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <h1>ControllableClock</h1>
 * <p>Gerçek saatin üzerine bir fark (offset) ekleyen saat. Zaman ileri sarıldığında saat durmaz, yeni noktadan
 * akmaya devam eder; böylece "31 gün sonra" senaryosunda timeout ve TTL hesapları da doğal çalışır.</p>
 * <p>Sadece {@code local} ve {@code test} ortamlarında kullanılır. Production'da sistem saati kullanılır.</p>
 *
 * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
 * @since 19.10.2026 - CP-1
 */
public class ControllableClock extends Clock {

    private final Clock base;
    private final AtomicReference<Duration> offset;

    public ControllableClock(@NotNull Clock base) {
        this(base, new AtomicReference<>(Duration.ZERO));
    }

    private ControllableClock(Clock base, AtomicReference<Duration> offset) {
        this.base = base;
        this.offset = offset;
    }

    @Override
    public ZoneId getZone() {
        return base.getZone();
    }

    /**
     * Aynı offset'i paylaşır; farklı zone ile alınan saat de ileri sarmadan etkilenir.
     */
    @Override
    public Clock withZone(ZoneId zone) {
        return new ControllableClock(base.withZone(zone), offset);
    }

    @Override
    public Instant instant() {
        return base.instant().plus(offset.get());
    }

    /**
     * <h1>Zamanı İleri Sarma</h1>
     * <p>Geri sarma desteklenmez; geçmişe dönen saat yevmiye ve vade sıralamasını bozar.</p>
     *
     * @param duration İleri sarılacak süre
     * @return Yeni an
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 19.10.2026 - CP-1
     */
    public Instant advance(@NotNull Duration duration) {
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Saat geri sarılamaz.");
        }
        offset.accumulateAndGet(duration, Duration::plus);
        return instant();
    }

    /**
     * <h1>Belirli Ana Ayarlama</h1>
     * <p>Simülatörün saatini backend'e taşırken kullanılır. Hedef an şu anki zamandan önceyse saat geri gitmez.</p>
     *
     * @param target Hedef an
     * @return Yeni an
     * @author Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
     * @since 19.10.2026 - CP-1
     */
    public Instant moveTo(@NotNull Instant target) {
        Duration diff = Duration.between(instant(), target);
        return diff.isNegative() ? instant() : advance(diff);
    }

    public Duration getOffset() {
        return offset.get();
    }

    // Sadece simülatör sıfırlanırken; çalışan senaryoda kullanılmaz
    public void reset() {
        offset.set(Duration.ZERO);
    }
}
