//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import DesignSystem
import DevMenu
import Networking
import SessionKit
import SwiftUI

/// Profil: hesap ve cihaz bilgisi, Face ID, çıkış. Kişisel bilgiler ve izinler CP-3'te.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
struct ProfileView: View {
    let dependencies: AppDependencies

    @State private var me: Me?
    @State private var message: String?
    @State private var isDevMenuPresented = false
    @State private var isSignOutConfirmationPresented = false

    private var session: SessionStore { dependencies.sessionStore }

    var body: some View {
        List {
            Section("Hesap") {
                LabeledContent("Telefon", value: me?.maskedPhone ?? "…")
                LabeledContent("Kişisel bilgiler", value: "CP-3")
                LabeledContent("İzinler", value: "CP-3")
            }
            Section {
                LabeledContent("Bu cihaz", value: me?.deviceName ?? "…")
                if session.biometricEnabled {
                    LabeledContent("\(session.biometrics.displayName) ile giriş", value: "Açık")
                } else if session.biometrics.isAvailable {
                    Button("\(session.biometrics.displayName) ile girişi aç") {
                        Task { await enableBiometrics() }
                    }
                }
            } header: {
                Text("Cihaz ve güvenlik")
            } footer: {
                Text("Hesabın aynı anda tek bir cihazda açık olabilir. Başka bir cihazdan girersen bu cihazın oturumu kapanır.")
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
            Section {
                Button("Hesaptan çık", role: .destructive) { isSignOutConfirmationPresented = true }
            }
            if let message {
                Section { Text(message).font(Typography.caption) }
            }
        }
        .navigationTitle("Profil")
        .task { await loadMe() }
        .refreshable { await loadMe() }
        .sheet(isPresented: $isDevMenuPresented) {
            DevMenuView(viewModel: dependencies.makeDevMenuViewModel())
        }
        .confirmationDialog("Bu cihazdan çıkılsın mı?", isPresented: $isSignOutConfirmationPresented, titleVisibility: .visible) {
            Button("Hesaptan çık", role: .destructive) {
                Task { await session.signOut() }
            }
        } message: {
            Text("Tekrar girmek için telefonuna gelen kod ve PIN'in gerekecek.")
        }
    }

    private func loadMe() async {
        do {
            me = try await dependencies.authenticatedClient.send(APIRequest<Me>.get("/v1/me"))
        } catch {
            message = error.localizedDescription
        }
    }

    private func enableBiometrics() async {
        do {
            try await session.enableBiometrics()
            message = "\(session.biometrics.displayName) ile giriş açıldı."
        } catch {
            message = error.localizedDescription
        }
    }
}
