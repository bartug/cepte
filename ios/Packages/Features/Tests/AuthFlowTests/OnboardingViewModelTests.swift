//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import DeviceSecurity
import Foundation
import LocalAuthentication
import Networking
import Synchronization
import Testing
@testable import AuthFlow
@testable import SessionKit

@MainActor
struct OnboardingViewModelTests {

    @Test(arguments: [("0532 123 45 67", "5321234567"), ("+90 532 123 45 67", "5321234567"), ("53212345678", "5321234567")])
    func telefonTemizlenir(input: String, expected: String) {
        #expect(PhoneNumber.digits(from: input) == expected)
    }

    @Test
    func telefonFormatlanir() {
        #expect(PhoneNumber.formatted("5321234567") == "532 123 45 67")
        #expect(!PhoneNumber.isValid("4321234567"))
    }

    @Test(arguments: [("111111", true), ("123456", true), ("654321", true), ("121212", true), ("123123", true), ("482915", false)])
    func zayifPinKuraliBackendIleAyni(pin: String, weak: Bool) {
        #expect(PINRules.isWeak(pin) == weak)
    }

    @Test
    func kayitsizNumaraPinOlusturmayaGider() async {
        let viewModel = makeViewModel(api: StubAuthAPI(registered: false))
        viewModel.phoneDigits = "5321234567"

        await viewModel.submitPhone()
        _ = await viewModel.submitCode("482915")

        #expect(viewModel.path == [.otp, .createPIN])
    }

    @Test
    func kayitliNumaraPinGirmeyeGider() async {
        let viewModel = makeViewModel(api: StubAuthAPI(registered: true))
        viewModel.phoneDigits = "5321234567"

        await viewModel.submitPhone()
        _ = await viewModel.submitCode("482915")

        #expect(viewModel.path == [.otp, .enterPIN])
    }

    @Test
    func hataliKoddaBackendMesajiGosterilir() async {
        let viewModel = makeViewModel(api: StubAuthAPI(registered: false, codeError: "Kod hatalı. Kalan deneme hakkı: 4"))
        viewModel.phoneDigits = "5321234567"
        await viewModel.submitPhone()

        let accepted = await viewModel.submitCode("000000")

        #expect(!accepted)
        #expect(viewModel.errorMessage == "Kod hatalı. Kalan deneme hakkı: 4")
        #expect(viewModel.path == [.otp])
    }

    @Test
    func zayifPinSunucuyaGitmedenReddedilir() async {
        let api = StubAuthAPI(registered: false)
        let viewModel = makeViewModel(api: api)
        viewModel.phoneDigits = "5321234567"
        await viewModel.submitPhone()
        _ = await viewModel.submitCode("482915")

        #expect(await !viewModel.createPIN("123456"))
        #expect(api.registerCount == 0)
    }

    private func makeViewModel(api: StubAuthAPI) -> OnboardingViewModel {
        let credentials = MemoryCredentials()
        let keyStore = MemoryKeyStore()
        let manager = SessionManager(api: api, credentials: credentials, keyStore: keyStore, clock: ServerClock())
        let sessionStore = SessionStore(manager: manager, api: api, credentials: credentials, keyStore: keyStore,
                                        biometrics: BiometricAuthenticator())
        return OnboardingViewModel(api: api, keyStore: keyStore, sessionStore: sessionStore, deviceName: "Test")
    }
}

private final class StubAuthAPI: AuthAPI {
    private let registered: Bool
    private let codeError: String?
    private let registers = Mutex(0)

    init(registered: Bool, codeError: String? = nil) {
        self.registered = registered
        self.codeError = codeError
    }

    var registerCount: Int { registers.withLock { $0 } }

    func requestOTP(phone: String) async throws -> OTPChallenge {
        OTPChallenge(challengeId: UUID(), expiresInSeconds: 180, resendAfterSeconds: 60)
    }

    func verifyOTP(challengeId: UUID, code: String) async throws -> OTPVerification {
        if let codeError {
            throw APIError.server(status: 401, message: codeError)
        }
        return OTPVerification(verificationToken: "ticket", registered: registered)
    }

    func register(verificationToken: String, pin: String, deviceKey: any SigningKey, deviceName: String) async throws -> AuthTokens {
        registers.withLock { $0 += 1 }
        return tokens()
    }

    func bindDevice(verificationToken: String, pin: String, deviceKey: any SigningKey, deviceName: String) async throws -> AuthTokens { tokens() }
    func login(pin: String, deviceId: Int64, deviceKey: any SigningKey) async throws -> AuthTokens { tokens() }
    func loginWithBiometric(deviceId: Int64, deviceKey: any SigningKey, biometricKey: any SigningKey) async throws -> AuthTokens { tokens() }
    func refresh(refreshToken: String, deviceId: Int64, deviceKey: any SigningKey) async throws -> AuthTokens { tokens() }
    func enableBiometric(publicKey: Data, accessToken: String, deviceId: Int64, deviceKey: any SigningKey) async throws {}
    func logout(accessToken: String) async throws {}

    private func tokens() -> AuthTokens {
        AuthTokens(userId: 1, deviceId: 7, accessToken: "a", accessTokenExpiresAt: Date().addingTimeInterval(900),
                   refreshToken: "r", refreshTokenExpiresAt: Date().addingTimeInterval(86_400))
    }
}

private final class MemoryCredentials: CredentialStore {
    private let value = Mutex<StoredCredentials?>(nil)
    func load() -> StoredCredentials? { value.withLock { $0 } }
    func save(_ credentials: StoredCredentials) { value.withLock { $0 = credentials } }
    func clear() { value.withLock { $0 = nil } }
}

private struct Key: SigningKey {
    var publicKeyDER: Data { Data([1]) }
    func sign(_ data: Data) throws -> Data { Data([2]) }
}

private struct MemoryKeyStore: DeviceKeyStore {
    func deviceKey() throws -> any SigningKey { Key() }
    func createBiometricKey() throws -> any SigningKey { Key() }
    func biometricKey(context: LAContext) throws -> (any SigningKey)? { Key() }
    func deleteAll() {}
}
