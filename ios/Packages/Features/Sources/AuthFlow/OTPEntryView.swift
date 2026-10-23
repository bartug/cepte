//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import DesignSystem
import SwiftUI

struct OTPEntryView: View {
    @Bindable var viewModel: OnboardingViewModel
    @State private var code = ""
    @FocusState private var isFocused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.l) {
            StepHeader(title: "Kodu gir",
                       subtitle: "\(viewModel.displayPhone) numarasına gönderdiğimiz 6 haneli kodu gir.",
                       error: viewModel.errorMessage)

            OTPCodeField(code: $code)
                .focused($isFocused)
                .onChange(of: code) { _, newValue in
                    if newValue.count == 6 {
                        submit(newValue)
                    }
                }

            resendRow

            #if DEBUG
            if viewModel.canPeekCode {
                Button("Simülatörden kodu al") {
                    Task {
                        if let peeked = await viewModel.peekCode() {
                            code = peeked
                        }
                    }
                }
                .font(Typography.caption)
            }
            #endif

            Spacer()
        }
        .padding(Spacing.m)
        .background(Palette.background)
        .disabled(viewModel.isLoading)
        .onAppear { isFocused = true }
    }

    // Saniyede bir yeniden çizilir; geri sayım için Timer tutmaya gerek yok
    private var resendRow: some View {
        TimelineView(.periodic(from: .now, by: 1)) { context in
            let remaining = Int((viewModel.resendAvailableAt ?? .distantPast).timeIntervalSince(context.date).rounded(.up))
            if remaining > 0 {
                Text("Yeni kod için \(remaining) sn")
                    .font(Typography.caption)
                    .foregroundStyle(Palette.textSecondary)
            } else {
                Button("Kodu tekrar gönder") {
                    Task { await viewModel.resendCode() }
                }
                .font(Typography.caption)
            }
        }
    }

    private func submit(_ value: String) {
        Task {
            if await !viewModel.submitCode(value) {
                code = ""
            }
        }
    }
}

/// 6 kutulu kod alanı. Görünmeyen bir TextField girişi alır; `.oneTimeCode` sayesinde gerçek cihazda SMS'teki kod
/// klavyenin üstünde önerilir.
struct OTPCodeField: View {
    @Binding var code: String

    var body: some View {
        ZStack {
            HStack(spacing: Spacing.xs) {
                ForEach(0..<6, id: \.self) { index in
                    let characters = Array(code)
                    Text(index < characters.count ? String(characters[index]) : "")
                        .font(.system(.title2, design: .rounded, weight: .semibold).monospacedDigit())
                        .frame(maxWidth: .infinity, minHeight: 56)
                        .background(Palette.surface, in: RoundedRectangle(cornerRadius: Radius.s))
                        .overlay(RoundedRectangle(cornerRadius: Radius.s)
                            .stroke(index == characters.count ? Palette.brand : Palette.divider, lineWidth: 1.5))
                }
            }
            TextField("", text: Binding(get: { code }, set: { code = String($0.filter(\.isNumber).prefix(6)) }))
                .frame(maxWidth: .infinity, minHeight: 56)
                .keyboardType(.numberPad)
                .textContentType(.oneTimeCode)
                .foregroundStyle(.clear)
                .tint(.clear)
                .accessibilityLabel("Doğrulama kodu")
        }
    }
}
