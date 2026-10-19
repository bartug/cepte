//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import AppConfiguration
import SwiftUI
import Testing
@testable import MiniAppKit

@MainActor
struct MiniAppRegistryTests {

    private let registry = MiniAppRegistry(apps: [StubMiniApp(code: "FOOD"), StubMiniApp(code: "TAXI")])

    @Test
    func backendSirasiKullanilir() {
        let entries = registry.entries(from: [remote("TAXI", order: 1), remote("FOOD", order: 2)], kycLevel: 0)

        #expect(entries.map(\.code) == ["TAXI", "FOOD"])
    }

    @Test
    func koduUygulamadaOlmayanGosterilmez() {
        let entries = registry.entries(from: [remote("FOOD", order: 1), remote("MARKET", order: 2)], kycLevel: 0)

        #expect(entries.map(\.code) == ["FOOD"])
    }

    @Test
    func kapaliOlanGosterilmez() {
        let entries = registry.entries(from: [remote("FOOD", order: 1, enabled: false), remote("TAXI", order: 2)], kycLevel: 0)

        #expect(entries.map(\.code) == ["TAXI"])
    }

    @Test
    func kycSeviyesiYetmeyenKilitliGorunur() {
        let entries = registry.entries(from: [remote("FOOD", order: 1, level: 2)], kycLevel: 1)

        #expect(entries.first?.isLocked == true)
    }

    private func remote(_ code: String, order: Int, enabled: Bool = true, level: Int = 0) -> RemoteMiniApp {
        RemoteMiniApp(code: code, title: code, icon: "star", requiredKycLevel: level, enabled: enabled, sortOrder: order)
    }
}

private struct StubMiniApp: MiniApp {
    let code: String

    func makeEntryView(context: MiniAppContext) -> AnyView {
        AnyView(Text(code))
    }
}
