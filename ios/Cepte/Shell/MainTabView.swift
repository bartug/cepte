//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import DesignSystem
import Home
import MiniAppKit
import SwiftUI

enum AppTab: Hashable {
    case home
    case wallet
    case qr
    case finance
    case profile
}

/// Ana tab bar. Mini uygulamalar tab değil, ana sayfadan tam ekran açılır.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
struct MainTabView: View {
    let dependencies: AppDependencies

    @State private var selectedTab = AppTab.home
    @State private var presentedMiniApp: MiniAppEntry?

    var body: some View {
        TabView(selection: $selectedTab) {
            Tab("Ana Sayfa", systemImage: "house.fill", value: .home) {
                NavigationStack {
                    HomeView(viewModel: dependencies.homeViewModel) { presentedMiniApp = $0 }
                        .navigationTitle("Cepte")
                }
            }
            Tab("Cüzdan", systemImage: "wallet.bifold.fill", value: .wallet) {
                placeholder("Cüzdan", systemImage: "wallet.bifold", phase: "CP-4")
            }
            Tab("QR", systemImage: "qrcode.viewfinder", value: .qr) {
                placeholder("QR ile öde", systemImage: "qrcode", phase: "CP-5")
            }
            Tab("Finans", systemImage: "chart.pie.fill", value: .finance) {
                placeholder("Finans", systemImage: "chart.pie", phase: "CP-7")
            }
            Tab("Profil", systemImage: "person.crop.circle", value: .profile) {
                NavigationStack {
                    ProfileView(dependencies: dependencies)
                }
            }
        }
        .tint(Palette.brand)
        .fullScreenCover(item: $presentedMiniApp) { entry in
            if let app = dependencies.miniAppRegistry.app(for: entry.code) {
                MiniAppContainerView(app: app, kycLevel: dependencies.kycLevel)
            }
        }
        // Açık olan mini app backend'den kapatılırsa kullanıcı süper uygulamaya döner
        .onChange(of: dependencies.homeViewModel.miniApps) { _, entries in
            if let presented = presentedMiniApp, !entries.contains(where: { $0.code == presented.code }) {
                presentedMiniApp = nil
            }
        }
    }

    private func placeholder(_ title: String, systemImage: String, phase: String) -> some View {
        NavigationStack {
            ComingSoonView(title, systemImage: systemImage, phase: phase)
                .navigationTitle(title)
        }
    }
}
