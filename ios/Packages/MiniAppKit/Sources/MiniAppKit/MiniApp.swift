//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import SwiftUI

/// Süper uygulamanın içinde çalışan bir dikey (yemek, taksi).
///
/// Mini app cüzdana, oturuma ya da başka bir mini app'e doğrudan erişemez. İhtiyacı olan her şey
/// `MiniAppContext` ile verilir; böylece yemek ekibinin bir hatası cüzdan bakiyesine dokunamaz.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
@MainActor
public protocol MiniApp {
    /// Backend'deki `mini_app.code` ile aynı.
    var code: String { get }

    func makeEntryView(context: MiniAppContext) -> AnyView
}

/// Shell'in mini app'e verdiği yetenekler. Yeni bir yetenek (ödeme başlatma gibi) buraya eklenir.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
public struct MiniAppContext {
    /// Kullanıcının KYC seviyesi. Mini app kendi içinde seviye isteyen bir aksiyonu buna göre kilitler.
    public let kycLevel: Int
    /// Mini app'i kapatıp süper uygulamaya döner.
    public let close: @MainActor () -> Void

    public init(kycLevel: Int, close: @escaping @MainActor () -> Void) {
        self.kycLevel = kycLevel
        self.close = close
    }
}
