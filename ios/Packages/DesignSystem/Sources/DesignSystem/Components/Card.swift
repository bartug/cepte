//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import SwiftUI

/// İçeriği yüzey rengi, köşe yuvarlaklığı ve iç boşlukla sarar.
///
/// Kullanım: `VStack { ... }.card()`
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
public struct CardModifier: ViewModifier {
    public func body(content: Content) -> some View {
        content
            .padding(Spacing.m)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Palette.surface, in: RoundedRectangle(cornerRadius: Radius.l))
    }
}

public extension View {
    func card() -> some View {
        modifier(CardModifier())
    }
}
