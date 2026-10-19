//
//  Copyright (c) 2026. Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
//

import DesignSystem
import MiniAppKit
import SwiftUI

/// Ana sayfa: bakiye kartı ve mini uygulamalar.
///
/// - Author: Bartuğ Sevindik <bartug.sevindik@taksidepos.com>
/// - Since: 19.10.2026 - CP-1
public struct HomeView: View {
    @State private var viewModel: HomeViewModel
    @State private var lockedEntry: MiniAppEntry?
    private let onOpenMiniApp: (MiniAppEntry) -> Void

    private let columns = [GridItem(.adaptive(minimum: 84), spacing: Spacing.m)]

    public init(viewModel: HomeViewModel, onOpenMiniApp: @escaping (MiniAppEntry) -> Void) {
        _viewModel = State(initialValue: viewModel)
        self.onOpenMiniApp = onOpenMiniApp
    }

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Spacing.l) {
                Text(viewModel.greeting)
                    .font(Typography.title)
                    .foregroundStyle(Palette.textPrimary)

                balanceCard

                Text("Hizmetler")
                    .font(Typography.headline)
                    .foregroundStyle(Palette.textSecondary)

                LazyVGrid(columns: columns, spacing: Spacing.l) {
                    ForEach(viewModel.miniApps) { entry in
                        Button {
                            if entry.isLocked {
                                lockedEntry = entry
                            } else {
                                onOpenMiniApp(entry)
                            }
                        } label: {
                            MiniAppTile(entry: entry)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .padding(Spacing.m)
        }
        .background(Palette.background)
        .refreshable { await viewModel.refresh() }
        .alert("Kimliğini doğrula", isPresented: Binding(get: { lockedEntry != nil }, set: { if !$0 { lockedEntry = nil } })) {
            Button("Tamam", role: .cancel) {}
        } message: {
            Text("\(lockedEntry?.title ?? "") için kimlik doğrulaması gerekiyor. Bu akış CP-3'te gelecek.")
        }
    }

    private var balanceCard: some View {
        VStack(alignment: .leading, spacing: Spacing.s) {
            Text("Cüzdan bakiyesi")
                .font(Typography.caption)
                .foregroundStyle(.white.opacity(0.8))
            MoneyText(minorUnits: 0, font: Typography.amountLarge)
                .foregroundStyle(.white)
            Text("Cüzdan CP-4'te açılacak")
                .font(Typography.caption)
                .foregroundStyle(.white.opacity(0.8))
        }
        .padding(Spacing.l)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(colors: [Palette.brand, Palette.brand.opacity(0.75)], startPoint: .topLeading, endPoint: .bottomTrailing),
            in: RoundedRectangle(cornerRadius: Radius.l)
        )
    }
}

struct MiniAppTile: View {
    let entry: MiniAppEntry

    var body: some View {
        VStack(spacing: Spacing.xs) {
            Image(systemName: entry.icon)
                .font(.title2)
                .foregroundStyle(Palette.brand)
                .frame(width: 60, height: 60)
                .background(Palette.brandSoft, in: RoundedRectangle(cornerRadius: Radius.m))
                .overlay(alignment: .topTrailing) {
                    if entry.isLocked {
                        Image(systemName: "lock.fill")
                            .font(.caption2)
                            .padding(4)
                            .background(Palette.surface, in: Circle())
                            .offset(x: 6, y: -6)
                    }
                }
            Text(entry.title)
                .font(Typography.caption)
                .foregroundStyle(Palette.textPrimary)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel(entry.isLocked ? "\(entry.title), kilitli" : entry.title)
    }
}
