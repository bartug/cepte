//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import AppConfiguration
import DevMenu
import FoodMiniApp
import Home
import MiniAppKit
import Networking
import TaxiMiniApp

/// Uygulamanın bağımlılık kökü (composition root). Spring'deki ApplicationContext'in elle kurulmuş hali:
/// hangi protokolün hangi gerçek sınıfla karşılanacağına sadece burada karar verilir.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
@MainActor
final class AppDependencies {
    let environment: APIEnvironment
    let configStore: AppConfigStore
    let miniAppRegistry: MiniAppRegistry
    let homeViewModel: HomeViewModel

    // Kullanıcı kimliği CP-2/CP-3'te gelecek; o zamana kadar herkes seviye 0
    let kycLevel = 0

    private let simulatorClient: any SimulatorClient

    init(environment: APIEnvironment) {
        self.environment = environment
        configStore = AppConfigStore(client: URLSessionHTTPClient(baseURL: environment.backendURL))
        miniAppRegistry = MiniAppRegistry(apps: [FoodMiniApp(), TaxiMiniApp()])
        homeViewModel = HomeViewModel(configStore: configStore, registry: miniAppRegistry, kycLevel: kycLevel)
        simulatorClient = LiveSimulatorClient(http: URLSessionHTTPClient(baseURL: environment.simulatorURL))
    }

    func makeDevMenuViewModel() -> DevMenuViewModel {
        DevMenuViewModel(environment: environment, configStore: configStore, simulator: simulatorClient)
    }
}
