//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import DeviceSecurity
import Foundation

/// Oturum bilgisinin kalıcı deposu.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 23.10.2026 - CP-2
public protocol CredentialStore: Sendable {
    func load() -> StoredCredentials?
    func save(_ credentials: StoredCredentials)
    func clear()
}

public struct KeychainCredentialStore: CredentialStore {
    private static let account = "session"
    private let keychain: KeychainStore

    public init(keychain: KeychainStore = KeychainStore()) {
        self.keychain = keychain
    }

    public func load() -> StoredCredentials? {
        guard let data = try? keychain.data(for: Self.account) else {
            return nil
        }
        return try? JSONDecoder().decode(StoredCredentials.self, from: data)
    }

    public func save(_ credentials: StoredCredentials) {
        guard let data = try? JSONEncoder().encode(credentials) else {
            return
        }
        try? keychain.set(data, for: Self.account)
    }

    public func clear() {
        keychain.delete(Self.account)
    }
}
