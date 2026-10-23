//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import CryptoKit
import Foundation
import Networking

/// İsteği cihaz anahtarıyla imzalar. Backend'deki `DeviceSignature` ile birebir aynı metin imzalanır:
///
/// ```
/// METHOD \n PATH \n TIMESTAMP \n NONCE \n hex(SHA-256(body))
/// ```
///
/// Zaman damgası cihaz saatinden değil sunucu saatinden alınır; simülasyonda saat ileri sarılmışsa imza yine geçerli olur.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 23.10.2026 - CP-2
public struct RequestSigner: Sendable {
    public static let deviceIdHeader = "X-Device-Id"
    public static let timestampHeader = "X-Timestamp"
    public static let nonceHeader = "X-Nonce"
    public static let signatureHeader = "X-Signature"
    public static let biometricSignatureHeader = "X-Biometric-Signature"

    private let clock: ServerClock
    private let nonce: @Sendable () -> String

    public init(clock: ServerClock, nonce: @escaping @Sendable () -> String = { UUID().uuidString }) {
        self.clock = clock
        self.nonce = nonce
    }

    public func sign<Response>(_ request: APIRequest<Response>, deviceId: Int64, deviceKey: any SigningKey,
                               biometricKey: (any SigningKey)? = nil) throws -> APIRequest<Response> {
        let timestamp = String(Int64(clock.now().timeIntervalSince1970))
        let nonce = nonce()
        let message = Data(Self.stringToSign(method: request.method.rawValue, path: request.path, timestamp: timestamp,
                                             nonce: nonce, body: request.body ?? Data()).utf8)
        var signed = request
        signed.headers[Self.deviceIdHeader] = String(deviceId)
        signed.headers[Self.timestampHeader] = timestamp
        signed.headers[Self.nonceHeader] = nonce
        signed.headers[Self.signatureHeader] = try deviceKey.sign(message).base64EncodedString()
        if let biometricKey {
            signed.headers[Self.biometricSignatureHeader] = try biometricKey.sign(message).base64EncodedString()
        }
        return signed
    }

    public static func stringToSign(method: String, path: String, timestamp: String, nonce: String, body: Data) -> String {
        let bodyHash = SHA256.hash(data: body).map { String(format: "%02x", $0) }.joined()
        return [method.uppercased(), path, timestamp, nonce, bodyHash].joined(separator: "\n")
    }
}
