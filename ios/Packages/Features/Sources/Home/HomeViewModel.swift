//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import AppConfiguration
import Foundation
import MiniAppKit
import Observation

/// Ana sayfanın durumu.
///
/// `miniApps` hesaplanan bir özellik: `configStore.config` değişince (konfigürasyon yenilendiğinde) SwiftUI bunu
/// fark eder ve ızgarayı yeniden çizer. Elle "listeyi güncelle" çağrısı yok.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
@MainActor
@Observable
public final class HomeViewModel {
    public var kycLevel: Int

    private let configStore: AppConfigStore
    private let registry: MiniAppRegistry

    public init(configStore: AppConfigStore, registry: MiniAppRegistry, kycLevel: Int = 0) {
        self.configStore = configStore
        self.registry = registry
        self.kycLevel = kycLevel
    }

    public var miniApps: [MiniAppEntry] {
        registry.entries(from: configStore.config?.miniApps ?? [], kycLevel: kycLevel)
    }

    /// Selamlama sunucu saatine göre; simülasyonda saat ileri sarıldıysa onu takip eder.
    public var greeting: String {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: configStore.config?.timeZone ?? "Europe/Istanbul") ?? .current
        switch calendar.component(.hour, from: configStore.serverNow()) {
        case 5..<12: return "Günaydın"
        case 12..<18: return "İyi günler"
        case 18..<23: return "İyi akşamlar"
        default: return "İyi geceler"
        }
    }

    public func refresh() async {
        await configStore.load()
    }
}
