//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import Foundation

/// `1.2.10` gibi sürüm numaraları. String karşılaştırması `1.10 < 1.9` der; bu tip parça parça karşılaştırır.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
public struct SemanticVersion: Comparable, Sendable, CustomStringConvertible {
    public let parts: [Int]

    public init?(_ value: String) {
        let parts = value.split(separator: ".").map { Int($0) }
        guard !parts.isEmpty, parts.allSatisfy({ $0 != nil }) else {
            return nil
        }
        self.parts = parts.compactMap { $0 }
    }

    public static func < (lhs: SemanticVersion, rhs: SemanticVersion) -> Bool {
        let count = max(lhs.parts.count, rhs.parts.count)
        for index in 0..<count {
            let left = index < lhs.parts.count ? lhs.parts[index] : 0
            let right = index < rhs.parts.count ? rhs.parts[index] : 0
            if left != right {
                return left < right
            }
        }
        return false
    }

    public static func == (lhs: SemanticVersion, rhs: SemanticVersion) -> Bool {
        !(lhs < rhs) && !(rhs < lhs)
    }

    public var description: String {
        parts.map(String.init).joined(separator: ".")
    }
}
