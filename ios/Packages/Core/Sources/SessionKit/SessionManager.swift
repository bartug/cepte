//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import DeviceSecurity
import Foundation
import Networking

public enum SessionError: Error, Equatable {
    /// Oturum yok ya da yenilenemedi; kullanıcı tekrar giriş yapmalı.
    case expired
}

/// Token'ları tutar ve gerektiğinde yeniler.
///
/// `actor`: içindeki değişkenlere aynı anda tek bir çağrı erişir (Java'da her metodu `synchronized` bir sınıf gibi,
/// ama thread bloklanmadan). Ekran açılırken 5 istek birden 401 alırsa 5 ayrı yenileme yapılmaz: ilk çağrı yenileme
/// görevini başlatır, diğerleri aynı görevin sonucunu bekler. 5 yenileme olsaydı ilki token'ı döndürür, kalan 4'ü
/// "kullanılmış token" sayılır ve backend oturumu güvenlik nedeniyle kapatırdı.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 23.10.2026 - CP-2
public actor SessionManager {
    private var tokens: AuthTokens?
    private var refreshTask: Task<AuthTokens, Error>?
    private var expiredHandler: (@Sendable () async -> Void)?

    private let api: any AuthAPI
    private let credentials: any CredentialStore
    private let keyStore: any DeviceKeyStore
    private let clock: ServerClock

    public init(api: any AuthAPI, credentials: any CredentialStore, keyStore: any DeviceKeyStore, clock: ServerClock) {
        self.api = api
        self.credentials = credentials
        self.keyStore = keyStore
        self.clock = clock
    }

    public func onExpired(_ handler: @escaping @Sendable () async -> Void) {
        expiredHandler = handler
    }

    /// Giriş ya da kayıt sonrası oturumu başlatır.
    public func start(_ tokens: AuthTokens) {
        self.tokens = tokens
        var stored = credentials.load() ?? StoredCredentials(userId: tokens.userId, deviceId: tokens.deviceId,
                                                            refreshToken: nil, biometricEnabled: false)
        if stored.deviceId != tokens.deviceId {
            stored = StoredCredentials(userId: tokens.userId, deviceId: tokens.deviceId, refreshToken: nil, biometricEnabled: false)
        }
        stored.refreshToken = tokens.refreshToken
        credentials.save(stored)
    }

    /// Uygulama kilitlenince bellekteki token'lar atılır; cihaz bağlı kalır, açmak için PIN / Face ID gerekir.
    public func suspend() {
        tokens = nil
        refreshTask?.cancel()
        refreshTask = nil
    }

    public func accessToken() async throws -> String {
        guard let tokens else {
            throw SessionError.expired
        }
        // Süresine 30 saniyeden az kaldıysa istek atmadan önce yenile
        if tokens.accessTokenExpiresAt.timeIntervalSince(clock.now()) < 30 {
            return try await refresh().accessToken
        }
        return tokens.accessToken
    }

    /// 401 alan istek çağırır. Arada başka bir istek token'ı yenilediyse tekrar yenilemez, yenisini döner.
    public func refreshAfterUnauthorized(failedToken: String) async throws -> String {
        if let tokens, tokens.accessToken != failedToken {
            return tokens.accessToken
        }
        return try await refresh().accessToken
    }

    public var currentTokens: AuthTokens? {
        tokens
    }

    private func refresh() async throws -> AuthTokens {
        if let refreshTask {
            return try await refreshTask.value
        }
        guard let refreshToken = tokens?.refreshToken, let deviceId = tokens?.deviceId else {
            throw SessionError.expired
        }
        let task = Task { [api, keyStore] in
            try await api.refresh(refreshToken: refreshToken, deviceId: deviceId, deviceKey: try keyStore.deviceKey())
        }
        refreshTask = task
        defer { refreshTask = nil }

        do {
            let refreshed = try await task.value
            start(refreshed)
            return refreshed
        } catch APIError.server(401, _) {
            tokens = nil
            await expiredHandler?()
            throw SessionError.expired
        }
    }
}
