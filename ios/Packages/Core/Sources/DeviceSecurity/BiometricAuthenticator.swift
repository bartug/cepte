//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import Foundation
import LocalAuthentication

/// Face ID / Touch ID. Sonuç backend'e gitmez; başarılı olursa biyometrik anahtarı açacak bir `LAContext` döner.
///
/// Simülatörde: Features > Face ID > Enrolled, ardından Matching Face / Non-matching Face.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 23.10.2026 - CP-2
@MainActor
public final class BiometricAuthenticator {

    public init() {}

    public var isAvailable: Bool {
        LAContext().canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: nil)
    }

    public var displayName: String {
        let context = LAContext()
        _ = context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: nil)
        return context.biometryType == .touchID ? "Touch ID" : "Face ID"
    }

    public func authenticate(reason: String) async throws -> LAContext {
        let context = LAContext()
        context.localizedCancelTitle = "PIN kullan"
        try await context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: reason)
        return context
    }
}
