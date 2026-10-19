//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import SwiftUI
import UIKit

/// Uygulamanın renkleri. Her renk açık ve koyu mod için ayrı tanımlıdır; ekranlar hex değer kullanmaz.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
public enum Palette {
    public static let brand = dynamic(light: 0x5B4CF0, dark: 0x8A7FFF)
    public static let brandSoft = dynamic(light: 0xEEECFE, dark: 0x262147)
    public static let accent = dynamic(light: 0x12B39A, dark: 0x3DD9BF)

    public static let background = dynamic(light: 0xF6F6FA, dark: 0x0E0E12)
    public static let surface = dynamic(light: 0xFFFFFF, dark: 0x1B1B22)
    public static let textPrimary = dynamic(light: 0x16161D, dark: 0xF4F4F8)
    public static let textSecondary = dynamic(light: 0x6B6B7B, dark: 0xA0A0B2)
    public static let divider = dynamic(light: 0xE6E6EE, dark: 0x2C2C36)

    public static let success = dynamic(light: 0x16A34A, dark: 0x4ADE80)
    public static let warning = dynamic(light: 0xD97706, dark: 0xFBBF24)
    public static let danger = dynamic(light: 0xDC2626, dark: 0xF87171)

    private static func dynamic(light: UInt32, dark: UInt32) -> Color {
        Color(uiColor: UIColor { traits in
            UIColor(hex: traits.userInterfaceStyle == .dark ? dark : light)
        })
    }
}

private extension UIColor {
    convenience init(hex: UInt32) {
        self.init(red: CGFloat((hex >> 16) & 0xFF) / 255,
                  green: CGFloat((hex >> 8) & 0xFF) / 255,
                  blue: CGFloat(hex & 0xFF) / 255,
                  alpha: 1)
    }
}
