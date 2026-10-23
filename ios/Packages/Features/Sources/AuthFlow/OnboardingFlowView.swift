//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import DesignSystem
import SwiftUI

/// Onboarding'in navigasyon yığını. Hangi ekrana geçileceğine ViewModel karar verir (`path`'e adım ekler);
/// ekranlar birbirini doğrudan açmaz.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 23.10.2026 - CP-2
public struct OnboardingFlowView: View {
    @State private var viewModel: OnboardingViewModel

    public init(viewModel: OnboardingViewModel) {
        _viewModel = State(initialValue: viewModel)
    }

    public var body: some View {
        NavigationStack(path: $viewModel.path) {
            PhoneEntryView(viewModel: viewModel)
                .navigationDestination(for: OnboardingViewModel.Step.self) { step in
                    switch step {
                    case .otp:
                        OTPEntryView(viewModel: viewModel)
                    case .createPIN:
                        PINCreateView(viewModel: viewModel)
                    case .enterPIN:
                        PINVerifyView(viewModel: viewModel)
                    case .biometric:
                        BiometricOptInView(viewModel: viewModel)
                    }
                }
        }
        .tint(Palette.brand)
    }
}

/// Başlık, açıklama ve hata mesajı; onboarding ekranlarının ortak üst kısmı.
struct StepHeader: View {
    let title: String
    let subtitle: String
    let error: String?

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text(title)
                .font(Typography.title)
                .foregroundStyle(Palette.textPrimary)
            Text(subtitle)
                .font(Typography.body)
                .foregroundStyle(Palette.textSecondary)
            if let error {
                Label(error, systemImage: "exclamationmark.circle.fill")
                    .font(Typography.caption)
                    .foregroundStyle(Palette.danger)
                    .padding(.top, Spacing.xxs)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
