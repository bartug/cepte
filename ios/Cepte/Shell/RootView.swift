//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import DesignSystem
import DevMenu
import SwiftUI

/// Konfigürasyon gelene kadar açılış ekranı, gelince ana ekran. Sürüm eskiyse güncelleme ekranı.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
struct RootView: View {
    let dependencies: AppDependencies

    @Environment(\.scenePhase) private var scenePhase
    @State private var isDevMenuPresented = false

    var body: some View {
        content
            .task { await dependencies.configStore.load() }
            // Uygulama arka plandan dönünce flag'ler ve sunucu saati tazelenir
            .onChange(of: scenePhase) { _, phase in
                if phase == .active {
                    Task { await dependencies.configStore.load() }
                }
            }
        #if DEBUG
            .onDeviceShake { isDevMenuPresented = true }
            .sheet(isPresented: $isDevMenuPresented) {
                DevMenuView(viewModel: dependencies.makeDevMenuViewModel())
            }
        #endif
    }

    @ViewBuilder
    private var content: some View {
        let store = dependencies.configStore
        if store.config == nil {
            if let error = store.errorMessage {
                ErrorStateView(message: error) { await store.load() }
            } else {
                SplashView()
            }
        } else if store.requiresUpdate(appVersion: AppInfo.version) {
            ForceUpdateView()
        } else {
            MainTabView(dependencies: dependencies)
        }
    }
}

private struct SplashView: View {
    var body: some View {
        VStack(spacing: Spacing.m) {
            Text("Cepte")
                .font(.system(size: 44, weight: .heavy, design: .rounded))
                .foregroundStyle(Palette.brand)
            ProgressView()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Palette.background)
    }
}

private struct ForceUpdateView: View {
    var body: some View {
        ContentUnavailableView("Güncelleme gerekli",
                               systemImage: "arrow.down.app",
                               description: Text("Bu sürüm artık desteklenmiyor. Devam etmek için uygulamayı güncelle."))
    }
}

enum AppInfo {
    static var version: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "0.0.0"
    }
}
