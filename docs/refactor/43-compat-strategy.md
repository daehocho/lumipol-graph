# 43 — 호환성 전략 (4단계)

- 작성일: 2026-07-27 / 승인: 위임
- 사용 빈도 근거: raw 인벤토리 §1 집계 (iOS 6파일·AOS 9파일이 전 접촉면 — 파괴 반경이 작다).

## API별 전략 (호출부 변경 적은 순)

| 변경 | 사용 빈도 | 전략 |
|---|---|---|
| B1 도메인 출력, B2 ScrubResult, B5 colorAnchors, B10 매핑 출력 | 신규 필드/메서드 | **1 무변경** — 추가만. 기존 `nearest`는 유지 후 `@Deprecated(ReplaceWith("nearestScrub"))` |
| A1/A2/B8 코어 내부 | 호출부 무관 | **1 무변경** |
| B3 ZoomState 이관 | 렌더러 내부 타입(앱 미사용) | **1 무변경**(렌더러만 재배선) |
| B6 컬러맵, B7 ChartDefaults | 렌더러 내부 + 앱 provider | **3 오버로드 병행** — `barColorProvider` 유지 + `colorBlindMode` 신설, provider는 deprecated |
| B9 보조 생성자 | iOS 앱 7참조(BarChartData) | **1 무변경**(추가만) |
| B13 그림자 제거(iOS barColors 등) | 앱 미사용 확인됨 | **5 파괴적** 허용 — 미사용 공개 API는 제거. 마이그레이션 가이드에 한 줄 |
| C1 전처리 회수 | iOS `PaceSamplePoint` 8참조 등 | **4 어댑터 병행** — 기존 `PaceSeriesInput` 직접 조립 경로 유지, `TrackChartBuilder` 신설. 앱 전환 후 기존 경로 deprecated |
| C2 포맷팅 | 앱 유틸 교체 | **3~4** — SDK 신규 API, 앱 유틸은 앱 사정에 따라 잔류 가능(차트 경로만 전환) |
| C3 최대심박, C5 도넛 조립·invertedAxes·segmentCount | 앱 1~2곳씩 | **3 오버로드/신설** |
| 막대 layout 호출 주체 통일(B5 후속) | iOS·AOS 앱 각 1곳 | **2 별칭** — 렌더러에 `render(data:)` 신설, `render(layout:)`는 deprecated 유지 |

## 원칙

1. 트랙 C는 성격상 3~5가 정상(프롬프트) — 호환성을 이유로 회수를 축소하지 않는다. 대신 마이그레이션
   가이드를 `rd-charts-DESIGN.md` 7장 릴리스 항목에 남긴다(기존 규약).
2. 두 앱이 유일 소비자이고 정확 버전 고정(00 문서)이므로, deprecated 유지 기간은 "양 앱 전환 릴리스
   +1"로 짧게 잡는다.
3. ObjC 표면: Kotlin `@Deprecated`는 ObjC 헤더에 deprecated attribute로 내려간다 — iOS도 경고를 받는다.
4. 앱 커밋 규약: Runday_IOS는 대문자 접두사·Co-Authored-By 금지, Runday_AOS는 소문자 접두사
   (메모리 `lumipol-release-flow` 기록).

## 마이그레이션 대응표 (호출부)

| 기존 (앱) | 신규 |
|---|---|
| `RunPaceUtils.mphToPace`/`pace`/`seconds` + 수동 x 누적 (iOS) | `TrackChartBuilder.paceInput(samples, totals, options)` |
| `Util.getNowPaceV2` + `xValue` (AOS) | 동일 |
| `RunPaceUtils.stringPace…` / `FormatUtil.formatPace` | `ChartFormat.pace` |
| `stringHHMMSS` / `formatZoneTime` | `ChartFormat.duration` |
| `ageBasedMaxHeartRate` / `maxBpm` | `HeartRateZoneEngine.maxHeartRate(age, gender)` |
| 색 앵커 블록(양 앱) | `layout.colorAnchors` |
| `RDRouteColorizer.color(forPace:…)` / `PaceColorUtil.paceColor` (차트 경로) | `PaceColormap.rgba` / `style.colorBlindMode` |
| 24/40 샘플 색바 재구성 | `PaceColormap.legendStops` |
| `paceOnPrimary ? [.primary] : []` / 슬롯 0/1 분기 | `SeriesSelection.invertedAxesFor(paceSlot)` |
| `HeartRateZoneEngine.calculate` + 수동 DonutChartData 조립 | `HeartRateZoneEngine.donutData(…)` |
