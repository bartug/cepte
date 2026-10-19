//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import SwiftUI

/// Tutarı kuruş cinsinden alıp Türkçe formatta gösterir: `125050` → `₺1.250,50`.
///
/// Para hiçbir zaman `Double` ile taşınmaz; backend'deki `Money` gibi en küçük birim (`Int64`) kullanılır.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
public struct MoneyText: View {
    private let minorUnits: Int64
    private let currencyCode: String
    private let font: Font

    public init(minorUnits: Int64, currencyCode: String = "TRY", font: Font = Typography.amount) {
        self.minorUnits = minorUnits
        self.currencyCode = currencyCode
        self.font = font
    }

    public var body: some View {
        Text(MoneyFormatter.format(minorUnits: minorUnits, currencyCode: currencyCode))
            .font(font)
            .contentTransition(.numericText())
    }
}

public enum MoneyFormatter {
    private static let locale = Locale(identifier: "tr_TR")

    public static func format(minorUnits: Int64, currencyCode: String = "TRY") -> String {
        // Decimal ile bölme kayıpsızdır; 0.1 + 0.2 sorunu yaşanmaz
        let amount = Decimal(minorUnits) / 100
        return amount.formatted(.currency(code: currencyCode).locale(locale))
    }
}
