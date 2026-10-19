//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import AppConfiguration
import Foundation

/// Ana sayfada gösterilecek bir mini app.
public struct MiniAppEntry: Identifiable, Equatable, Sendable {
    public let code: String
    public let title: String
    public let icon: String
    public let requiredKycLevel: Int
    /// KYC seviyesi yetmiyorsa ikon görünür ama açılmaz; kullanıcı önce kimliğini doğrular.
    public let isLocked: Bool

    public var id: String { code }
}

/// Uygulamanın içinde kodu bulunan mini app'ler ile backend'in listesini birleştirir.
///
/// - Kodu uygulamada olmayan mini app gösterilmez (yeni dikey önce mağaza sürümüyle gelir).
/// - Backend'de kapalı olan gösterilmez.
/// - Sıra ve başlık backend'den gelir.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
@MainActor
public final class MiniAppRegistry {
    private let apps: [String: any MiniApp]

    public init(apps: [any MiniApp]) {
        self.apps = Dictionary(uniqueKeysWithValues: apps.map { ($0.code, $0) })
    }

    public func entries(from remote: [RemoteMiniApp], kycLevel: Int) -> [MiniAppEntry] {
        remote
            .filter { $0.enabled && apps[$0.code] != nil }
            .sorted { $0.sortOrder < $1.sortOrder }
            .map { item in
                MiniAppEntry(code: item.code, title: item.title, icon: item.icon,
                             requiredKycLevel: item.requiredKycLevel, isLocked: kycLevel < item.requiredKycLevel)
            }
    }

    public func app(for code: String) -> (any MiniApp)? {
        apps[code]
    }
}
