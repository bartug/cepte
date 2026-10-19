//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import DesignSystem
import SwiftUI

struct Place: Identifiable, Hashable {
    let id: Int
    let name: String
    // Örnek ücret hesabı için Kadıköy'e uzaklık
    let kilometers: Int
}

enum SamplePlaces {
    static let all = [
        Place(id: 1, name: "Kadıköy İskele", kilometers: 0),
        Place(id: 2, name: "Moda", kilometers: 2),
        Place(id: 3, name: "Ataşehir", kilometers: 9),
        Place(id: 4, name: "Levent", kilometers: 17),
        Place(id: 5, name: "Sabiha Gökçen Havalimanı", kilometers: 35),
    ]
}

struct RideRequestView: View {
    @State private var pickup = SamplePlaces.all[0]
    @State private var destination = SamplePlaces.all[3]

    var body: some View {
        Form {
            Section("Yolculuk") {
                Picker("Nereden", selection: $pickup) {
                    ForEach(SamplePlaces.all) { Text($0.name).tag($0) }
                }
                Picker("Nereye", selection: $destination) {
                    ForEach(SamplePlaces.all) { Text($0.name).tag($0) }
                }
            }
            Section {
                LabeledContent("Tahmini ücret") {
                    MoneyText(minorUnits: TaxiFare.estimate(from: pickup, to: destination))
                }
            } footer: {
                Text("Yolculuk başlarken tahmini ücret cüzdanda bloke edilir, bitişte gerçek ücret tahsil edilir (CP-6).")
            }
            Section {
                Button("Taksi çağır") {}
                    .buttonStyle(.primary)
                    .disabled(true)
                    .listRowInsets(EdgeInsets())
            }
        }
        .navigationTitle("Taksi")
    }
}

enum TaxiFare {
    static let opening: Int64 = 5_000
    static let perKilometer: Int64 = 3_200

    static func estimate(from: Place, to: Place) -> Int64 {
        let distance = Int64(max(abs(from.kilometers - to.kilometers), 1))
        return opening + distance * perKilometer
    }
}

#Preview {
    NavigationStack {
        RideRequestView()
    }
}
