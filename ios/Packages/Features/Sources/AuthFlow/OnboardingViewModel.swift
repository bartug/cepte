//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import DeviceSecurity
import Foundation
import Observation
import SessionKit

/// Telefon → OTP → PIN → Face ID akışı.
///
/// Numara kayıtlı değilse PIN oluşturulur (kayıt), kayıtlıysa mevcut PIN istenir (yeni cihaz bağlama).
/// Kullanıcıya hangisinin olduğu OTP doğrulanana kadar söylenmez.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 23.10.2026 - CP-2
@MainActor
@Observable
public final class OnboardingViewModel {
    public enum Step: Hashable {
        case otp
        case createPIN
        case enterPIN
        case biometric
    }

    public var path: [Step] = []
    public var phoneDigits = "" {
        didSet {
            let clean = PhoneNumber.digits(from: phoneDigits)
            if clean != phoneDigits {
                phoneDigits = clean
            }
        }
    }
    public var errorMessage: String?
    public private(set) var isLoading = false
    public private(set) var resendAvailableAt: Date?

    private var challenge: OTPChallenge?
    private var verification: OTPVerification?
    private var pendingTokens: AuthTokens?

    private let api: any AuthAPI
    private let keyStore: any DeviceKeyStore
    private let sessionStore: SessionStore
    private let deviceName: String
    private let codePeek: (@Sendable (String) async -> String?)?

    /// - Parameter codePeek: Sadece Debug build'de verilir; simülatörün SMS kutusundan kodu okur.
    public init(api: any AuthAPI, keyStore: any DeviceKeyStore, sessionStore: SessionStore, deviceName: String,
                codePeek: (@Sendable (String) async -> String?)? = nil) {
        self.api = api
        self.keyStore = keyStore
        self.sessionStore = sessionStore
        self.deviceName = deviceName
        self.codePeek = codePeek
    }

    public var canSubmitPhone: Bool {
        PhoneNumber.isValid(phoneDigits) && !isLoading
    }

    public var displayPhone: String {
        "+90 " + PhoneNumber.formatted(phoneDigits)
    }

    public var canPeekCode: Bool {
        codePeek != nil
    }

    public var biometricName: String {
        sessionStore.biometrics.displayName
    }

    public func submitPhone() async {
        guard PhoneNumber.isValid(phoneDigits) else {
            errorMessage = "Numara 5 ile başlamalı ve 10 haneli olmalı."
            return
        }
        if await sendCode() {
            path.append(.otp)
        }
    }

    public func resendCode() async {
        _ = await sendCode()
    }

    /// - Returns: Kod hatalıysa `false`; ekran kutuları temizler.
    public func submitCode(_ code: String) async -> Bool {
        guard let challenge else {
            return false
        }
        return await perform {
            let verification = try await api.verifyOTP(challengeId: challenge.challengeId, code: code)
            self.verification = verification
            path.append(verification.registered ? .enterPIN : .createPIN)
        }
    }

    public func createPIN(_ pin: String) async -> Bool {
        guard !PINRules.isWeak(pin) else {
            errorMessage = "Tahmin edilmesi kolay bir PIN seçme (111111, 123456 gibi)."
            return false
        }
        guard let verification else {
            return false
        }
        return await perform {
            let tokens = try await api.register(verificationToken: verification.verificationToken, pin: pin,
                                                deviceKey: try keyStore.deviceKey(), deviceName: deviceName)
            await didAuthenticate(tokens)
        }
    }

    public func enterPIN(_ pin: String) async -> Bool {
        guard let verification else {
            return false
        }
        return await perform {
            // Bu cihaz için yeni bir anahtar: önceki bir kurulumdan kalan anahtar bu hesaba bağlı olmayabilir
            keyStore.deleteAll()
            let tokens = try await api.bindDevice(verificationToken: verification.verificationToken, pin: pin,
                                                  deviceKey: try keyStore.deviceKey(), deviceName: deviceName)
            await didAuthenticate(tokens)
        }
    }

    public func finish(enableBiometrics: Bool) async {
        guard let pendingTokens else {
            return
        }
        do {
            try await sessionStore.activate(pendingTokens, enableBiometrics: enableBiometrics)
        } catch {
            // Face ID açılamadı (iptal, yüz tanınmadı); oturum yine açılır, Face ID profilden sonra açılabilir
            try? await sessionStore.activate(pendingTokens, enableBiometrics: false)
        }
    }

    public func peekCode() async -> String? {
        guard let codePeek else {
            return nil
        }
        return await codePeek(PhoneNumber.e164(phoneDigits))
    }

    private func didAuthenticate(_ tokens: AuthTokens) async {
        pendingTokens = tokens
        if sessionStore.biometrics.isAvailable {
            path.append(.biometric)
        } else {
            await finish(enableBiometrics: false)
        }
    }

    private func sendCode() async -> Bool {
        await perform {
            let challenge = try await api.requestOTP(phone: PhoneNumber.e164(phoneDigits))
            self.challenge = challenge
            resendAvailableAt = Date().addingTimeInterval(TimeInterval(challenge.resendAfterSeconds))
        }
    }

    private func perform(_ work: () async throws -> Void) async -> Bool {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        do {
            try await work()
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }
}
