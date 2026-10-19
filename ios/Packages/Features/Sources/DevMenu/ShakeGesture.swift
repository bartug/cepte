//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import SwiftUI
import UIKit

extension Notification.Name {
    static let deviceDidShake = Notification.Name("cepte.deviceDidShake")
}

// SwiftUI'de sallama hareketi için hazır bir API yok; UIKit'in pencere olayı bildirime çevrilir
extension UIWindow {
    open override func motionEnded(_ motion: UIEvent.EventSubtype, with event: UIEvent?) {
        if motion == .motionShake {
            NotificationCenter.default.post(name: .deviceDidShake, object: nil)
        }
        super.motionEnded(motion, with: event)
    }
}

public extension View {
    /// Cihaz sallandığında çalışır.
    func onDeviceShake(perform action: @escaping () -> Void) -> some View {
        onReceive(NotificationCenter.default.publisher(for: .deviceDidShake)) { _ in
            action()
        }
    }
}
