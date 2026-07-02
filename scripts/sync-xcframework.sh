#!/usr/bin/env bash
# KMP 코어 xcframework를 빌드해 iOS 렌더러 패키지 안으로 복사한다.
# SPM binary target은 패키지 내부 경로만 안전하게 참조할 수 있어 복사 방식을 쓴다.
set -euo pipefail
cd "$(dirname "$0")/.."
./gradlew :core:assembleLumipolGraphReleaseXCFramework
mkdir -p ios-renderer/Frameworks
rsync -a --delete core/build/XCFrameworks/release/LumipolGraph.xcframework ios-renderer/Frameworks/
echo "Synced LumipolGraph.xcframework -> ios-renderer/Frameworks/"
