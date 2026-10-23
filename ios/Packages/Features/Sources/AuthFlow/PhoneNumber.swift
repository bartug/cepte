//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import Foundation

/// Türkiye cep telefonu: kullanıcı 10 hane girer (5XX XXX XX XX), backend'e E.164 (+905XXXXXXXXX) gider.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 23.10.2026 - CP-2
public enum PhoneNumber {

    public static func digits(from input: String) -> String {
        var digits = input.filter(\.isNumber)
        // "0532..." ya da "90532..." yapıştırılırsa baştaki ülke / şehir kodu atılır
        if digits.hasPrefix("90"), digits.count > 10 {
            digits.removeFirst(2)
        }
        if digits.hasPrefix("0") {
            digits.removeFirst()
        }
        return String(digits.prefix(10))
    }

    public static func isValid(_ digits: String) -> Bool {
        digits.count == 10 && digits.hasPrefix("5")
    }

    public static func e164(_ digits: String) -> String {
        "+90" + digits
    }

    /// 5321234567 → 532 123 45 67
    public static func formatted(_ digits: String) -> String {
        var result = ""
        for (index, character) in digits.enumerated() {
            if [3, 6, 8].contains(index) {
                result.append(" ")
            }
            result.append(character)
        }
        return result
    }
}

/// Backend'deki zayıf PIN kuralının aynısı; kullanıcı sunucuya gitmeden uyarılır.
public enum PINRules {

    public static func isWeak(_ pin: String) -> Bool {
        let digits = pin.compactMap(\.wholeNumberValue)
        guard digits.count == 6 else {
            return true
        }
        let repeating = Set(digits).count == 1 || pin == String(pin.prefix(2)).repeated(3) || pin == String(pin.prefix(3)).repeated(2)
        let step = digits[1] - digits[0]
        let sequential = abs(step) == 1 && zip(digits, digits.dropFirst()).allSatisfy { $1 - $0 == step }
        return repeating || sequential
    }
}

private extension String {
    func repeated(_ count: Int) -> String {
        String(repeating: self, count: count)
    }
}
