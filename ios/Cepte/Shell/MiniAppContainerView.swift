//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import MiniAppKit
import SwiftUI

/// Mini uygulamayı kendi navigasyon yığınıyla tam ekran açar ve kapatma butonunu ekler.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
struct MiniAppContainerView: View {
    let app: any MiniApp
    let kycLevel: Int

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            app.makeEntryView(context: MiniAppContext(kycLevel: kycLevel, close: { dismiss() }))
                .toolbar {
                    ToolbarItem(placement: .topBarLeading) {
                        Button {
                            dismiss()
                        } label: {
                            Image(systemName: "xmark")
                        }
                        .accessibilityLabel("Kapat")
                    }
                }
        }
    }
}
