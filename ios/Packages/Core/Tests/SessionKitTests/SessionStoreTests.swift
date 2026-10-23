//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import DeviceSecurity
import Foundation
import Networking
import Testing
@testable import SessionKit

@MainActor
struct SessionStoreTests {
    private let now = Date(timeIntervalSince1970: 1_792_400_400)

    @Test
    func bagliCihazYoksaOnboardingBaslar() async {
        let store = makeStore(credentials: InMemoryCredentialStore())

        await store.restore()

        #expect(store.state == .signedOut)
    }

    @Test
    func bagliCihazVarsaKilitEkraniAcilir() async {
        let store = makeStore(credentials: InMemoryCredentialStore(StoredCredentials(userId: 1, deviceId: 7, refreshToken: "r", biometricEnabled: true)))

        await store.restore()

        #expect(store.state == .locked)
        #expect(store.biometricEnabled)
    }

    @Test
    func pinIleKilitAcilir() async throws {
        let store = makeStore(credentials: InMemoryCredentialStore(StoredCredentials(userId: 1, deviceId: 7, refreshToken: "r", biometricEnabled: false)))
        await store.restore()

        try await store.unlock(pin: "482915")

        #expect(store.state == .active)
    }

    @Test
    func cikistaKimlikBilgileriSilinir() async throws {
        let credentials = InMemoryCredentialStore(StoredCredentials(userId: 1, deviceId: 7, refreshToken: "r", biometricEnabled: false))
        let store = makeStore(credentials: credentials)
        await store.restore()
        try await store.unlock(pin: "482915")

        await store.signOut()

        #expect(store.state == .signedOut)
        #expect(credentials.load() == nil)
    }

    private func makeStore(credentials: InMemoryCredentialStore) -> SessionStore {
        let api = FakeAuthAPI(now: now)
        let manager = SessionManager(api: api, credentials: credentials, keyStore: InMemoryKeyStore(), clock: ServerClock())
        return SessionStore(manager: manager, api: api, credentials: credentials, keyStore: InMemoryKeyStore(),
                            biometrics: BiometricAuthenticator())
    }
}
