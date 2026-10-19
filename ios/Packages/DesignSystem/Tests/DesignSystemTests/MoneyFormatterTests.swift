//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import Testing
@testable import DesignSystem

struct MoneyFormatterTests {

    @Test(arguments: [(Int64(125_050), "1.250,50"), (Int64(5), "0,05"), (Int64(0), "0,00"), (Int64(100_000_000), "1.000.000,00")])
    func kurusTurkceFormatlanir(minorUnits: Int64, expected: String) {
        let formatted = MoneyFormatter.format(minorUnits: minorUnits)

        #expect(formatted.contains(expected))
        #expect(formatted.contains("₺"))
    }
}
