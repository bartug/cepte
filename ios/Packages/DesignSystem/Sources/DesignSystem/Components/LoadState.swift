//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import SwiftUI

/// Veri yükleyen her ekranın durumu. Ekran bu dört durumdan birinde olmak zorunda; "yükleniyor ama hata da var"
/// gibi tutarsız bir durum tip seviyesinde imkânsız.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
public enum LoadState<Value> {
    case idle
    case loading
    case loaded(Value)
    case failed(String)

    public var value: Value? {
        if case .loaded(let value) = self {
            return value
        }
        return nil
    }
}

/// `LoadState`'e göre iskelet, içerik ya da hata gösterir.
public struct LoadStateView<Value, Content: View>: View {
    private let state: LoadState<Value>
    private let retry: () async -> Void
    private let content: (Value) -> Content

    public init(_ state: LoadState<Value>, retry: @escaping () async -> Void, @ViewBuilder content: @escaping (Value) -> Content) {
        self.state = state
        self.retry = retry
        self.content = content
    }

    public var body: some View {
        switch state {
        case .idle, .loading:
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        case .loaded(let value):
            content(value)
        case .failed(let message):
            ErrorStateView(message: message, retry: retry)
        }
    }
}

public struct ErrorStateView: View {
    private let message: String
    private let retry: () async -> Void

    public init(message: String, retry: @escaping () async -> Void) {
        self.message = message
        self.retry = retry
    }

    public var body: some View {
        ContentUnavailableView {
            Label("Bir sorun oluştu", systemImage: "wifi.exclamationmark")
        } description: {
            Text(message)
        } actions: {
            Button("Tekrar dene") {
                Task { await retry() }
            }
            .buttonStyle(.secondary)
            .frame(maxWidth: 220)
        }
    }
}
