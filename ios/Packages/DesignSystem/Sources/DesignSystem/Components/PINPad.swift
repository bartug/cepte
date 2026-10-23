//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import SwiftUI

/// 6 haneli PIN için noktalar ve rakam klavyesi. Sistem klavyesi yerine kendi klavyemiz: tuş yerleri sabit,
/// otomatik tamamlama ve klavye geçmişi yok.
///
/// `pin` bir Binding: değeri ekranın ViewModel'i tutar, bu bileşen sadece günceller.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 23.10.2026 - CP-2
public struct PINPad: View {
    @Binding private var pin: String
    private let length: Int
    private let isDisabled: Bool
    private let biometricSymbol: String?
    private let onBiometric: (() -> Void)?
    private let onComplete: (String) -> Void

    public init(pin: Binding<String>, length: Int = 6, isDisabled: Bool = false, biometricSymbol: String? = nil,
                onBiometric: (() -> Void)? = nil, onComplete: @escaping (String) -> Void) {
        _pin = pin
        self.length = length
        self.isDisabled = isDisabled
        self.biometricSymbol = biometricSymbol
        self.onBiometric = onBiometric
        self.onComplete = onComplete
    }

    public var body: some View {
        VStack(spacing: Spacing.xl) {
            HStack(spacing: Spacing.m) {
                ForEach(0..<length, id: \.self) { index in
                    Circle()
                        .fill(index < pin.count ? Palette.brand : Palette.divider)
                        .frame(width: 14, height: 14)
                }
            }
            .accessibilityElement()
            .accessibilityLabel("\(pin.count) / \(length) hane girildi")

            Grid(horizontalSpacing: Spacing.l, verticalSpacing: Spacing.m) {
                ForEach([["1", "2", "3"], ["4", "5", "6"], ["7", "8", "9"]], id: \.self) { row in
                    GridRow {
                        ForEach(row, id: \.self) { digit in
                            key(digit)
                        }
                    }
                }
                GridRow {
                    if let biometricSymbol, let onBiometric {
                        iconKey(biometricSymbol, label: "Biyometrik giriş", action: onBiometric)
                    } else {
                        Color.clear.frame(width: 72, height: 72)
                    }
                    key("0")
                    iconKey("delete.left", label: "Sil") {
                        if !pin.isEmpty {
                            pin.removeLast()
                        }
                    }
                }
            }
        }
        .disabled(isDisabled)
        .opacity(isDisabled ? 0.5 : 1)
        .sensoryFeedback(.selection, trigger: pin)
    }

    private func key(_ digit: String) -> some View {
        Button {
            guard pin.count < length else {
                return
            }
            pin.append(digit)
            if pin.count == length {
                onComplete(pin)
            }
        } label: {
            Text(digit)
                .font(.system(.title, design: .rounded, weight: .medium))
                .frame(width: 72, height: 72)
                .foregroundStyle(Palette.textPrimary)
                .background(Palette.surface, in: Circle())
        }
        .buttonStyle(.plain)
    }

    private func iconKey(_ symbol: String, label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.title2)
                .frame(width: 72, height: 72)
                .foregroundStyle(Palette.brand)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }
}

#Preview {
    @Previewable @State var pin = "12"
    PINPad(pin: $pin, biometricSymbol: "faceid", onBiometric: {}) { _ in }
        .padding()
        .background(Palette.background)
}
