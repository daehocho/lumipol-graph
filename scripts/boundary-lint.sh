#!/usr/bin/env bash
# 경계 정책 금지 패턴 게이트 (docs/refactor/50-guardrails.md §5, 30-boundary-policy.md §4).
# 렌더러(+옵션으로 앱 차트 디렉토리) 소스를 grep으로 검사한다. 위반 시 실패.
# 정당한 예외는 해당 줄(또는 바로 윗줄)에 `boundary-allow: 사유` 주석을 남긴다.
#
# 사용법: scripts/boundary-lint.sh [추가 검사 디렉토리...]
#   예) scripts/boundary-lint.sh ~/Runday_IOS/RunDay/Sources/Chart ~/Runday_AOS/.../chart
set -uo pipefail
cd "$(dirname "$0")/.."

RENDERER_DIRS=(
  "android-renderer/src/main"
  "ios-renderer/Sources"
)
EXTRA_DIRS=("${@:1}")
FAIL=0

# match <설명> <디렉토리들...> -- <grep -E 패턴>
check() {
  local desc="$1"; shift
  local dirs=()
  while [[ "$1" != "--" ]]; do dirs+=("$1"); shift; done
  shift
  local pattern="$1"
  local hits
  hits=$(grep -rnE --include='*.kt' --include='*.swift' "$pattern" "${dirs[@]}" 2>/dev/null \
    | grep -v "boundary-allow" \
    | grep -vE '^\S+:[0-9]+:\s*(//|///|\*|/\*)' || true)
  # 바로 윗줄 boundary-allow 허용: 파일:줄 목록을 다시 검사
  if [[ -n "$hits" ]]; then
    local filtered=""
    while IFS= read -r line; do
      local file="${line%%:*}"
      local rest="${line#*:}"
      local lineno="${rest%%:*}"
      if [[ "$lineno" =~ ^[0-9]+$ ]] && (( lineno > 1 )); then
        local prev
        prev=$(sed -n "$((lineno - 1))p" "$file")
        [[ "$prev" == *boundary-allow* ]] && continue
      fi
      filtered+="$line"$'\n'
    done <<< "$hits"
    hits="${filtered%$'\n'}"
  fi
  if [[ -n "$hits" ]]; then
    echo "✗ $desc"
    echo "$hits" | sed 's/^/    /'
    FAIL=1
  else
    echo "✓ $desc"
  fi
}

ALL_DIRS=("${RENDERER_DIRS[@]}")
if (( ${#EXTRA_DIRS[@]} )); then ALL_DIRS+=("${EXTRA_DIRS[@]}"); fi

# 1. 렌더러 표시 문자열 생성 금지(정책 §4-4) — 포맷 지시자를 담은 포맷 호출.
check "렌더러 표시 문자열 생성 금지(String.format/%-포맷)" "${ALL_DIRS[@]}" -- \
  'String\.format\(|String\(format:'

# 2. 코어 commonMain libm 재유입 금지(정책 §5) — atan2는 이산 출력 전용으로 허용(B4 KDoc).
#    \basin( 은 \bsin( 에 안 걸린다(단어 경계) — 역삼각·tan 계열은 별도 나열.
check "코어 libm 재유입 금지(log10/pow/sin/cos/tan/asin/acos/atan/exp/ln)" "core/src/commonMain" -- \
  '\blog10\(|\bexp\(|kotlin\.math\.pow|\.pow\(|\bsin\(|\bcos\(|\btan\(|\basin\(|\bacos\(|\batan\(|\bln\('

# 3. 정책성 숫자 상수 신설 금지(휴리스틱) — 렌더러의 새 소수 상수는 ChartDefaults 참조여야 한다.
#    플랫폼 보정(헤어라인·fontScale 등)은 boundary-allow로 표시.
#    이름은 숫자 포함(fadeAlpha2 등) 허용, 리터럴 뒤 트레일링 주석이 있어도 잡는다(우회 방지).
check "렌더러 정책 숫자 상수 신설 금지(코어 상수 참조 필요)" "${ALL_DIRS[@]}" -- \
  '(private )?(static )?(const )?(val|let) [A-Za-z_][A-Za-z0-9_]*(: [A-Za-z]+)? = -?[0-9]+\.[0-9]+f?[[:space:]]*(//.*)?$'

# 4. 스냅/히트테스트 재구현 금지 — 근접 탐색·인덱스 산술은 코어 query 소관(B2/B4).
check "렌더러 스냅/히트테스트 재구현 금지" "${ALL_DIRS[@]}" -- \
  'minByOrNull \{ abs\(|min\(by: \{ abs\('

if (( FAIL )); then
  echo "결과: 경계 정책 위반 — 위 항목을 코어로 이관하거나 'boundary-allow: 사유'를 남길 것"
  exit 1
fi
echo "결과: 경계 정책 통과"
