//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import DesignSystem
import SwiftUI

struct PhoneEntryView: View {
    @Bindable var viewModel: OnboardingViewModel
    @FocusState private var isFocused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.l) {
            Text("Cepte")
                .font(.system(size: 40, weight: .heavy, design: .rounded))
                .foregroundStyle(Palette.brand)

            StepHeader(title: "Telefonunla devam et",
                       subtitle: "Numarana bir doğrulama kodu göndereceğiz.",
                       error: viewModel.errorMessage)

            HStack(spacing: Spacing.s) {
                Text("+90")
                    .font(Typography.headline)
                    .foregroundStyle(Palette.textSecondary)
                // Görünmeyen alan rakamları alır, üstünde formatlı hali gösterilir. Alanın metnini her tuşta
                // yeniden yazmak hızlı yazarken karakter kaybettiriyordu.
                ZStack(alignment: .leading) {
                    TextField("", text: $viewModel.phoneDigits)
                        .frame(maxWidth: .infinity, minHeight: 32)
                        .keyboardType(.numberPad)
                        .textContentType(.telephoneNumber)
                        .foregroundStyle(.clear)
                        .tint(.clear)
                        .focused($isFocused)
                        .accessibilityLabel("Telefon numarası")
                    Text(viewModel.phoneDigits.isEmpty ? "5XX XXX XX XX" : PhoneNumber.formatted(viewModel.phoneDigits))
                        .foregroundStyle(viewModel.phoneDigits.isEmpty ? Palette.textSecondary.opacity(0.5) : Palette.textPrimary)
                        .allowsHitTesting(false)
                        .accessibilityHidden(true)
                }
                .font(.system(.title3, design: .rounded).monospacedDigit())
            }
            .padding(Spacing.m)
            .background(Palette.surface, in: RoundedRectangle(cornerRadius: Radius.m))

            Spacer()

            Button {
                Task { await viewModel.submitPhone() }
            } label: {
                if viewModel.isLoading {
                    ProgressView().tint(.white)
                } else {
                    Text("Kod gönder")
                }
            }
            .buttonStyle(.primary)
            .disabled(!viewModel.canSubmitPhone)
        }
        .padding(Spacing.m)
        .background(Palette.background)
        .onAppear { isFocused = true }
    }
}
