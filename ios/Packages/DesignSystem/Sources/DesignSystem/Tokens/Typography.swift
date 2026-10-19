//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import SwiftUI

/// Yazı stilleri. Hepsi Dynamic Type ile büyür; kullanıcı sistemden yazı boyutunu artırırsa uygulama da büyür.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
public enum Typography {
    public static let amountLarge = Font.system(.largeTitle, design: .rounded, weight: .bold).monospacedDigit()
    public static let amount = Font.system(.title3, design: .rounded, weight: .semibold).monospacedDigit()
    public static let title = Font.system(.title2, weight: .bold)
    public static let headline = Font.system(.headline)
    public static let body = Font.system(.body)
    public static let caption = Font.system(.caption)
}
