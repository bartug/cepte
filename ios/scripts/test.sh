#!/usr/bin/env bash
# Tüm SPM paketlerinin testlerini simülatörde çalıştırır, ardından uygulamayı derler.
# Kullanım: ios/scripts/test.sh ["iPhone 17 Pro"]
set -euo pipefail

DEVICE="${1:-iPhone 17 Pro}"
DESTINATION="platform=iOS Simulator,name=${DEVICE}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

run_tests() {
  local package="$1" scheme="$2"
  echo "▶ ${package} (${scheme})"
  (cd "${ROOT}/Packages/${package}" && xcodebuild test -scheme "${scheme}" -destination "${DESTINATION}" -quiet)
}

run_tests Core Core-Package
run_tests DesignSystem DesignSystem
run_tests MiniAppKit MiniAppKit
run_tests Features Features-Package

echo "▶ Cepte uygulaması"
xcodebuild build -project "${ROOT}/Cepte.xcodeproj" -scheme Cepte -destination "${DESTINATION}" -quiet
echo "✔ Tüm testler geçti, uygulama derlendi."
