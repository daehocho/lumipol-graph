#!/usr/bin/env bash
# 동일성 골든 테스트 게이트 (docs/refactor/50-guardrails.md §1).
# 코어 하네스 덤프를 JVM·iOS 시뮬레이터 양쪽에서 생성해 커밋된 골든과 완전 일치(--strict)로 대조한다.
# 골든 갱신: 의도 변경 커밋에서 core-dump-jvm.json을 core/golden/core-dump.golden.json으로 복사.
set -euo pipefail
cd "$(dirname "$0")/.."

GOLDEN=core/golden/core-dump.golden.json
DUMP_DIR=${LUMIPOL_DUMP_DIR:-/tmp/lumipol-graph-dump}
export DEVELOPER_DIR=${DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}

./gradlew :core:jvmTest --tests "com.lumipol.graph.harness.*" --rerun --quiet
python3 scripts/diff-core-dump.py "$GOLDEN" "$DUMP_DIR/core-dump-jvm.json" --strict
echo "✓ JVM == golden"

./gradlew :core:iosSimulatorArm64Test --tests "com.lumipol.graph.harness.*" --rerun --quiet
python3 scripts/diff-core-dump.py "$GOLDEN" "$DUMP_DIR/core-dump-iosSimulatorArm64.json" --strict
echo "✓ iosSimulatorArm64 == golden"
