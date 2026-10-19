//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import Foundation

/// `GET /v1/app/config` cevabı.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
public struct AppConfig: Decodable, Sendable, Equatable {
    public let serverTime: Date
    public let timeZone: String
    public let simulatedClock: Bool
    public let minimumAppVersion: String
    public let miniApps: [RemoteMiniApp]
    public let features: [String: Bool]

    public init(serverTime: Date, timeZone: String, simulatedClock: Bool, minimumAppVersion: String,
                miniApps: [RemoteMiniApp], features: [String: Bool]) {
        self.serverTime = serverTime
        self.timeZone = timeZone
        self.simulatedClock = simulatedClock
        self.minimumAppVersion = minimumAppVersion
        self.miniApps = miniApps
        self.features = features
    }

    /// Backend'in bilmediği ya da göndermediği flag kapalı sayılır.
    public func isEnabled(_ feature: Feature) -> Bool {
        features[feature.rawValue] ?? false
    }
}

/// Backend'in bildiği mini uygulama. Kodu iOS'ta olmayan bir mini app gelirse gösterilmez.
public struct RemoteMiniApp: Decodable, Sendable, Equatable {
    public let code: String
    public let title: String
    public let icon: String
    public let requiredKycLevel: Int
    public let enabled: Bool
    public let sortOrder: Int

    public init(code: String, title: String, icon: String, requiredKycLevel: Int, enabled: Bool, sortOrder: Int) {
        self.code = code
        self.title = title
        self.icon = icon
        self.requiredKycLevel = requiredKycLevel
        self.enabled = enabled
        self.sortOrder = sortOrder
    }
}

public enum Feature: String, CaseIterable, Sendable {
    case qrPayment = "QR_PAYMENT"
    case credit = "CREDIT"
    case certificatePinning = "CERTIFICATE_PINNING"
}
