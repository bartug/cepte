//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import DesignSystem
import DevMenu
import SwiftUI

/// Profil. Kişisel bilgiler, cihazlar ve güvenlik ayarları sonraki fazlarda gelecek.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
struct ProfileView: View {
    let dependencies: AppDependencies

    @State private var isDevMenuPresented = false

    var body: some View {
        List {
            Section("Hesap") {
                LabeledContent("Kişisel bilgiler", value: "CP-3")
                LabeledContent("Cihazlar ve güvenlik", value: "CP-2")
                LabeledContent("İzinler", value: "CP-3")
            }
            Section("Uygulama") {
                LabeledContent("Sürüm", value: AppInfo.version)
            }
            #if DEBUG
            Section {
                Button("Geliştirici menüsü") { isDevMenuPresented = true }
            } footer: {
                Text("Cihazı sallayarak da açılır (simülatörde ⌃⌘Z).")
            }
            #endif
        }
        .navigationTitle("Profil")
        .sheet(isPresented: $isDevMenuPresented) {
            DevMenuView(viewModel: dependencies.makeDevMenuViewModel())
        }
    }
}
