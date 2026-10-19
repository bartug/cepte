//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import Foundation

struct Restaurant: Identifiable, Hashable {
    let id: Int
    let name: String
    let cuisine: String
    let rating: Double
    let deliveryMinutes: ClosedRange<Int>
    let minimumBasket: Int64
    let menu: [MenuItem]
}

struct MenuItem: Identifiable, Hashable {
    let id: Int
    let name: String
    let price: Int64
}

// CP-6'da backend'deki commerce modülünden gelecek
enum SampleRestaurants {
    static let all: [Restaurant] = [
        Restaurant(id: 1, name: "Burger House", cuisine: "Burger", rating: 4.6, deliveryMinutes: 25...35, minimumBasket: 25_000,
                   menu: [MenuItem(id: 11, name: "Klasik Burger", price: 32_000),
                          MenuItem(id: 12, name: "Cheddar Burger", price: 36_500),
                          MenuItem(id: 13, name: "Patates Kızartması", price: 9_000)]),
        Restaurant(id: 2, name: "Kebapçı Halil", cuisine: "Kebap", rating: 4.8, deliveryMinutes: 35...45, minimumBasket: 30_000,
                   menu: [MenuItem(id: 21, name: "Adana Dürüm", price: 28_000),
                          MenuItem(id: 22, name: "Urfa Porsiyon", price: 42_000),
                          MenuItem(id: 23, name: "Ayran", price: 4_000)]),
        Restaurant(id: 3, name: "Pizza Napoli", cuisine: "Pizza", rating: 4.4, deliveryMinutes: 30...40, minimumBasket: 20_000,
                   menu: [MenuItem(id: 31, name: "Margherita", price: 29_000),
                          MenuItem(id: 32, name: "Karışık", price: 34_000)]),
    ]
}
