//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import CryptoKit
import Foundation

/// İstek imzalayan P-256 anahtar. Backend'deki `DeviceSignature` ile aynı algoritma: ECDSA + SHA-256, DER imza.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 23.10.2026 - CP-2
public protocol SigningKey: Sendable {
    /// Backend'e kaydedilen public key: DER SubjectPublicKeyInfo. Java'da `X509EncodedKeySpec` ile okunur.
    var publicKeyDER: Data { get }

    func sign(_ data: Data) throws -> Data
}

/// Özel anahtarı Secure Enclave çipinde duran anahtar. Uygulama anahtarın kendisini hiç görmez; elinde sadece
/// çipin şifreli tuttuğu bir referans (`dataRepresentation`) vardır, o da sadece bu cihazın çipinde çalışır.
struct SecureEnclaveSigningKey: SigningKey, @unchecked Sendable {
    let key: SecureEnclave.P256.Signing.PrivateKey

    var publicKeyDER: Data { key.publicKey.derRepresentation }

    func sign(_ data: Data) throws -> Data {
        try key.signature(for: data).derRepresentation
    }
}

/// Secure Enclave'i olmayan cihazlar için yazılımsal anahtar; Keychain'de saklanır.
struct SoftwareSigningKey: SigningKey, @unchecked Sendable {
    let key: P256.Signing.PrivateKey

    var publicKeyDER: Data { key.publicKey.derRepresentation }

    func sign(_ data: Data) throws -> Data {
        try key.signature(for: data).derRepresentation
    }
}
