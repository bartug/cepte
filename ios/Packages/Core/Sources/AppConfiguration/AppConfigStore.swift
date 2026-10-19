//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import Foundation
import Networking
import Observation

/// Uygulamanın uzak konfigürasyonunu tutar ve yeniler.
///
/// `@Observable` olduğu için `config` değiştiğinde onu okuyan tüm ekranlar kendiliğinden yeniden çizilir.
/// `@MainActor`: değerleri ekranlar okuduğu için tüm erişim ana thread'de.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
@MainActor
@Observable
public final class AppConfigStore {
    public private(set) var config: AppConfig?
    public private(set) var isLoading = false
    public private(set) var errorMessage: String?

    /// Sunucu saati ile cihaz saati arasındaki fark. Simülasyonda zaman ileri sarıldıysa günlerce olabilir.
    public private(set) var serverTimeOffset: TimeInterval = 0

    private let client: any HTTPClient
    private let now: @Sendable () -> Date

    public init(client: any HTTPClient, now: @escaping @Sendable () -> Date = { Date() }) {
        self.client = client
        self.now = now
    }

    /// Konfigürasyonu backend'den okur. Hata olursa eldeki konfigürasyon korunur.
    public func load() async {
        guard !isLoading else {
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            let loaded = try await client.send(APIRequest<AppConfig>.get("/v1/app/config"))
            config = loaded
            serverTimeOffset = loaded.serverTime.timeIntervalSince(now())
            errorMessage = nil
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    /// Tarih gösterirken cihaz saati yerine bu kullanılır; vadeler sunucu saatine göre hesaplanır.
    public func serverNow() -> Date {
        now().addingTimeInterval(serverTimeOffset)
    }

    /// Uygulamanın sürümü backend'in istediği minimum sürümün altında mı.
    public func requiresUpdate(appVersion: String) -> Bool {
        guard let config,
              let current = SemanticVersion(appVersion),
              let minimum = SemanticVersion(config.minimumAppVersion) else {
            return false
        }
        return current < minimum
    }
}
