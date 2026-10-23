//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import Foundation
import Networking

/// Oturum gerektiren isteklerin istemcisi. Access token'ı ekler; 401 gelirse bir kez yenileyip tekrar dener.
///
/// Feature'lar token'ı hiç görmez, sadece bu istemciyi kullanır.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 23.10.2026 - CP-2
public struct AuthenticatedHTTPClient: HTTPClient {
    private let base: any HTTPClient
    private let session: SessionManager

    public init(base: any HTTPClient, session: SessionManager) {
        self.base = base
        self.session = session
    }

    public func send<Response>(_ request: APIRequest<Response>) async throws -> Response {
        let token = try await session.accessToken()
        do {
            return try await base.send(authorized(request, token: token))
        } catch APIError.server(401, _) {
            let renewed = try await session.refreshAfterUnauthorized(failedToken: token)
            return try await base.send(authorized(request, token: renewed))
        }
    }

    private func authorized<Response>(_ request: APIRequest<Response>, token: String) -> APIRequest<Response> {
        var request = request
        request.headers["Authorization"] = "Bearer \(token)"
        return request
    }
}
