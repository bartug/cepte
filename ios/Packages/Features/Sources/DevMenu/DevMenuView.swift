//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import AppConfiguration
import DesignSystem
import SwiftUI

/// Sadece Debug build'de açılır. Cihazı sallayınca (simülatörde Device > Shake, ⌃⌘Z) ya da Profil'den.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
public struct DevMenuView: View {
    @State private var viewModel: DevMenuViewModel
    @Environment(\.dismiss) private var dismiss

    public init(viewModel: DevMenuViewModel) {
        _viewModel = State(initialValue: viewModel)
    }

    public var body: some View {
        NavigationStack {
            Form {
                Section("Ortam") {
                    LabeledContent("Ortam", value: viewModel.environment.name)
                    LabeledContent("Backend", value: viewModel.environment.backendURL.absoluteString)
                    LabeledContent("Simülatör", value: viewModel.environment.simulatorURL.absoluteString)
                }

                Section {
                    LabeledContent("Sunucu saati", value: viewModel.configStore.serverNow().formatted(date: .abbreviated, time: .shortened))
                    if let clock = viewModel.simulatorClock {
                        LabeledContent("Simülatör saati", value: clock.now.formatted(date: .abbreviated, time: .shortened))
                    }
                    LabeledContent("Simüle saat", value: viewModel.configStore.config?.simulatedClock == true ? "Evet" : "Hayır")
                    HStack {
                        advanceButton("+1 gün", days: 1)
                        advanceButton("+7 gün", days: 7)
                        advanceButton("+31 gün", days: 31)
                    }
                } header: {
                    Text("Zaman makinesi")
                } footer: {
                    Text("Vade, gecikme ve tahsilat senaryoları için. Saat geri sarılamaz.")
                }

                if let config = viewModel.configStore.config {
                    Section("Mini uygulamalar") {
                        ForEach(config.miniApps, id: \.code) { app in
                            LabeledContent(app.title, value: app.enabled ? "Açık" : "Kapalı")
                        }
                    }
                    Section("Feature flag'ler") {
                        ForEach(config.features.keys.sorted(), id: \.self) { key in
                            LabeledContent(key, value: config.features[key] == true ? "Açık" : "Kapalı")
                        }
                    }
                }

                Section {
                    Button("Konfigürasyonu yenile") {
                        Task { await viewModel.reloadConfig() }
                    }
                }

                if let message = viewModel.message {
                    Section {
                        Text(message)
                            .font(Typography.caption)
                    }
                }
            }
            .disabled(viewModel.isWorking)
            .navigationTitle("Geliştirici menüsü")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Kapat") { dismiss() }
                }
            }
            .task { await viewModel.load() }
        }
    }

    private func advanceButton(_ title: String, days: Int) -> some View {
        Button(title) {
            Task { await viewModel.advance(days: days) }
        }
        .buttonStyle(.bordered)
        .frame(maxWidth: .infinity)
    }
}
