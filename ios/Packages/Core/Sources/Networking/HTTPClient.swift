//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import Foundation

/// Ağ istemcisi. Ekranlar ve servisler bu protokole bağımlıdır; testte sahte bir istemci verilir.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
public protocol HTTPClient: Sendable {
    func send<Response>(_ request: APIRequest<Response>) async throws -> Response
}

/// `URLSession` üzerinden çalışan gerçek istemci.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
public final class URLSessionHTTPClient: HTTPClient {
    private let baseURL: URL
    private let session: URLSession

    public init(baseURL: URL, session: URLSession = .shared) {
        self.baseURL = baseURL
        self.session = session
    }

    public func send<Response>(_ request: APIRequest<Response>) async throws -> Response {
        let (data, response) = try await perform(makeURLRequest(request))
        guard let http = response as? HTTPURLResponse else {
            throw APIError.decoding("HTTP cevabı değil")
        }
        let isSuccessStatus = (200..<300).contains(http.statusCode)

        let envelope: ResponseEnvelope<Response>
        do {
            envelope = try JSONCoding.decoder().decode(ResponseEnvelope<Response>.self, from: data)
        } catch {
            // 502 gibi cevaplar backend'den değil proxy'den gelir, zarf yoktur
            if !isSuccessStatus {
                throw APIError.server(status: http.statusCode, message: nil)
            }
            throw APIError.decoding(String(describing: error))
        }

        guard isSuccessStatus, envelope.success else {
            throw APIError.server(status: http.statusCode, message: envelope.message)
        }
        if let object = envelope.object {
            return object
        }
        if let empty = EmptyResponse() as? Response {
            return empty
        }
        throw APIError.emptyBody
    }

    private func makeURLRequest<Response>(_ request: APIRequest<Response>) -> URLRequest {
        // appending(path:) "?" işaretini de encode ederdi; sorgu parametreli yollar için göreli URL
        let url = URL(string: request.path, relativeTo: baseURL)?.absoluteURL ?? baseURL.appending(path: request.path)
        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = request.method.rawValue
        urlRequest.timeoutInterval = 15
        urlRequest.setValue("application/json", forHTTPHeaderField: "Accept")
        if let body = request.body {
            urlRequest.httpBody = body
            urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }
        for (name, value) in request.headers {
            urlRequest.setValue(value, forHTTPHeaderField: name)
        }
        return urlRequest
    }

    private func perform(_ urlRequest: URLRequest) async throws -> (Data, URLResponse) {
        do {
            return try await session.data(for: urlRequest)
        } catch let error as URLError {
            throw APIError.transport(error.code)
        }
    }
}
