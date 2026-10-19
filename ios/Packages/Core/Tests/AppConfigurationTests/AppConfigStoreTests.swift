//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import Foundation
import Testing
@testable import AppConfiguration
@testable import Networking

@MainActor
struct AppConfigStoreTests {

    @Test
    func sunucuSaatiFarkiHesaplanir() async {
        let deviceNow = Date(timeIntervalSince1970: 1_792_400_400)
        let store = AppConfigStore(client: FakeClient(result: .success(config(serverTime: deviceNow.addingTimeInterval(31 * 86_400)))),
                                   now: { deviceNow })

        await store.load()

        #expect(store.serverNow() == deviceNow.addingTimeInterval(31 * 86_400))
        #expect(store.errorMessage == nil)
    }

    @Test
    func hataOlursaEskiKonfigurasyonKorunur() async {
        let client = FakeClient(result: .success(config(serverTime: Date())))
        let store = AppConfigStore(client: client)
        await store.load()

        client.result = .failure(APIError.transport(.notConnectedToInternet))
        await store.load()

        #expect(store.config != nil)
        #expect(store.errorMessage == "Sunucuya ulaşılamadı. Bağlantını kontrol et.")
    }

    @Test(arguments: [("1.0.0", "1.0.0", false), ("1.0.0", "1.0.1", true), ("1.10.0", "1.9.0", false), ("2.0", "1.9.9", false)])
    func guncellemeZorunluluguSurumeGore(appVersion: String, minimum: String, expected: Bool) async {
        let store = AppConfigStore(client: FakeClient(result: .success(config(serverTime: Date(), minimumVersion: minimum))))
        await store.load()

        #expect(store.requiresUpdate(appVersion: appVersion) == expected)
    }

    @Test
    func bilinmeyenFlagKapaliSayilir() {
        let config = AppConfig(serverTime: Date(), timeZone: "Europe/Istanbul", simulatedClock: true,
                               minimumAppVersion: "1.0.0", miniApps: [], features: ["CREDIT": true])

        #expect(config.isEnabled(.credit))
        #expect(!config.isEnabled(.qrPayment))
    }

    private func config(serverTime: Date, minimumVersion: String = "1.0.0") -> AppConfig {
        AppConfig(serverTime: serverTime, timeZone: "Europe/Istanbul", simulatedClock: true,
                  minimumAppVersion: minimumVersion, miniApps: [], features: [:])
    }
}

private final class FakeClient: HTTPClient, @unchecked Sendable {
    var result: Result<AppConfig, Error>

    init(result: Result<AppConfig, Error>) {
        self.result = result
    }

    func send<Response>(_ request: APIRequest<Response>) async throws -> Response {
        try result.get() as! Response
    }
}
