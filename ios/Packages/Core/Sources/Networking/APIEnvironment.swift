//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import Foundation

/// Uygulamanın konuştuğu adresler.
///
/// Simülatörde `localhost` Mac'in kendisidir; backend ve external-sim Mac'te çalıştığı için ek ayar gerekmez.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
public struct APIEnvironment: Sendable, Equatable {
    public let name: String
    public let backendURL: URL
    public let simulatorURL: URL

    public init(name: String, backendURL: URL, simulatorURL: URL) {
        self.name = name
        self.backendURL = backendURL
        self.simulatorURL = simulatorURL
    }

    public static let local = APIEnvironment(
        name: "Lokal",
        backendURL: URL(string: "http://localhost:8080")!,
        simulatorURL: URL(string: "http://localhost:8091")!
    )
}
