//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import Foundation

/// `/v1/auth/*` cevaplarının karşılıkları.
public struct AuthTokens: Codable, Sendable, Equatable {
    public let userId: Int64
    public let deviceId: Int64
    public let accessToken: String
    public let accessTokenExpiresAt: Date
    public let refreshToken: String
    public let refreshTokenExpiresAt: Date

    public init(userId: Int64, deviceId: Int64, accessToken: String, accessTokenExpiresAt: Date,
                refreshToken: String, refreshTokenExpiresAt: Date) {
        self.userId = userId
        self.deviceId = deviceId
        self.accessToken = accessToken
        self.accessTokenExpiresAt = accessTokenExpiresAt
        self.refreshToken = refreshToken
        self.refreshTokenExpiresAt = refreshTokenExpiresAt
    }
}

public struct OTPChallenge: Decodable, Sendable, Equatable {
    public let challengeId: UUID
    public let expiresInSeconds: Int
    public let resendAfterSeconds: Int

    public init(challengeId: UUID, expiresInSeconds: Int, resendAfterSeconds: Int) {
        self.challengeId = challengeId
        self.expiresInSeconds = expiresInSeconds
        self.resendAfterSeconds = resendAfterSeconds
    }
}

public struct OTPVerification: Decodable, Sendable, Equatable {
    public let verificationToken: String
    public let registered: Bool

    public init(verificationToken: String, registered: Bool) {
        self.verificationToken = verificationToken
        self.registered = registered
    }
}

public struct Me: Decodable, Sendable, Equatable {
    public let userId: Int64
    public let maskedPhone: String
    public let deviceId: Int64
    public let deviceName: String
    public let biometricEnabled: Bool
    public let registeredAt: Date
}

/// Uygulama yeniden açıldığında oturumu sürdürmek için Keychain'de tutulan bilgi. Access token burada yok;
/// sadece bellekte durur.
public struct StoredCredentials: Codable, Sendable, Equatable {
    public let userId: Int64
    public let deviceId: Int64
    public var refreshToken: String?
    public var biometricEnabled: Bool

    public init(userId: Int64, deviceId: Int64, refreshToken: String?, biometricEnabled: Bool) {
        self.userId = userId
        self.deviceId = deviceId
        self.refreshToken = refreshToken
        self.biometricEnabled = biometricEnabled
    }
}
