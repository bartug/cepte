//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import MiniAppKit
import SwiftUI

/// Taksi dikeyi. Yolculukta önce blokaj, bitişte gerçek tutarın tahsili CP-6'da.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
public struct TaxiMiniApp: MiniApp {
    public let code = "TAXI"

    public init() {}

    public func makeEntryView(context: MiniAppContext) -> AnyView {
        AnyView(RideRequestView())
    }
}
