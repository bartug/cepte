//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import SwiftUI

/// Uygulamanın giriş noktası. Java'daki `main` metodunun karşılığı `@main` işaretli tiptir.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
@main
struct CepteApp: App {
    // Bağımlılıklar uygulama boyunca bir kez oluşturulur; @State görünüm yeniden çizilse de nesneyi korur
    @State private var dependencies = AppDependencies(environment: .local)

    var body: some Scene {
        WindowGroup {
            RootView(dependencies: dependencies)
        }
    }
}
