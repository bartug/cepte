//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import DesignSystem
import SessionKit
import SwiftUI

/// Kilit ekranı. Uygulama açılışında ve 5 dakikadan uzun arka planda kaldıktan sonra görünür.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 23.10.2026 - CP-2
public struct LockView: View {
    private let sessionStore: SessionStore
    @State private var pin = ""
    @State private var errorMessage: String?
    @State private var isWorking = false
    @State private var didPromptBiometrics = false
    @State private var isSignOutConfirmationPresented = false

    public init(sessionStore: SessionStore) {
        self.sessionStore = sessionStore
    }

    public var body: some View {
        VStack(spacing: Spacing.xl) {
            VStack(spacing: Spacing.xs) {
                Text("Tekrar hoş geldin")
                    .font(Typography.title)
                Text("Devam etmek için PIN'ini gir")
                    .font(Typography.body)
                    .foregroundStyle(Palette.textSecondary)
                if let errorMessage {
                    Text(errorMessage)
                        .font(Typography.caption)
                        .foregroundStyle(Palette.danger)
                        .multilineTextAlignment(.center)
                }
            }
            .padding(.top, Spacing.xl)

            Spacer()

            PINPad(pin: $pin, isDisabled: isWorking,
                   biometricSymbol: sessionStore.biometricEnabled ? "faceid" : nil,
                   onBiometric: sessionStore.biometricEnabled ? { Task { await unlockWithBiometrics() } } : nil) { entered in
                Task { await unlock(entered) }
            }

            Button("Hesaptan çık") {
                isSignOutConfirmationPresented = true
            }
            .font(Typography.caption)
            .foregroundStyle(Palette.textSecondary)
        }
        .padding(Spacing.m)
        .frame(maxWidth: .infinity)
        .background(Palette.background)
        .task {
            // Face ID açıksa ekran gelir gelmez bir kez sorulur; vazgeçilirse PIN ile devam edilir
            guard sessionStore.biometricEnabled, !didPromptBiometrics else {
                return
            }
            didPromptBiometrics = true
            await unlockWithBiometrics()
        }
        .confirmationDialog("Bu cihazdan çıkılsın mı?", isPresented: $isSignOutConfirmationPresented, titleVisibility: .visible) {
            Button("Hesaptan çık", role: .destructive) {
                Task { await sessionStore.signOut() }
            }
        } message: {
            Text("Tekrar girmek için telefonuna gelen kod ve PIN'in gerekecek.")
        }
    }

    private func unlock(_ entered: String) async {
        isWorking = true
        defer { isWorking = false }
        do {
            try await sessionStore.unlock(pin: entered)
        } catch {
            errorMessage = error.localizedDescription
            pin = ""
        }
    }

    private func unlockWithBiometrics() async {
        do {
            try await sessionStore.unlockWithBiometrics()
        } catch {
            // Kullanıcı vazgeçtiyse mesaj gösterilmez
            if (error as NSError).domain != "com.apple.LocalAuthentication" {
                errorMessage = error.localizedDescription
            }
        }
    }
}
