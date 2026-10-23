//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import Foundation
import Networking

/// external-sim'in zaman makinesi.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
public protocol SimulatorClient: Sendable {
    func clock() async throws -> SimulatorClock
    func advance(days: Int) async throws -> SimulatorClock
    /// SMS ağ geçidinin gelen kutusu, en yeni başta.
    func messages(phone: String?, limit: Int) async throws -> [SimulatorSMS]
}

public struct SimulatorSMS: Decodable, Sendable, Equatable, Identifiable {
    public let phone: String
    public let text: String
    public let code: String?
    public let sentAt: Date

    public var id: String { phone + sentAt.description }

    public init(phone: String, text: String, code: String?, sentAt: Date) {
        self.phone = phone
        self.text = text
        self.code = code
        self.sentAt = sentAt
    }
}

public struct SimulatorClock: Decodable, Sendable, Equatable {
    public let now: Date
    /// Sadece ileri sarma cevabında gelir.
    public let backendNotified: Bool?

    public init(now: Date, backendNotified: Bool? = nil) {
        self.now = now
        self.backendNotified = backendNotified
    }
}

public struct LiveSimulatorClient: SimulatorClient {
    private let http: any HTTPClient

    public init(http: any HTTPClient) {
        self.http = http
    }

    public func clock() async throws -> SimulatorClock {
        try await http.send(.get("/sim/clock"))
    }

    public func advance(days: Int) async throws -> SimulatorClock {
        try await http.send(.post("/sim/clock/advance", body: ["days": days]))
    }

    public func messages(phone: String?, limit: Int) async throws -> [SimulatorSMS] {
        var path = "/sim/sms?limit=\(limit)"
        if let phone, let encoded = phone.addingPercentEncoding(withAllowedCharacters: .alphanumerics) {
            path += "&phone=\(encoded)"
        }
        return try await http.send(.get(path))
    }
}
