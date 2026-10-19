//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import DesignSystem
import SwiftUI

struct RestaurantListView: View {
    var body: some View {
        List(SampleRestaurants.all) { restaurant in
            NavigationLink(value: restaurant) {
                VStack(alignment: .leading, spacing: Spacing.xxs) {
                    Text(restaurant.name)
                        .font(Typography.headline)
                    HStack(spacing: Spacing.xs) {
                        Label(restaurant.rating.formatted(.number.precision(.fractionLength(1))), systemImage: "star.fill")
                        Text("·")
                        Text("\(restaurant.deliveryMinutes.lowerBound)-\(restaurant.deliveryMinutes.upperBound) dk")
                        Text("·")
                        Text("Min. \(MoneyFormatter.format(minorUnits: restaurant.minimumBasket))")
                    }
                    .font(Typography.caption)
                    .foregroundStyle(Palette.textSecondary)
                }
                .padding(.vertical, Spacing.xxs)
            }
        }
        .navigationTitle("Yemek")
        .navigationDestination(for: Restaurant.self) { restaurant in
            RestaurantDetailView(restaurant: restaurant)
        }
    }
}

struct RestaurantDetailView: View {
    let restaurant: Restaurant

    var body: some View {
        List {
            Section("Menü") {
                ForEach(restaurant.menu) { item in
                    HStack {
                        Text(item.name)
                        Spacer()
                        MoneyText(minorUnits: item.price, font: Typography.body)
                    }
                }
            }
            Section {
                Button("Sepete ekle") {}
                    .buttonStyle(.primary)
                    .disabled(true)
                    .listRowInsets(EdgeInsets())
            } footer: {
                Text("Sepet ve ortak checkout CP-6'da gelecek.")
            }
        }
        .navigationTitle(restaurant.name)
    }
}

#Preview {
    NavigationStack {
        RestaurantListView()
    }
}
