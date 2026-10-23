//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import CryptoKit
import DeviceSecurity
import Foundation
import LocalAuthentication
import Networking
import Synchronization
@testable import SessionKit

final class InMemoryCredentialStore: CredentialStore {
    private let value = Mutex<StoredCredentials?>(nil)

    init(_ initial: StoredCredentials? = nil) {
        value.withLock { $0 = initial }
    }

    func load() -> StoredCredentials? { value.withLock { $0 } }
    func save(_ credentials: StoredCredentials) { value.withLock { $0 = credentials } }
    func clear() { value.withLock { $0 = nil } }
}

struct TestKey: SigningKey {
    var publicKeyDER: Data { Data([1]) }
    func sign(_ data: Data) throws -> Data { Data([2]) }
}

struct InMemoryKeyStore: DeviceKeyStore {
    func deviceKey() throws -> any SigningKey { TestKey() }
    func createBiometricKey() throws -> any SigningKey { TestKey() }
    func biometricKey(context: LAContext) throws -> (any SigningKey)? { TestKey() }
    func deleteAll() {}
}

/// Yenileme çağrılarını sayar; yenilemeyi bilerek yavaşlatır ki eşzamanlı çağrılar üst üste binsin.
final class FakeAuthAPI: AuthAPI {
    private let refreshCalls = Mutex(0)
    private let refreshFails: Bool
    private let now: Date

    init(now: Date, refreshFails: Bool = false) {
        self.now = now
        self.refreshFails = refreshFails
    }

    var refreshCount: Int { refreshCalls.withLock { $0 } }

    func refresh(refreshToken: String, deviceId: Int64, deviceKey: any SigningKey) async throws -> AuthTokens {
        let call = refreshCalls.withLock { value -> Int in
            value += 1
            return value
        }
        try await Task.sleep(for: .milliseconds(100))
        if refreshFails {
            throw APIError.server(status: 401, message: "Oturum güvenlik nedeniyle sonlandırıldı.")
        }
        return tokens(access: "access-\(call)", expiresIn: 900)
    }

    func tokens(access: String, expiresIn: TimeInterval) -> AuthTokens {
        AuthTokens(userId: 1, deviceId: 7, accessToken: access, accessTokenExpiresAt: now.addingTimeInterval(expiresIn),
                   refreshToken: "refresh-\(access)", refreshTokenExpiresAt: now.addingTimeInterval(30 * 86_400))
    }

    func requestOTP(phone: String) async throws -> OTPChallenge { fatalError() }
    func verifyOTP(challengeId: UUID, code: String) async throws -> OTPVerification { fatalError() }
    func register(verificationToken: String, pin: String, deviceKey: any SigningKey, deviceName: String) async throws -> AuthTokens { fatalError() }
    func bindDevice(verificationToken: String, pin: String, deviceKey: any SigningKey, deviceName: String) async throws -> AuthTokens { fatalError() }
    func login(pin: String, deviceId: Int64, deviceKey: any SigningKey) async throws -> AuthTokens {
        tokens(access: "login", expiresIn: 900)
    }
    func loginWithBiometric(deviceId: Int64, deviceKey: any SigningKey, biometricKey: any SigningKey) async throws -> AuthTokens { fatalError() }
    func enableBiometric(publicKey: Data, accessToken: String, deviceId: Int64, deviceKey: any SigningKey) async throws {}
    func logout(accessToken: String) async throws {}
}
