//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import Foundation

/// Backend'deki `ResponseMessage`'ın karşılığı. Tüm cevaplar bu zarfın içinde gelir.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
public struct ResponseEnvelope<Object: Decodable>: Decodable {
    public let success: Bool
    public let message: String?
    public let object: Object?
    public let httpStatusCode: Int?
}
