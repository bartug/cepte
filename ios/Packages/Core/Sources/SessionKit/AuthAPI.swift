//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import DeviceSecurity
import Foundation
import Networking

/// Backend'in `/v1/auth` uçları. İmza gereken çağrılar (giriş, yenileme) burada imzalanır.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 23.10.2026 - CP-2
public protocol AuthAPI: Sendable {
    func requestOTP(phone: String) async throws -> OTPChallenge
    func verifyOTP(challengeId: UUID, code: String) async throws -> OTPVerification
    func register(verificationToken: String, pin: String, deviceKey: any SigningKey, deviceName: String) async throws -> AuthTokens
    func bindDevice(verificationToken: String, pin: String, deviceKey: any SigningKey, deviceName: String) async throws -> AuthTokens
    func login(pin: String, deviceId: Int64, deviceKey: any SigningKey) async throws -> AuthTokens
    func loginWithBiometric(deviceId: Int64, deviceKey: any SigningKey, biometricKey: any SigningKey) async throws -> AuthTokens
    func refresh(refreshToken: String, deviceId: Int64, deviceKey: any SigningKey) async throws -> AuthTokens
    func enableBiometric(publicKey: Data, accessToken: String, deviceId: Int64, deviceKey: any SigningKey) async throws
    func logout(accessToken: String) async throws
}

public struct LiveAuthAPI: AuthAPI {
    private let http: any HTTPClient
    private let signer: RequestSigner

    public init(http: any HTTPClient, signer: RequestSigner) {
        self.http = http
        self.signer = signer
    }

    public func requestOTP(phone: String) async throws -> OTPChallenge {
        try await http.send(.post("/v1/auth/otp", body: ["phone": phone]))
    }

    public func verifyOTP(challengeId: UUID, code: String) async throws -> OTPVerification {
        try await http.send(.post("/v1/auth/otp/verify", body: ["challengeId": challengeId.uuidString, "code": code]))
    }

    public func register(verificationToken: String, pin: String, deviceKey: any SigningKey, deviceName: String) async throws -> AuthTokens {
        try await http.send(.post("/v1/auth/register", body: deviceBody(verificationToken, pin, deviceKey, deviceName)))
    }

    public func bindDevice(verificationToken: String, pin: String, deviceKey: any SigningKey, deviceName: String) async throws -> AuthTokens {
        try await http.send(.post("/v1/auth/devices", body: deviceBody(verificationToken, pin, deviceKey, deviceName)))
    }

    public func login(pin: String, deviceId: Int64, deviceKey: any SigningKey) async throws -> AuthTokens {
        let request = try APIRequest<AuthTokens>.post("/v1/auth/login", body: ["pin": pin])
        return try await http.send(signer.sign(request, deviceId: deviceId, deviceKey: deviceKey))
    }

    public func loginWithBiometric(deviceId: Int64, deviceKey: any SigningKey, biometricKey: any SigningKey) async throws -> AuthTokens {
        let request = APIRequest<AuthTokens>(method: .post, path: "/v1/auth/login/biometric")
        return try await http.send(signer.sign(request, deviceId: deviceId, deviceKey: deviceKey, biometricKey: biometricKey))
    }

    public func refresh(refreshToken: String, deviceId: Int64, deviceKey: any SigningKey) async throws -> AuthTokens {
        let request = try APIRequest<AuthTokens>.post("/v1/auth/refresh", body: ["refreshToken": refreshToken])
        return try await http.send(signer.sign(request, deviceId: deviceId, deviceKey: deviceKey))
    }

    public func enableBiometric(publicKey: Data, accessToken: String, deviceId: Int64, deviceKey: any SigningKey) async throws {
        var request = try APIRequest<EmptyResponse>.put("/v1/auth/biometric", body: ["publicKey": publicKey.base64EncodedString()])
        request.headers["Authorization"] = "Bearer \(accessToken)"
        _ = try await http.send(signer.sign(request, deviceId: deviceId, deviceKey: deviceKey))
    }

    public func logout(accessToken: String) async throws {
        var request = APIRequest<EmptyResponse>(method: .post, path: "/v1/auth/logout")
        request.headers["Authorization"] = "Bearer \(accessToken)"
        _ = try await http.send(request)
    }

    private func deviceBody(_ token: String, _ pin: String, _ key: any SigningKey, _ name: String) -> [String: String] {
        ["verificationToken": token, "pin": pin, "devicePublicKey": key.publicKeyDER.base64EncodedString(), "deviceName": name]
    }
}
