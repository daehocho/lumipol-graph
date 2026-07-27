#!/usr/bin/env bash
# 코어 버전 고정 검사 (docs/refactor/50-guardrails.md §3).
# 1) 체크인된 xcframework가 현재 core/ 콘텐츠에서 빌드됐는가 (동봉 해시 대조)
# 2) core / android-renderer 의 version 선언이 일치하는가
set -euo pipefail
cd "$(dirname "$0")/.."

EMBEDDED_FILE=ios-renderer/Frameworks/LumipolGraph.xcframework/CORE_CONTENT_HASH
if [[ ! -f "$EMBEDDED_FILE" ]]; then
  echo "✗ xcframework에 CORE_CONTENT_HASH 없음 — scripts/sync-xcframework.sh 로 재생성 필요" >&2
  exit 1
fi
EMBEDDED=$(cat "$EMBEDDED_FILE")
CURRENT=$(scripts/core-content-hash.sh)
if [[ "$EMBEDDED" != "$CURRENT" ]]; then
  echo "✗ xcframework가 현재 core/ 와 다른 콘텐츠에서 빌드됨" >&2
  echo "  embedded: $EMBEDDED" >&2
  echo "  current : $CURRENT" >&2
  exit 1
fi
echo "✓ xcframework == core/ (hash $CURRENT)"

CORE_VER=$(sed -n 's/^version = "\(.*\)"/\1/p' core/build.gradle.kts)
RENDERER_VER=$(sed -n 's/^version = "\(.*\)"/\1/p' android-renderer/build.gradle.kts)
if [[ "$CORE_VER" != "$RENDERER_VER" || -z "$CORE_VER" ]]; then
  echo "✗ version 불일치: core=$CORE_VER renderer=$RENDERER_VER" >&2
  exit 1
fi
echo "✓ version 일치: $CORE_VER"
