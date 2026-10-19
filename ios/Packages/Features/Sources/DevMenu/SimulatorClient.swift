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
}
