//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import CryptoKit
import Foundation
import Networking
import Testing
@testable import DeviceSecurity

struct RequestSignerTests {

    @Test
    func imzalananMetinBackendIleAyni() {
        // Backend'deki DeviceSignature.stringToSign ile aynı girdi; boş gövdenin SHA-256'sı sabittir
        let text = RequestSigner.stringToSign(method: "post", path: "/v1/auth/refresh", timestamp: "1792400400",
                                              nonce: "n-1", body: Data())

        #expect(text == "POST\n/v1/auth/refresh\n1792400400\nn-1\ne3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
    }

    @Test
    func imzaPublicKeyIleDogrulanir() throws {
        let key = SoftwareSigningKey(key: P256.Signing.PrivateKey())
        let clock = ServerClock(deviceNow: { Date(timeIntervalSince1970: 1_792_400_400) })
        let signer = RequestSigner(clock: clock, nonce: { "n-1" })
        let body = Data(#"{"pin":"482915"}"#.utf8)

        let signed = try signer.sign(APIRequest<EmptyResponse>(method: .post, path: "/v1/auth/login", body: body),
                                     deviceId: 7, deviceKey: key)

        #expect(signed.headers["X-Device-Id"] == "7")
        #expect(signed.headers["X-Timestamp"] == "1792400400")
        let signature = try P256.Signing.ECDSASignature(derRepresentation: Data(base64Encoded: signed.headers["X-Signature"]!)!)
        let message = RequestSigner.stringToSign(method: "POST", path: "/v1/auth/login", timestamp: "1792400400", nonce: "n-1", body: body)
        let publicKey = try P256.Signing.PublicKey(derRepresentation: key.publicKeyDER)
        #expect(publicKey.isValidSignature(signature, for: Data(message.utf8)))
    }

    @Test
    func zamanDamgasiSunucuSaatindenAlinir() throws {
        let deviceNow = Date(timeIntervalSince1970: 1_792_400_400)
        let clock = ServerClock(deviceNow: { deviceNow })
        clock.update(serverTime: deviceNow.addingTimeInterval(31 * 86_400))
        let signer = RequestSigner(clock: clock)

        let signed = try signer.sign(APIRequest<EmptyResponse>.get("/v1/me"), deviceId: 1,
                                     deviceKey: SoftwareSigningKey(key: P256.Signing.PrivateKey()))

        #expect(signed.headers["X-Timestamp"] == String(1_792_400_400 + 31 * 86_400))
    }
}
