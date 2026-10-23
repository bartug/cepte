//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import AuthFlow
import DesignSystem
import DevMenu
import SwiftUI

/// Konfigürasyon ve oturum durumuna göre açılış, onboarding, kilit ya da ana ekran.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
struct RootView: View {
    /// Bu süreden uzun arka planda kalan uygulama kilitlenir.
    private static let lockAfter: TimeInterval = 5 * 60

    let dependencies: AppDependencies

    @Environment(\.scenePhase) private var scenePhase
    @State private var isDevMenuPresented = false
    @State private var backgroundedAt: Date?

    var body: some View {
        content
            .task {
                await dependencies.sessionStore.restore()
                await dependencies.configStore.load()
            }
            .onChange(of: scenePhase) { _, phase in
                handle(phase)
            }
            // Uygulama değiştiricide bakiye ve kişisel bilgiler görünmesin
            .overlay {
                if scenePhase != .active {
                    PrivacyCover()
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
        let session = dependencies.sessionStore
        if store.config == nil || !session.isRestored {
            if let error = store.errorMessage {
                ErrorStateView(message: error) { await store.load() }
            } else {
                SplashView()
            }
        } else if store.requiresUpdate(appVersion: AppInfo.version) {
            ForceUpdateView()
        } else {
            switch session.state {
            case .signedOut:
                OnboardingFlowView(viewModel: dependencies.makeOnboardingViewModel())
            case .locked:
                LockView(sessionStore: session)
            case .active:
                MainTabView(dependencies: dependencies)
            }
        }
    }

    private func handle(_ phase: ScenePhase) {
        switch phase {
        case .background:
            backgroundedAt = Date()
        case .active:
            // Arka plandan dönünce flag'ler ve sunucu saati tazelenir
            Task { await dependencies.configStore.load() }
            if let backgroundedAt, Date().timeIntervalSince(backgroundedAt) > Self.lockAfter {
                Task { await dependencies.sessionStore.lock() }
            }
            backgroundedAt = nil
        default:
            break
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

private struct PrivacyCover: View {
    var body: some View {
        Text("Cepte")
            .font(.system(size: 44, weight: .heavy, design: .rounded))
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Palette.brand)
            .ignoresSafeArea()
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
