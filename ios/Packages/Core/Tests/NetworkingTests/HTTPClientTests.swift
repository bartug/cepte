//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import Foundation
import Testing
@testable import Networking

@Suite(.serialized)
struct HTTPClientTests {

    private struct Greeting: Decodable, Sendable, Equatable {
        let text: String
        let at: Date
    }

    @Test
    func basariliCevapZarftanCikarilir() async throws {
        let client = makeClient(status: 200, body: """
            {"success": true, "message": "Tamam", "object": {"text": "Merhaba", "at": "2026-10-19T09:00:00.172954Z"}, "httpStatusCode": 200}
            """)

        let greeting = try await client.send(APIRequest<Greeting>.get("/greeting"))

        #expect(greeting.text == "Merhaba")
        #expect(greeting.at.timeIntervalSince1970.rounded() == 1_792_400_400)
    }

    @Test
    func kusuratsizTarihOkunur() throws {
        let data = Data(#"{"text": "x", "at": "2026-10-19T09:00:00Z"}"#.utf8)

        let greeting = try JSONCoding.decoder().decode(Greeting.self, from: data)

        #expect(greeting.at.timeIntervalSince1970 == 1_792_400_400)
    }

    @Test
    func backendMesajiHataOlarakDoner() async {
        let client = makeClient(status: 404, body: """
            {"success": false, "message": "Mini uygulama bulunamadı: CINEMA", "httpStatusCode": 404}
            """)

        await #expect(throws: APIError.server(status: 404, message: "Mini uygulama bulunamadı: CINEMA")) {
            try await client.send(APIRequest<Greeting>.get("/x"))
        }
    }

    @Test
    func zarfsizHataCevabi() async {
        let client = makeClient(status: 502, body: "<html>Bad Gateway</html>")

        await #expect(throws: APIError.server(status: 502, message: nil)) {
            try await client.send(APIRequest<Greeting>.get("/x"))
        }
    }

    @Test
    func bosGovdeliCevap() async throws {
        let client = makeClient(status: 200, body: #"{"success": true, "message": "Güncellendi.", "httpStatusCode": 200}"#)

        let response = try await client.send(APIRequest<EmptyResponse>.get("/x"))

        #expect(response == EmptyResponse())
    }

    @Test
    func sorguParametreliYolBozulmaz() async throws {
        let client = makeClient(status: 200, body: #"{"success": true, "httpStatusCode": 200}"#)

        _ = try await client.send(APIRequest<EmptyResponse>.get("/sim/sms?limit=5&phone=%2B905321234567"))

        #expect(StubURLProtocol.lastURL?.absoluteString == "https://test.local/sim/sms?limit=5&phone=%2B905321234567")
    }

    private func makeClient(status: Int, body: String) -> URLSessionHTTPClient {
        StubURLProtocol.response = (status, Data(body.utf8))
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [StubURLProtocol.self]
        return URLSessionHTTPClient(baseURL: URL(string: "https://test.local")!, session: URLSession(configuration: configuration))
    }
}

/// İsteği ağa çıkarmadan sabit cevap döner.
final class StubURLProtocol: URLProtocol, @unchecked Sendable {
    nonisolated(unsafe) static var response: (Int, Data) = (200, Data())
    nonisolated(unsafe) static var lastURL: URL?

    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

    override func startLoading() {
        let (status, data) = Self.response
        Self.lastURL = request.url
        let response = HTTPURLResponse(url: request.url!, statusCode: status, httpVersion: nil, headerFields: nil)!
        client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
        client?.urlProtocol(self, didLoad: data)
        client?.urlProtocolDidFinishLoading(self)
    }

    override func stopLoading() {}
}
