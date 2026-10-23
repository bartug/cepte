//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import CryptoKit
import Foundation
import LocalAuthentication
import Security

/// Cihaz anahtarlarını üretir ve saklar.
///
/// İki anahtar var:
/// - **Cihaz anahtarı**: Her imzalı istekte kullanılır, Face ID sormaz (token yenileme arka planda sessizce olur).
/// - **Biyometrik anahtar**: Sadece Face ID ile açılır, sadece girişte kullanılır.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 23.10.2026 - CP-2
public protocol DeviceKeyStore: Sendable {
    /// Varsa mevcut cihaz anahtarını, yoksa yenisini döner.
    func deviceKey() throws -> any SigningKey
    /// Eskisini silip yeni biyometrik anahtar üretir.
    func createBiometricKey() throws -> any SigningKey
    /// Face ID'si doğrulanmış context ile biyometrik anahtarı açar.
    func biometricKey(context: LAContext) throws -> (any SigningKey)?
    func deleteAll()
}

public struct KeychainDeviceKeyStore: DeviceKeyStore {
    private static let deviceAccount = "device-key"
    private static let biometricAccount = "biometric-key"

    private let keychain: KeychainStore

    public init(keychain: KeychainStore = KeychainStore()) {
        self.keychain = keychain
    }

    /// Apple Silicon Mac'teki simülatörde de true döner; Secure Enclave'i olmayan eski cihazlarda false.
    public static var usesSecureEnclave: Bool {
        SecureEnclave.isAvailable
    }

    /// Parmak izi / yüz değişirse (biometryCurrentSet) anahtar kullanılamaz olur; sonradan eklenen bir yüz giriş yapamaz.
    private static var biometricFlags: SecAccessControlCreateFlags {
        #if targetEnvironment(simulator)
        // Simülatör Secure Enclave anahtarı üretiyor ama biyometrik erişim kuralını desteklemiyor
        // ("not supported on iOS Simulator"). Orada Face ID kontrolünü BiometricAuthenticator yapar.
        return [.privateKeyUsage]
        #else
        return [.privateKeyUsage, .biometryCurrentSet]
        #endif
    }

    public func deviceKey() throws -> any SigningKey {
        if let stored = try keychain.data(for: Self.deviceAccount) {
            return try restore(stored, context: nil)
        }
        let flags: SecAccessControlCreateFlags = Self.usesSecureEnclave ? [.privateKeyUsage] : []
        return try create(account: Self.deviceAccount, flags: flags)
    }

    public func createBiometricKey() throws -> any SigningKey {
        keychain.delete(Self.biometricAccount)
        return try create(account: Self.biometricAccount, flags: Self.usesSecureEnclave ? Self.biometricFlags : [])
    }

    public func biometricKey(context: LAContext) throws -> (any SigningKey)? {
        guard let stored = try keychain.data(for: Self.biometricAccount) else {
            return nil
        }
        return try restore(stored, context: context)
    }

    public func deleteAll() {
        keychain.delete(Self.deviceAccount)
        keychain.delete(Self.biometricAccount)
    }

    private func create(account: String, flags: SecAccessControlCreateFlags) throws -> any SigningKey {
        if Self.usesSecureEnclave {
            var error: Unmanaged<CFError>?
            guard let access = SecAccessControlCreateWithFlags(nil, kSecAttrAccessibleWhenUnlockedThisDeviceOnly, flags, &error) else {
                throw error!.takeRetainedValue() as Error
            }
            let key = try SecureEnclave.P256.Signing.PrivateKey(accessControl: access)
            try keychain.set(key.dataRepresentation, for: account)
            return SecureEnclaveSigningKey(key: key)
        }
        // Secure Enclave yok: anahtar yazılımsal üretilir, Keychain'de durur
        let key = P256.Signing.PrivateKey()
        try keychain.set(key.rawRepresentation, for: account)
        return SoftwareSigningKey(key: key)
    }

    private func restore(_ data: Data, context: LAContext?) throws -> any SigningKey {
        if Self.usesSecureEnclave {
            return SecureEnclaveSigningKey(key: try SecureEnclave.P256.Signing.PrivateKey(dataRepresentation: data,
                                                                                           authenticationContext: context))
        }
        return SoftwareSigningKey(key: try P256.Signing.PrivateKey(rawRepresentation: data))
    }
}
