//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import MiniAppKit
import SwiftUI

/// Yemek dikeyi. Sipariş ve ödeme CP-6'da; şimdilik katalog.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
public struct FoodMiniApp: MiniApp {
    public let code = "FOOD"

    public init() {}

    public func makeEntryView(context: MiniAppContext) -> AnyView {
        AnyView(RestaurantListView())
    }
}
