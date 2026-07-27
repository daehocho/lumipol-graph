#!/usr/bin/env bash
# KMP 코어 xcframework를 빌드해 iOS 렌더러 패키지 안으로 복사한다.
# SPM binary target은 패키지 내부 경로만 안전하게 참조할 수 있어 복사 방식을 쓴다.
set -euo pipefail
cd "$(dirname "$0")/.."
./gradlew :core:assembleLumipolGraphReleaseXCFramework
mkdir -p ios-renderer/Frameworks
rsync -a --delete core/build/XCFrameworks/release/LumipolGraph.xcframework ios-renderer/Frameworks/
# 빌드 원본 코어의 콘텐츠 해시를 동봉 — "체크인된 xcframework가 현재 코어에서 빌드됐나"를
# scripts/check-version-lock.sh 가 커밋 이력 없이 직접 검증한다(50-guardrails §3).
# 커밋 해시가 아니라 콘텐츠 해시인 이유: 재생성과 코어 변경이 같은 커밋에 담기는 규약이라
# 빌드 시점엔 그 커밋이 아직 존재하지 않는다.
scripts/core-content-hash.sh > ios-renderer/Frameworks/LumipolGraph.xcframework/CORE_CONTENT_HASH
echo "Synced LumipolGraph.xcframework -> ios-renderer/Frameworks/ (hash: $(cat ios-renderer/Frameworks/LumipolGraph.xcframework/CORE_CONTENT_HASH))"
