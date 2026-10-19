//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import Foundation

/// Backend'e atılacak bir istek ve cevabının tipi.
///
/// Cevap tipi generic parametrede taşındığı için `client.send(.get("/v1/app/config"))` çağrısının ne döneceğini
/// derleyici bilir; Java'daki `ParameterizedTypeReference` ihtiyacı yoktur.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
public struct APIRequest<Response: Decodable & Sendable>: Sendable {
    public enum Method: String, Sendable {
        case get = "GET"
        case post = "POST"
        case put = "PUT"
        case delete = "DELETE"
    }

    public let method: Method
    public let path: String
    public let body: Data?
    public var headers: [String: String]

    public init(method: Method, path: String, body: Data? = nil, headers: [String: String] = [:]) {
        self.method = method
        self.path = path
        self.body = body
        self.headers = headers
    }

    public static func get(_ path: String) -> APIRequest {
        APIRequest(method: .get, path: path)
    }

    public static func post(_ path: String, body: some Encodable) throws -> APIRequest {
        APIRequest(method: .post, path: path, body: try JSONCoding.encoder().encode(body))
    }

    public static func put(_ path: String, body: some Encodable) throws -> APIRequest {
        APIRequest(method: .put, path: path, body: try JSONCoding.encoder().encode(body))
    }
}

/// Gövdesi olmayan cevaplar için (`object: null`).
public struct EmptyResponse: Decodable, Sendable, Equatable {
    public init() {}
}
