//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import Foundation

/// Ağ katmanının fırlatabileceği hatalar. Ekranlar sadece `localizedDescription` gösterir.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
public enum APIError: Error, Equatable, LocalizedError {
    /// İstek sunucuya ulaşmadı ya da cevap gelmedi (bağlantı yok, timeout).
    case transport(URLError.Code)
    /// Sunucu cevap verdi ama işlem başarısız. Mesaj backend'den gelir.
    case server(status: Int, message: String?)
    /// Cevap beklenen formatta değil.
    case decoding(String)
    case emptyBody

    public var errorDescription: String? {
        switch self {
        case .transport(.timedOut):
            "Sunucu zamanında cevap vermedi. Tekrar dene."
        case .transport:
            "Sunucuya ulaşılamadı. Bağlantını kontrol et."
        case .server(let status, let message):
            message ?? "Bir hata oluştu (\(status))."
        case .decoding, .emptyBody:
            "Sunucudan beklenmeyen bir cevap geldi."
        }
    }
}
