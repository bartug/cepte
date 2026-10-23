//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import AppConfiguration
import Foundation
import Networking
import Testing
@testable import DevMenu

@MainActor
struct DevMenuViewModelTests {

    @Test
    func ileriSarincaKonfigurasyonYenilenir() async {
        let backend = CountingClient()
        let viewModel = DevMenuViewModel(environment: .local, configStore: AppConfigStore(client: backend),
                                         simulator: FakeSimulator(notified: true))

        await viewModel.advance(days: 31)

        #expect(backend.calls == 1)
        await viewModel.load()
        #expect(viewModel.messages.first?.code == "482915")
        #expect(viewModel.message == "Zaman 31 gün ileri sarıldı.")
    }

    @Test
    func backendBildirilemezseUyarilir() async {
        let viewModel = DevMenuViewModel(environment: .local, configStore: AppConfigStore(client: CountingClient()),
                                         simulator: FakeSimulator(notified: false))

        await viewModel.advance(days: 1)

        #expect(viewModel.message?.contains("bildirilemedi") == true)
    }
}

private struct FakeSimulator: SimulatorClient {
    let notified: Bool

    func clock() async throws -> SimulatorClock {
        SimulatorClock(now: Date())
    }

    func advance(days: Int) async throws -> SimulatorClock {
        SimulatorClock(now: Date().addingTimeInterval(Double(days) * 86_400), backendNotified: notified)
    }

    func messages(phone: String?, limit: Int) async throws -> [SimulatorSMS] {
        [SimulatorSMS(phone: "+905321234567", text: "Cepte doğrulama kodun: 482915.", code: "482915", sentAt: Date())]
    }
}

private final class CountingClient: HTTPClient, @unchecked Sendable {
    var calls = 0

    func send<Response>(_ request: APIRequest<Response>) async throws -> Response {
        calls += 1
        return AppConfig(serverTime: Date(), timeZone: "Europe/Istanbul", simulatedClock: true, minimumAppVersion: "1.0.0",
                         miniApps: [], features: [:]) as! Response
    }
}
