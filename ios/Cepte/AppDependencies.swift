//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import AppConfiguration
import AuthFlow
import DeviceSecurity
import DevMenu
import FoodMiniApp
import Home
import MiniAppKit
import Networking
import SessionKit
import TaxiMiniApp
import UIKit

/// Uygulamanın bağımlılık kökü (composition root). Spring'deki ApplicationContext'in elle kurulmuş hali:
/// hangi protokolün hangi gerçek sınıfla karşılanacağına sadece burada karar verilir.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
@MainActor
final class AppDependencies {
    let environment: APIEnvironment
    let configStore: AppConfigStore
    let sessionStore: SessionStore
    /// Oturum gerektiren uçlar için; token ekler ve gerekirse yeniler.
    let authenticatedClient: any HTTPClient
    let miniAppRegistry: MiniAppRegistry
    let homeViewModel: HomeViewModel

    // KYC seviyesi CP-3'te backend'den gelecek; o zamana kadar herkes seviye 0
    let kycLevel = 0

    private let authAPI: any AuthAPI
    private let keyStore: any DeviceKeyStore
    private let simulatorClient: any SimulatorClient

    init(environment: APIEnvironment) {
        self.environment = environment
        let serverClock = ServerClock()
        let backend = URLSessionHTTPClient(baseURL: environment.backendURL)
        let credentials = KeychainCredentialStore()
        keyStore = KeychainDeviceKeyStore()
        authAPI = LiveAuthAPI(http: backend, signer: RequestSigner(clock: serverClock))

        let sessionManager = SessionManager(api: authAPI, credentials: credentials, keyStore: keyStore, clock: serverClock)
        sessionStore = SessionStore(manager: sessionManager, api: authAPI, credentials: credentials, keyStore: keyStore,
                                    biometrics: BiometricAuthenticator())
        authenticatedClient = AuthenticatedHTTPClient(base: backend, session: sessionManager)

        configStore = AppConfigStore(client: backend, serverClock: serverClock)
        miniAppRegistry = MiniAppRegistry(apps: [FoodMiniApp(), TaxiMiniApp()])
        homeViewModel = HomeViewModel(configStore: configStore, registry: miniAppRegistry, kycLevel: kycLevel)
        simulatorClient = LiveSimulatorClient(http: URLSessionHTTPClient(baseURL: environment.simulatorURL))
    }

    func makeOnboardingViewModel() -> OnboardingViewModel {
        var codePeek: (@Sendable (String) async -> String?)?
        #if DEBUG
        let simulator = simulatorClient
        codePeek = { phone in try? await simulator.messages(phone: phone, limit: 1).first?.code }
        #endif
        return OnboardingViewModel(api: authAPI, keyStore: keyStore, sessionStore: sessionStore,
                                   deviceName: "\(UIDevice.current.model) · iOS \(UIDevice.current.systemVersion)",
                                   codePeek: codePeek)
    }

    func makeDevMenuViewModel() -> DevMenuViewModel {
        DevMenuViewModel(environment: environment, configStore: configStore, simulator: simulatorClient)
    }
}
