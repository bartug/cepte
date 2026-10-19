//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import SwiftUI

/// Henüz yazılmamış bir bölümün yer tutucusu; hangi fazda geleceğini söyler.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
public struct ComingSoonView: View {
    private let title: String
    private let systemImage: String
    private let phase: String

    public init(_ title: String, systemImage: String, phase: String) {
        self.title = title
        self.systemImage = systemImage
        self.phase = phase
    }

    public var body: some View {
        ContentUnavailableView {
            Label(title, systemImage: systemImage)
        } description: {
            Text("Bu bölüm \(phase) fazında geliştirilecek.")
        }
        .background(Palette.background)
    }
}

#Preview {
    ComingSoonView("Cüzdan", systemImage: "wallet.bifold", phase: "CP-4")
}
