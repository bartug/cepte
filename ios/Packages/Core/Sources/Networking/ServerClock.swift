//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import Foundation
import Synchronization

/// Sunucu saati. Simülasyonda zaman ileri sarıldığında cihaz saatiyle günlerce fark olabilir; imzalı isteklerin
/// zaman damgası ve ekrandaki tarihler bu saatten alınır.
///
/// Farklı thread'lerden okunup yazıldığı için değer `Mutex` içinde. Java'daki `AtomicLong` gibi düşün.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 23.10.2026 - CP-2
public final class ServerClock: Sendable {
    private let offset = Mutex<TimeInterval>(0)
    private let deviceNow: @Sendable () -> Date

    public init(deviceNow: @escaping @Sendable () -> Date = { Date() }) {
        self.deviceNow = deviceNow
    }

    public func now() -> Date {
        deviceNow().addingTimeInterval(offsetSeconds)
    }

    public var offsetSeconds: TimeInterval {
        offset.withLock { $0 }
    }

    public func update(serverTime: Date) {
        let difference = serverTime.timeIntervalSince(deviceNow())
        offset.withLock { $0 = difference }
    }
}
