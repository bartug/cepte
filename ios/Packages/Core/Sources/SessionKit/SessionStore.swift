//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import DeviceSecurity
import Foundation
import Observation

public enum SessionState: Equatable, Sendable {
    /// Bu cihaz bağlı değil: telefon + OTP ile başlanır.
    case signedOut
    /// Cihaz bağlı ama oturum açık değil: PIN ya da Face ID gerekir.
    case locked
    case active
}

/// Uygulamanın oturum durumu. Shell bu duruma göre onboarding, kilit ekranı ya da ana ekranı gösterir.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 23.10.2026 - CP-2
@MainActor
@Observable
public final class SessionStore {
    public private(set) var state: SessionState = .signedOut
    public private(set) var biometricEnabled = false
    /// Keychain okunana kadar false; shell bu sürede açılış ekranında bekler, onboarding bir an görünüp kaybolmaz.
    public private(set) var isRestored = false

    public let biometrics: BiometricAuthenticator
    private let manager: SessionManager
    private let api: any AuthAPI
    private let credentials: any CredentialStore
    private let keyStore: any DeviceKeyStore

    public init(manager: SessionManager, api: any AuthAPI, credentials: any CredentialStore,
                keyStore: any DeviceKeyStore, biometrics: BiometricAuthenticator) {
        self.manager = manager
        self.api = api
        self.credentials = credentials
        self.keyStore = keyStore
        self.biometrics = biometrics
    }

    /// Açılışta çağrılır. Cihaz daha önce bağlandıysa kilit ekranı, bağlanmadıysa onboarding.
    public func restore() async {
        await manager.onExpired { [weak self] in
            await self?.sessionExpired()
        }
        let stored = credentials.load()
        biometricEnabled = stored?.biometricEnabled ?? false
        state = stored == nil ? .signedOut : .locked
        isRestored = true
    }

    /// Kayıt ya da cihaz bağlama tamamlandı. İstenirse Face ID ile giriş de açılır.
    public func activate(_ tokens: AuthTokens, enableBiometrics: Bool) async throws {
        await manager.start(tokens)
        if enableBiometrics {
            try await self.enableBiometrics()
        }
        state = .active
    }

    public func unlock(pin: String) async throws {
        guard let stored = credentials.load() else {
            state = .signedOut
            return
        }
        let tokens = try await api.login(pin: pin, deviceId: stored.deviceId, deviceKey: try keyStore.deviceKey())
        await manager.start(tokens)
        state = .active
    }

    public func unlockWithBiometrics() async throws {
        guard let stored = credentials.load(), stored.biometricEnabled else {
            return
        }
        let context = try await biometrics.authenticate(reason: "Cepte'ye giriş yap")
        guard let biometricKey = try keyStore.biometricKey(context: context) else {
            disableBiometricsLocally()
            return
        }
        let tokens = try await api.loginWithBiometric(deviceId: stored.deviceId, deviceKey: try keyStore.deviceKey(),
                                                      biometricKey: biometricKey)
        await manager.start(tokens)
        state = .active
    }

    /// Face ID ile yeni bir biyometrik anahtar üretir ve backend'e kaydeder.
    public func enableBiometrics() async throws {
        _ = try await biometrics.authenticate(reason: "Face ID ile girişi aç")
        guard let tokens = await manager.currentTokens else {
            throw SessionError.expired
        }
        let biometricKey = try keyStore.createBiometricKey()
        try await api.enableBiometric(publicKey: biometricKey.publicKeyDER, accessToken: tokens.accessToken,
                                      deviceId: tokens.deviceId, deviceKey: try keyStore.deviceKey())
        if var stored = credentials.load() {
            stored.biometricEnabled = true
            credentials.save(stored)
        }
        biometricEnabled = true
    }

    /// Uygulama uzun süre arka planda kaldı; tekrar açmak için PIN / Face ID gerekir.
    public func lock() async {
        guard state == .active else {
            return
        }
        await manager.suspend()
        state = .locked
    }

    /// Bu cihazdan tamamen çıkış. Cihaz anahtarı silinir; tekrar girmek için OTP + PIN gerekir.
    public func signOut() async {
        if let tokens = await manager.currentTokens {
            try? await api.logout(accessToken: tokens.accessToken)
        }
        await manager.suspend()
        credentials.clear()
        keyStore.deleteAll()
        biometricEnabled = false
        state = .signedOut
    }

    func sessionExpired() {
        state = credentials.load() == nil ? .signedOut : .locked
    }

    private func disableBiometricsLocally() {
        if var stored = credentials.load() {
            stored.biometricEnabled = false
            credentials.save(stored)
        }
        biometricEnabled = false
    }
}
