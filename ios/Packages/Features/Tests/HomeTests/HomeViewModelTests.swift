//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import AppConfiguration
import Foundation
import MiniAppKit
import Networking
import SwiftUI
import Testing
@testable import Home

@MainActor
struct HomeViewModelTests {

    @Test(arguments: [("2026-10-19T06:30:00Z", "Günaydın"), ("2026-10-19T11:00:00Z", "İyi günler"),
                      ("2026-10-19T17:00:00Z", "İyi akşamlar"), ("2026-10-19T21:30:00Z", "İyi geceler")])
    func selamlamaIstanbulSaatineGore(serverTime: String, expected: String) async throws {
        let time = try Date.ISO8601FormatStyle().parse(serverTime)
        let store = AppConfigStore(client: ConfigClient(config: config(serverTime: time)), now: { time })
        await store.load()

        let viewModel = HomeViewModel(configStore: store, registry: MiniAppRegistry(apps: []))

        #expect(viewModel.greeting == expected)
    }

    @Test
    func miniAppListesiKonfigurasyondanGelir() async {
        let store = AppConfigStore(client: ConfigClient(config: config(serverTime: Date())))
        let viewModel = HomeViewModel(configStore: store, registry: MiniAppRegistry(apps: [Stub(code: "FOOD")]))
        #expect(viewModel.miniApps.isEmpty)

        await viewModel.refresh()

        #expect(viewModel.miniApps.map(\.code) == ["FOOD"])
    }

    private func config(serverTime: Date) -> AppConfig {
        AppConfig(serverTime: serverTime, timeZone: "Europe/Istanbul", simulatedClock: true, minimumAppVersion: "1.0.0",
                  miniApps: [RemoteMiniApp(code: "FOOD", title: "Yemek", icon: "fork.knife", requiredKycLevel: 0, enabled: true, sortOrder: 1)],
                  features: [:])
    }
}

private struct ConfigClient: HTTPClient {
    let config: AppConfig

    func send<Response>(_ request: APIRequest<Response>) async throws -> Response {
        config as! Response
    }
}

private struct Stub: MiniApp {
    let code: String

    func makeEntryView(context: MiniAppContext) -> AnyView {
        AnyView(EmptyView())
    }
}
