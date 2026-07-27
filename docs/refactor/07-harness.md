# 07 — 대조 하네스 (0.7단계)

- 작성일: 2026-07-27
- 상태: **코어 덤프 작성 완료 — 사용자 실행 대기** (실행 결과가 오면 실측 diff 절을 채운다)
- 렌더러 덤프는 계획대로 이 단계에서 만들지 않는다 — 1단계(S2~)에서 서브시스템별로
  렌더러 자체 구현 항목이 식별될 때마다 증분 추가한다.

## 구성

| 파일 | 역할 |
|---|---|
| `core/src/commonTest/.../harness/JsonDump.kt` | 결정론적 직렬화 — 고정 소수 12자리 포매터(플랫폼 `toString` 미사용, IEEE 기본 연산만), JSON 빌더, 대형 배열 다이제스트 |
| `core/src/commonTest/.../harness/HarnessFixtures.kt` | 고정 입력 데이터셋(아래 표) — 난수·시각·초월 함수 없이 정수 모듈러로만 생성 |
| `core/src/commonTest/.../harness/CoreDump.kt` | 코어 중간 결과 전체를 JSON으로 조립 |
| `core/src/commonTest/.../harness/DumpHarnessTest.kt` | 덤프 진입점(테스트) — `/tmp/lumipol-graph-dump/core-dump-<플랫폼>.json` 기록 |
| `core/src/{jvmTest,androidUnitTest,iosTest}/.../DumpWriter.*.kt` | 파일 기록 expect/actual (테스트 전용 — 프로덕션 소스는 여전히 분기 0) |
| `scripts/diff-core-dump.py` | T1 완전 일치 / T2 오차(1e-9) diff + 오차 내 드리프트 집계 |
| `core/build.gradle.kts` 말미 | iOS 시뮬레이터 테스트 디바이스 지정 (`-PiosSimDevice=`로 재정의) |

프로덕션 로직 변경: **없음** (절대 규칙의 관측 지점 예외에 해당하는 코드만 추가).

## 입력 데이터셋 커버리지 (0.7단계 §1 요구 ↔ 픽스처)

| 요구 항목 | 픽스처 |
|---|---|
| 빈 배열 | L01, B01, N01, P02, hf_empty, ip_empty |
| 단일 포인트 | L02, N05, hf_single, ip_single |
| 두 포인트 (동일 x / 동일 y) | L03(+span=0 스플릿), L04, ip_multi_dup_x(중복 x 브래킷) |
| 상수 시리즈 (range=0) | L05, hf_constant, niceScale `degenerate_5_5`·`zero_zero` |
| NaN / Infinity | L06a(y), L06b(x), B02, N04(NaN 세그먼트), P07(NaN 페이스) |
| 음수·0 교차 | L07, niceScale `neg3_7`·`headroom_neg50_50` |
| 극단 범위 | L08(1e-9~1e12 혼재), niceScale `tiny_range`·`huge_0_1e12` |
| 다운샘플 임계 직전/직후 | P08a(5999→skip1) / P08b(6000→skip2). 부수 경계: P04/P05(유효 10/11 = MIN_VALID_PACE_COUNT), P06a/P06b(표본 19/20 = p95 컷 발동) |
| 실제 앱 대표 케이스 | L09/L10/L12 = iOS 스냅샷 픽스처(`ios-renderer/.../TestFixtures.swift:5-6`) 미러, P09 = 10km 러닝 600표본 합성(결측 소급·스파이크 포함) |

추가 커버: viewport(W01~W04), 배경 area 전용(BG01~BG05), OVERLAY 자체 정규화(L11),
시간모드 막대(B07/B08, endMinutes 20.5분 반올림 경계), 심박존 정확 경계 bpm(95/114/…/171),
스냅 동률(x=2.25), 히트테스트 경계/클램프, 라벨 솎기 마지막 배수 규칙(lt_44), 토글 전이 전체,
`SeriesSelection` 규칙 전체, 코어 공개 상수(`Y_AXIS_HEADROOM_FRACTION`, `PaceSeriesId`).

주의: 픽스처 생성에 sin/log 등 초월 함수를 쓰지 않았다 — JVM/Native ULP 차이가
"픽스처 차이"로 새는 것을 막기 위함(`HarnessFixtures.kt` 상단 주석).

## 실행 절차 (사용자)

```bash
cd /Users/daeho/lumipol-graph

# 1) JVM 덤프 → /tmp/lumipol-graph-dump/core-dump-jvm.json
./gradlew :core:jvmTest --tests "com.lumipol.graph.harness.*" --rerun

# 2) iOS 네이티브 덤프 → /tmp/lumipol-graph-dump/core-dump-iosSimulatorArm64.json
#    (시뮬레이터 이름이 다르면 -PiosSimDevice="<설치된 기기명>" 추가)
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
  ./gradlew :core:iosSimulatorArm64Test --tests "com.lumipol.graph.harness.*" --rerun

# 3) diff
python3 scripts/diff-core-dump.py \
  /tmp/lumipol-graph-dump/core-dump-jvm.json \
  /tmp/lumipol-graph-dump/core-dump-iosSimulatorArm64.json
```

- `--rerun`: 그래들이 UP-TO-DATE로 건너뛰지 않게 강제.
- diff 종료 코드: `0` 동등 / `1` 허용 오차 초과 존재.
- 출력의 **"드리프트"**(허용 오차 내 비트 불일치)는 실패가 아니라 JVM↔Native 부동소수
  차이의 실측 증거다 — 5단계 "Native 골든 테스트 필요" 근거로 그대로 인용한다.
- 로케일: 코어 덤프는 로케일 API를 쓰지 않는다(포맷팅은 아직 렌더러/앱에 있음).
  ko-KR 고정은 1단계에서 렌더러/앱 포맷터 덤프를 추가할 때 적용한다(05 문서 승인 사항).

## diff 판정 규칙 (scripts/diff-core-dump.py)

- 구조(키 존재·배열 길이)·문자열·정수·bool → **완전 일치(T1)**. 배열 길이 불일치는 "T1 위반"으로 표기
- 실수 → 절대오차 `max(1e-9, |값|×1e-9)` — 05 문서 승인값(큰 도메인 값 상대오차 환산 포함)
- NaN/Infinity는 문자열 토큰(`"NaN"` 등)으로 직렬화되므로 T1 비교에 걸린다

## 실측 diff 결과

_사용자 실행 결과 대기. 결과가 오면 여기에 (1) 동등/불일치 요약, (2) 드리프트 목록,
(3) 1단계 경계 지도 `실측 차이` 칸에 넘길 항목을 기록한다._

## 관측 후보 메모 (하네스 작성 중 코드에서 발견 — 판정은 1단계에서)

- **NaN 페이스가 필터를 통과한다 (추정: 결함).** `PaceSeriesEngine.preprocess`의 필터
  `core/src/commonMain/.../PaceSeriesEngine.kt:52` `if (pace <= 0.0 || pace < filterMin || pace >= filterMax) pace = 0.0`
  — NaN은 모든 비교가 false라 0 처리를 피해가고, 이후 `s.pace <= 0.0 || s.pace > slowCap`(75행)도
  통과해 valid 표본에 들어간다. P07 픽스처가 이 경로를 관측한다. 수정은 분석 완료 후 트랙에서.
- 도넛 `value=NaN` 세그먼트는 `it.value.value > 0.0` 필터(`DonutEngine.kt:10`)에 걸러진다 — 정상.

## 다음 세션 인계

1. 사용자 실행 결과(덤프 2개 + diff 출력)를 받아 위 "실측 diff 결과" 절 기록
2. S2 진입: 1단계 경계 지도 — 첫 세션에서 서브시스템 A~G 전체 기능 목록을 만들어
   **한 번에 승인**받은 뒤 서브시스템별 진행 (10-boundary-map.md 규약)
3. 렌더러 덤프는 S2에서 항목 식별될 때마다 이 하네스 형식(JSON 스키마·12자리 직렬화)을 재사용해 증분 추가
