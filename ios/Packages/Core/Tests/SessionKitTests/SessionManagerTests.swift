//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import Foundation
import Networking
import Synchronization
import Testing
@testable import SessionKit

struct SessionManagerTests {
    private let now = Date(timeIntervalSince1970: 1_792_400_400)

    @Test
    func eszamanli401lerTekYenilemeYapar() async throws {
        let api = FakeAuthAPI(now: now)
        let manager = makeManager(api: api)
        await manager.start(api.tokens(access: "old", expiresIn: 900))

        // Ekran açılırken 5 istek birden 401 aldı
        let tokens = try await withThrowingTaskGroup(of: String.self) { group in
            for _ in 0..<5 {
                group.addTask { try await manager.refreshAfterUnauthorized(failedToken: "old") }
            }
            return try await group.reduce(into: [String]()) { $0.append($1) }
        }

        #expect(api.refreshCount == 1)
        #expect(Set(tokens) == ["access-1"])
    }

    @Test
    func suresiDolmakUzereOlanTokenOnceYenilenir() async throws {
        let api = FakeAuthAPI(now: now)
        let manager = makeManager(api: api)
        await manager.start(api.tokens(access: "old", expiresIn: 10))

        #expect(try await manager.accessToken() == "access-1")
    }

    @Test
    func yenilemeReddedilirseOturumBiter() async {
        let api = FakeAuthAPI(now: now, refreshFails: true)
        let manager = makeManager(api: api)
        let expired = Mutex(false)
        await manager.onExpired { expired.withLock { $0 = true } }
        await manager.start(api.tokens(access: "old", expiresIn: 900))

        await #expect(throws: SessionError.expired) {
            try await manager.refreshAfterUnauthorized(failedToken: "old")
        }
        #expect(expired.withLock { $0 })
        await #expect(throws: SessionError.expired) {
            try await manager.accessToken()
        }
    }

    @Test
    func yenilenenRefreshTokenKeychaineYazilir() async throws {
        let api = FakeAuthAPI(now: now)
        let credentials = InMemoryCredentialStore()
        let manager = makeManager(api: api, credentials: credentials)
        await manager.start(api.tokens(access: "old", expiresIn: 900))

        _ = try await manager.refreshAfterUnauthorized(failedToken: "old")

        #expect(credentials.load()?.refreshToken == "refresh-access-1")
    }

    @Test
    func authenticatedClient401deBirKezYenileyipTekrarDener() async throws {
        let api = FakeAuthAPI(now: now)
        let manager = makeManager(api: api)
        await manager.start(api.tokens(access: "old", expiresIn: 900))
        let base = RecordingClient()

        let client = AuthenticatedHTTPClient(base: base, session: manager)
        _ = try await client.send(APIRequest<EmptyResponse>.get("/v1/me"))

        #expect(base.authorizations == ["Bearer old", "Bearer access-1"])
    }

    private func makeManager(api: FakeAuthAPI, credentials: InMemoryCredentialStore = InMemoryCredentialStore()) -> SessionManager {
        SessionManager(api: api, credentials: credentials, keyStore: InMemoryKeyStore(), clock: ServerClock(deviceNow: { [now] in now }))
    }
}

/// İlk istekte 401, sonrakilerde başarılı.
private final class RecordingClient: HTTPClient {
    private let headers = Mutex<[String]>([])

    var authorizations: [String] { headers.withLock { $0 } }

    func send<Response>(_ request: APIRequest<Response>) async throws -> Response {
        let count = headers.withLock { value -> Int in
            value.append(request.headers["Authorization"] ?? "")
            return value.count
        }
        if count == 1 {
            throw APIError.server(status: 401, message: nil)
        }
        return EmptyResponse() as! Response
    }
}
