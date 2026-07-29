# 11 — 앱 인벤토리 (1단계 별첨)

- 작성일: 2026-07-27
- 원자료(전체 인용): `raw/ios-app-inventory.md`(464줄), `raw/aos-app-inventory.md`(773줄)
- 양 앱 모두 SDK 접촉면이 좁다: iOS 프로덕션 6파일(`ChartComponents/`), AOS 9파일(`chart/lumipol/`) —
  회수 작업의 diff 범위가 명확하다.

## 1. 앱이 SDK 호출 **전에** 데이터에 가하는 변형 (전수)

| # | 변형 | iOS | AOS | 동일? |
|---|---|---|---|---|
| 1 | 원본 정렬 | `sortedTrackPointsA2` — exerciseTime 승순, `exerciseTime==0` 포인트는 created 최신 1개만 | 없음 — DB 순서 신뢰 | **다름** |
| 2 | 첫 포인트 처리 | 전 포인트 사용 | 라인: `location[0]` 소비(페이스 prev), 도넛·스플릿: 포함 | **다름** |
| 3 | 결측 센티널→null (hr>0, alt>−100, cad>0 & ≤250) | `RDPaceChartDataBuilder.swift:53-63` | `ChartSamples.kt:31-45` | 동일(값·규칙) |
| 4 | **2점 페이스 계산** | 워치: `mphToPace(speed m/s)` / GPS: `pace(distance,time)` → `seconds(withPace:)`+`floorToDecimal(2)` 내림 | `getNowPaceV2`: 누적거리 델타≤0이면 **Haversine 폴백**, 속도 게이트 `1<km/h<41` 밖은 0, `16.666666666666998/(m/s)`, **float 중간연산** | **다름 — 화면 차이 직접 원인** |
| 5 | 워치/GPS 분기 | `useRundayWatch==1`로 명시 분기 | 분기 불가(동일 Location 테이블) — Haversine 폴백이 사실상 대응 | **다름(구조)** |
| 6 | x값(거리) | 델타 재누적 `abs(d×unit)/1000` | 누적 `realDistance`(×0.621371 마일) | **다름** |
| 7 | x값(시간) | `timeSum/60` (저장 timeInterval 누적) | `realExerciseTime/1000/60` (누적 직독) | **다름(원천)** |
| 8 | 단위 환산 상수 | `MileToKilometer=1.609344000000865`(페이스) / `KilometerToMile=0.621371`(x) / `metersPerMile=1609.344`(막대) — 3벌 | `MILE_PER_KM=1.609344000000865`(**이름이 값과 반대**) / `KM_PER_MILE=0.621371` / `METERS_PER_MILE=1609.344` — 3벌 | 값 세트는 동일, **양 앱 모두 내부 3벌 병존**. 0.621371×1.609344≠1 (역수 아님) |
| 9 | 평균 페이스 | `RunPaceUtils` 경로(요약 행) | `calcAvgPace` — `Int` 나눗셈 ms 절삭 | 유사(정밀도 다름) |
| 10 | 최고 페이스 되돌림 | `stringPace` 직행 | `bestPaceSeconds/60/unitFactor` → 항상 분/km 정규화 | **다름(경로)** |
| 11 | HR존 dt | `exerciseTime` 델타 + 전부0이면 `timeInterval` 폴백 (2021.01 이전 기록 대응) | `realExerciseTime` 델타 단일, 첫 샘플 dt=누적 전체 | **다름 — 구 기록에서 갈림** |
| 12 | HR존 심박 결측 표현 | `?? 0` (0=미측정 계약 의존) | `heartBeatCount` 그대로(0 포함) | 동일(계약상) — 단 iOS 라인차트(nil)와 카드 간 불일치 |
| 13 | 스플릿 델타 | per-point 델타 직사용 + `>0` 필터 | 누적→델타 재구성 + 첫 구간 특례 + `>0` 필터 | **다름(원천), 필터 동일** |
| 14 | 최대심박 | 남 `220−age`, 여 `206−0.88age`, **비MALE 전부(미설정 포함) 여성 공식**, Int 절삭 | 여(`gender==0`) Gulati, **그 외(-1 미설정 포함) 남성 공식**, Double→Int 절삭 | **다름 — 성별 불명 처리가 정반대** |
| 15 | 필터·결측 승계·다운샘플·가용성 | 코어 위임 | 코어 위임 | 동일(위임) |

## 2. 앱이 만드는 라벨·툴팁·범례 문자열

| # | 문자열 | iOS | AOS | 동일? |
|---|---|---|---|---|
| 1 | **페이스** | `4'30"` (`%d'%02d\"`), 무효 `-'--"`, 절삭, 음수 가드 없음 | `5'30''`(작은따옴표 2개), 무효 `-'--''`, 절삭, 상한 99분 | **다름 — 표기 자체가 다름** |
| 2 | 무효→`--` 정규화 | 문자열 비교 `== "-'--\""` — 작동 | 동일 리터럴 비교인데 실출력과 불일치 → **폴백 사문화** (`UnifiedChartCard.kt:180`) | **다름(AOS 버그)** |
| 3 | x축 시간 | `"\(Int(v)):00"`, `v<=0.1` 빈문자열 | `"${v.toInt()}:00"`, 동일 게이트 | 동일 |
| 4 | x축 거리 | `%g` (로케일 비의존) | `%g` 재현 + trim (`%g` 로케일 의존 잠재) | 유사(로케일 노출면 AOS만) |
| 5 | y 정수(심박·케이던스·고도) | `Int(v)` 절삭 | `v.toInt()` 절삭 | 동일 |
| 6 | 고도 스크럽 | `"\(Int(v.rounded()))m"` | `"${v.roundToInt()}m"` | 동일 |
| 7 | 존 시간 | `stringHHMMSS` | `formatZoneTime` — 주석 "iOS 동일" | 동일 |
| 8 | bpm 범위 | id 6632/6633 + 수동 replace | 동일 id + 동일 방식 | 동일 |
| 9 | 존 이름·제목·칩 | StringTable 34/142/5091-5099/6611/6627-6633 | 동일 id 세트(원격 테이블 공유) | 동일 |
| 10 | 막대 x축 | 거리: `1...count` 인덱스 / 시간: `endMinutes` | 거리: `index+1` / 시간: `endMinutes ?: index+1` | 동일(폴백만 AOS 추가) |

> **10행 현행화(2026-07-29)**: 양 앱 모두 0.42.0~0.43.0에서 `endDistanceMeters`/`endSeconds` +
> `splitEndDistance`/`splitEndTime` + `xAxisUnitLabel`로 마이그레이션 완료 — `index+1`/`endMinutes`
> 직표기는 null 폴백으로만 남아 있다(iOS `RDSplitChartView.swift`, AOS `SplitChartCard.kt`).
> 이 행을 현행 관용구로 인용하지 말 것.

## 3. 앱이 SDK 위에/옆에 그리는 것

z축 오버레이는 양 앱 모두 없음(무데이터 라벨 센터 겹침 제외). 전부 형제 뷰지만, **SDK가 범례를
제공하지 않아 생긴 자체 구현**이 많고 SDK와 논리적으로 결합돼 있다:

| # | 항목 | iOS | AOS | 동일? |
|---|---|---|---|---|
| 1 | 지표 칩 행 | UIKit 버튼 스택 | Compose FlowRow | 유사(모양), 구현 별개 |
| 2 | 평균/최고 요약 | 2열 스택 | StatColumn×2 | 유사 |
| 3 | 라인 범례 | 색점+이름+값, 빈 UIView 패딩으로 피치 고정 | **3단 폭 적응**(gap 12→8dp, 폰트 12→9sp) + TextMeasurer 실측 | **다름(적응 알고리즘)** |
| 4 | **스플릿 색바 범례** | `GradientBarView` — SDK 색 공식 **24회 샘플링** 재구성 | `PaceColorLegendBar` — Canvas **40세그먼트** 자체 구현(색약 이산 보장) | **다름(샘플 수·방식)** — 둘 다 SDK 색 로직 의존 복제 |
| 5 | 심박존 범례 | 5행, Z5→Z1 역순, `selectSegment(at:)` 양방향 결합, 도넛 160→120 축소+bpm 제거 적응 | 5행 역순, `selection.toggle` 공유, 도넛 크기·bpm 폭 적응(실측) — **다른 알고리즘** | **다름(적응 로직)** |
| 6 | 무데이터 문구 | noDataLabel 센터 | 동일 패턴 | 동일 |

→ **SDK API 공백 목록**: (a) 범례(색·이름·값·선택 연동), (b) 페이스 색바(그라데이션/이산),
(c) 막대 색 앵커 공개, (d) 지표 칩(선택 UI)은 앱 고유 UX로 잔류 타당. 42-track-c에서 명세.

## 4. 앱이 SDK에 넘기는 하드코딩 값

| # | 값 | iOS | AOS | 동일? |
|---|---|---|---|---|
| 1 | `ChartConfig` | `(segmentCount: 5, maxTicks: 5)` | `(segmentCount: floor(총거리단위).coerceIn(0,120), maxTicks: 5)` — 시간모드 0 | **다름! iOS는 항상 5, AOS는 거리 비례(상한 120)** |
| 2 | `toleranceSecPerUnit` | 10.0 (사문화 — target 항상 0→nil) | 기본값(미지정) | 사실상 동일(경로 미실행) |
| 3 | `targetPaceSecPerUnit` | 항상 0→nil (**기능 사문화**) | 명시 null | 동일(비활성) |
| 4 | 지표 고정색 | pace `7B7BFF`·hr `FF789B`·cad GraphGreen·alt GraphOrange(α0.22) | 동일 값 (`LumipolChartStyle.kt:8-18` — iOS 대응 주석) | 동일 |
| 5 | `gradientMaxAlpha` | 0.25 명시(기본값과 동일하나 고정 의도) | 미지정(기본값) | 실효 동일 |
| 6 | `barPartialOpacity/partialBarAlpha` | 1.0 (기본 0.6 덮어씀) | 1f | 동일 |
| 7 | 도넛 존 색 | WarmGrey/GraphBlue/Green/Orange/Red | 동일 세트 | 동일 |
| 8 | `donutRingWidth` | 28 × (도넛크기/160) 비례 축소 | 동일 공식 | 동일 |
| 9 | `invertedAxes` | `paceOnPrimary ? [.primary] : []` | 슬롯 0→PRIMARY, 1→SECONDARY, **2+ 누락** | **다름(경계 케이스)** |
| 10 | `isZoomEnabled` | true | true | 동일 |
| 11 | 기본 지표 선택 | `[.pace, .heartRate, .altitude]` | 동일 (`DEFAULT_METRICS`) | 동일 |
| 12 | 선택 영속 | (미확인 — 저장 로직 검출 안 됨) | SharedPreferences enum명 CSV + 미가용 보존 래퍼 | **다름(AOS만 영속·보존)** — iOS 확인 필요 |
| 13 | 다크 테마 | 동적 UIColor(시스템 추종) | `darkTheme=false` 고정 + 카드 배경 White | **다름(다크 지원 여부)** |
| 14 | 차트 높이 | 섹션 고정 460/300/220pt | 320/240dp 적응 + 스플릿 180dp | **다름** |

## 5. 결론 — 회수 대상 판정

**"다른 항목이 화면 차이의 직접 원인이다"** 기준으로:

- **1순위 (같은 데이터 → 다른 숫자)**: §1-4 페이스 계산, §1-6/7 x값, §1-11 HR존 dt, §1-14 최대심박.
  → 트랙 C: 코어에 "원천 샘플 → 페이스/x" 전처리 API 신설 + 최대심박 공식 API.
- **2순위 (같은 숫자 → 다른 표기)**: §2-1 페이스 문자열, §2-2 사문 폴백.
  → 트랙 C: 코어 포맷팅 API (05 문서 승인대로 ko-KR 고정, 로케일 의존부 예외 표시).
- **3순위 (같은 표기 → 다른 배치·색)**: §3-4 색바, §3-3/5 범례 적응, §4-1 segmentCount, §4-9 invertedAxes.
  → 트랙 B/C: 색 앵커·컬러맵 코어화, invertedAxes 규칙 코어화(slotAxis처럼), segmentCount 정책 통일.
- 이미 동일해서 회수 이득이 낮은 것(§1-3 센티널, §2-3~10, §4-4~8, §4-11)은 후순위로 코어 기본값화만 검토.
