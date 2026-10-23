//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import DesignSystem
import SwiftUI

/// Yeni kullanıcı: PIN iki kez girilir.
struct PINCreateView: View {
    let viewModel: OnboardingViewModel
    @State private var pin = ""
    @State private var firstPIN: String?
    @State private var localError: String?

    var body: some View {
        VStack(spacing: Spacing.xl) {
            StepHeader(title: firstPIN == nil ? "PIN oluştur" : "PIN'i tekrar gir",
                       subtitle: "Girişte ve para gönderirken bu 6 haneli PIN'i kullanacaksın.",
                       error: localError ?? viewModel.errorMessage)
            Spacer()
            PINPad(pin: $pin, isDisabled: viewModel.isLoading) { entered in
                Task { await handle(entered) }
            }
            Spacer()
        }
        .padding(Spacing.m)
        .background(Palette.background)
    }

    private func handle(_ entered: String) async {
        localError = nil
        guard let firstPIN else {
            if PINRules.isWeak(entered) {
                localError = "Tahmin edilmesi kolay bir PIN seçme (111111, 123456 gibi)."
            } else {
                self.firstPIN = entered
            }
            pin = ""
            return
        }
        guard entered == firstPIN else {
            localError = "PIN'ler eşleşmedi. Baştan dene."
            self.firstPIN = nil
            pin = ""
            return
        }
        if await !viewModel.createPIN(entered) {
            self.firstPIN = nil
            pin = ""
        }
    }
}

/// Kayıtlı kullanıcı yeni cihazda: mevcut PIN istenir.
struct PINVerifyView: View {
    let viewModel: OnboardingViewModel
    @State private var pin = ""

    var body: some View {
        VStack(spacing: Spacing.xl) {
            StepHeader(title: "PIN'ini gir",
                       subtitle: "Bu numarayla bir hesabın var. Bu cihazı bağlamak için PIN'ini gir; diğer cihazındaki oturum kapanacak.",
                       error: viewModel.errorMessage)
            Spacer()
            PINPad(pin: $pin, isDisabled: viewModel.isLoading) { entered in
                Task {
                    if await !viewModel.enterPIN(entered) {
                        pin = ""
                    }
                }
            }
            Spacer()
        }
        .padding(Spacing.m)
        .background(Palette.background)
    }
}

struct BiometricOptInView: View {
    let viewModel: OnboardingViewModel

    var body: some View {
        VStack(spacing: Spacing.l) {
            Spacer()
            Image(systemName: "faceid")
                .font(.system(size: 72))
                .foregroundStyle(Palette.brand)
            Text("\(viewModel.biometricName) ile giriş")
                .font(Typography.title)
            Text("Her seferinde PIN girmek yerine \(viewModel.biometricName) ile gir. PIN'in yine geçerli kalır.")
                .font(Typography.body)
                .foregroundStyle(Palette.textSecondary)
                .multilineTextAlignment(.center)
            Spacer()
            Button("\(viewModel.biometricName)'yi aç") {
                Task { await viewModel.finish(enableBiometrics: true) }
            }
            .buttonStyle(.primary)
            Button("Şimdi değil") {
                Task { await viewModel.finish(enableBiometrics: false) }
            }
            .buttonStyle(.secondary)
        }
        .padding(Spacing.m)
        .background(Palette.background)
        .navigationBarBackButtonHidden()
    }
}
