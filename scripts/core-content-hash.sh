#!/usr/bin/env bash
# 코어 프로덕션 입력(커먼 소스 + 빌드 스크립트)의 결정론적 콘텐츠 해시.
# commonTest는 xcframework 산출물에 영향이 없어 제외한다.
set -euo pipefail
cd "$(dirname "$0")/.."
{
  find core/src/commonMain core/src/androidMain -type f | LC_ALL=C sort | xargs shasum -a 256
  shasum -a 256 core/build.gradle.kts
} | shasum -a 256 | cut -d' ' -f1
