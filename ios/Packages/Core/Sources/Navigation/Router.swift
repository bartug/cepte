//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import Observation
import SwiftUI

/// Bir `NavigationStack`'in yolunu tutar. Her tab'ın kendi router'ı vardır.
///
/// Ekranlar birbirini doğrudan açmaz, `router.push(.transactionDetail(id))` der. Deeplink de aynı şekilde
/// router'a route ekleyerek çalışır.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
@MainActor
@Observable
public final class Router<Route: Hashable> {
    public var path: [Route] = []

    public init() {}

    public func push(_ route: Route) {
        path.append(route)
    }

    public func pop() {
        guard !path.isEmpty else {
            return
        }
        path.removeLast()
    }

    public func popToRoot() {
        path.removeAll()
    }
}
