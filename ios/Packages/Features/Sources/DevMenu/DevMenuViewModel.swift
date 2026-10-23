//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import AppConfiguration
import Foundation
import Networking
import Observation

/// Geliştirici menüsünün durumu: ortam bilgisi, zaman makinesi, konfigürasyon.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
@MainActor
@Observable
public final class DevMenuViewModel {
    public let environment: APIEnvironment
    public let configStore: AppConfigStore
    public private(set) var simulatorClock: SimulatorClock?
    public private(set) var messages: [SimulatorSMS] = []
    public private(set) var isWorking = false
    public private(set) var message: String?

    private let simulator: any SimulatorClient

    public init(environment: APIEnvironment, configStore: AppConfigStore, simulator: any SimulatorClient) {
        self.environment = environment
        self.configStore = configStore
        self.simulator = simulator
    }

    public func load() async {
        do {
            simulatorClock = try await simulator.clock()
            messages = try await simulator.messages(phone: nil, limit: 5)
        } catch {
            message = "Simülatöre ulaşılamadı: \(error.localizedDescription)"
        }
    }

    /// Zamanı ileri sarar ve konfigürasyonu yeniler; ekranlar yeni sunucu saatini hemen görür.
    public func advance(days: Int) async {
        isWorking = true
        defer { isWorking = false }
        do {
            let clock = try await simulator.advance(days: days)
            simulatorClock = clock
            await configStore.load()
            message = clock.backendNotified == true
                ? "Zaman \(days) gün ileri sarıldı."
                : "Zaman ileri sarıldı ama backend'e bildirilemedi. Backend açılınca saati kendisi çeker."
        } catch {
            message = error.localizedDescription
        }
    }

    public func reloadConfig() async {
        await configStore.load()
        message = configStore.errorMessage ?? "Konfigürasyon yenilendi."
    }
}
