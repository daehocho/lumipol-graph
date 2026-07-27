# 42 — 트랙 C: 앱 로직 회수 (4단계)

- 작성일: 2026-07-27 / 승인: 위임
- 목표: 앱에는 "DB 읽기 + 원천 레코드 전달 + 뷰 배치"만 남긴다. 앱 변경은 SDK 릴리스와 분리 커밋.
- 각 항목: 신규 SDK API 명세 + 앱 diff 규모(파일 단위) + 회수 후 잔여 확인.

## C1 — 원천 샘플 전처리 회수 (페이스 계산·x값·정렬·스플릿 델타)

**신규 API (코어)**:
```kotlin
/** 앱의 DB 행을 그대로 옮긴 원천 레코드 — 계산·해석 없음. 결측은 null. */
data class RawTrackSample(
    val cumulativeDistanceMeters: Double?, // AOS realDistance*1000 / iOS 누적(없으면 delta로)
    val deltaDistanceMeters: Double?,      // iOS point.distance
    val cumulativeSeconds: Double?,        // AOS realExerciseTime/1000
    val deltaSeconds: Double?,             // iOS timeInterval
    val speedMps: Double?,                 // 워치 속도(iOS useRundayWatch 경로)
    val latitude: Double?, val longitude: Double?, // Haversine 폴백용
    val heartRate: Double?, val cadence: Double?, val altitude: Double?,
)
object TrackChartBuilder {
    /** 원천 → PaceSeriesInput. 페이스 계산식·무효 게이트·x 산출·정렬을 코어가 단독 소유. */
    fun paceInput(samples: List<RawTrackSample>, totals: RunTotals, options: BuildOptions): PaceSeriesInput
    fun splitSamples(samples: List<RawTrackSample>): List<SplitSample>
    fun zoneSamples(samples: List<RawTrackSample>): List<HeartRateZoneSample>  // dt 재구성+폴백 규칙 포함
}
data class BuildOptions(val unit: DistanceUnit, val xMode: XMode /* DISTANCE|TIME */)
```
- 결측 센티널 해석(0/−100/250)도 `RawTrackSample` 생성 헬퍼로 흡수(양 앱 동일 규칙이므로 코어 기본).
- **기준식 결정 필요(44 문서)**: 워치 속도 우선(iOS) + Haversine 폴백(AOS) + 속도 게이트(AOS) 병합안 제시.
- 마일 상수는 코어 단일(`DistanceUnit.METERS_PER_MILE = 1609.344`, 역수 파생) — 3벌 병존 종료.
- **앱 diff 규모**: iOS `RDPaceChartDataBuilder.swift`(354줄) 대부분 삭제, `RDSplitChartDataBuilder.swift`(108줄) 축소,
  `RunPaceUtils` 차트 경로 미사용화. AOS `ChartSamples.kt`·`UnifiedChartDataBuilder.kt` 절반 삭제,
  `getNowPaceV2` 차트 의존 제거. 각 앱 3~4파일.

## C2 — 포맷팅 회수

**신규 API (코어)**:
```kotlin
object ChartFormat { // ko-KR 고정(05 승인), 로케일 API 미사용
    fun pace(seconds: Double): String          // 표기 결정 → 44 (4'30" vs 5'30'')
    fun paceInvalid(): String                  // 무효 표현 단일화
    fun duration(seconds: Double): String      // H:MM:SS / MM:SS
    fun percent(fraction: Double): String
    fun distanceTick(value: Double): String    // %g 동등 규칙(트레일링 0 제거)
    fun timeTick(minutes: Double): String      // "N:00", 0.1 이하 빈문자열
    fun intTick(value: Double): String         // 절삭 규칙 명문화
}
```
- AOS `normalizedStat` 사문 폴백은 `paceInvalid()` 단일 원본으로 자연 해소.
- 로케일 의존 예외 후보: 없음(양 앱 모두 로케일 비의존 표기를 의도) — 향후 다국어 시 `NumberFormat` 주입점만 설계.
- **앱 diff**: iOS `RunPaceUtils` 호출 ~8곳 교체(2파일), AOS `formatPace`/`formatZoneTime`/`formatDistanceTick` 교체(3파일).

## C3 — 최대심박 회수

```kotlin
object HeartRateZoneEngine { // 기존 object 확장
    fun maxHeartRate(age: Int, gender: Gender): Int  // Gender { MALE, FEMALE, UNKNOWN }
}
```
- 공식: 남 Fox `220−age`, 여 Gulati `206−0.88age` — 양 앱 일치. **UNKNOWN 처리 결정 → 44**
  (현재 iOS=여성 공식, AOS=남성 공식 — 정반대).
- **앱 diff**: iOS `RDHeartRateZoneCalculator.swift` 공식부 삭제(1파일), AOS `HeartRateZoneCalculator.kt` 공식부 삭제(1파일).

## C4 — 색 앵커·컬러맵 소비 전환 (B5/B6 후속)

- 앱 복사본 삭제: iOS `RDSplitChartView.swift:113-130` + `RDRouteColorizer` 차트 경로,
  AOS `SplitChartCard.kt:69-83` + `PaceColorUtil` 차트 경로.
- 색약 모드 플래그는 앱 설정 → `barColorProvider` 대신 `style.colorBlindMode: Boolean` 주입으로 단순화.
- **앱 diff**: 각 앱 2파일. 지도 경로(비차트)의 colorizer는 그대로 둔다(범위 밖).

## C5 — SDK API 공백(오버레이류) 처리

| 공백 | 결정 |
|---|---|
| 색바 범례(24샘플/40세그먼트 재구성) | **SDK가 데이터 API 제공** — `PaceColormap.legendStops(anchors, count, colorBlind): List<Long>` — 그리기는 앱 유지(뷰 배치), 색 산출만 코어. 두 앱의 샘플 수 차이는 44에서 통일 결정 |
| 라인/심박존 범례(값·선택 연동·폭 적응) | **확장 포인트 유지** — 범례는 앱 UX 영역으로 판정(카드 레이아웃과 결합). 단 범례에 필요한 데이터(`availableSeries`, 선택 상태, `ZoneBpmRange`, 스크럽 값)는 이미 SDK가 제공 — 추가 API 불필요. 폭 적응 알고리즘 통일은 비목표(플랫폼 레이아웃 시스템 상이) |
| 지표 칩 | 앱 잔류(제품 UI) |
| `invertedAxes` 산출 | 코어 헬퍼 `SeriesSelection.invertedAxesFor(paceSlot): Set<Axis>` — AOS 슬롯2+ 누락 버그 소거 |
| segmentCount 정책 | 코어 기본 `ChartConfig.segmentCountFor(totalUnits, xMode)` — iOS 고정 5 vs AOS 거리비례 → 44 결정 |
| 도넛 조립 | `HeartRateZoneEngine.donutData(zoneSeconds, labels): DonutChartData?` — 전존 0 → null 규칙 코어로 |

## 실행 기록

- [x] **SDK 신규 API(0.38.0, 2026-07-28)** — C1: `RawTrackSample(+sanitized 센티널 흡수)`·
  `BuildOptions(unit, xMode, useWatchSpeed)`·`RunTotals`·`TrackChartBuilder.paceInput/splitSamples/
  zoneSamples`(D2 병합식 — 워치 우선+Haversine 폴백(1e-6m 양자화, AOS ×1000 결함 소거)+1~41km/h
  게이트 공통+Double+내림 제거 / D11 dt 폴백). 마일 상수 단일화 `DistanceUnit.METERS_PER_MILE=
  1609.344`(역수 파생 — iOS 1.609344000000865·0.621371 불일치 종료, iOS 마일 워치 페이스 결함도
  단일식으로 소거). C2: `ChartFormat`(D1 — pace `4'30"`+99분 상한, paceInvalid, duration, percent,
  distanceTick(%g 동등), timeTick, intTick). C3: `HeartRateZoneEngine.maxHeartRate(age, gender)` +
  `Gender`(D3 — UNKNOWN=여성 공식). C5: `donutData(zoneSeconds, labels)`(전존 0→null)·
  `SeriesSelection.invertedAxesFor(paceSlot)`(AOS 슬롯2+ 누락 버그 소거)·
  `ChartConfig.segmentCountFor(D4 — 거리 비례·상한 120·시간 0)`. `PaceColormap.legendStops`는
  B6(0.34.0)에서 선행. 골든 3섹션 추가(폴백 trig 드리프트 0 실측).
- [ ] 앱 전환(C1~C5 소비부) — 후속 커밋

## 회수 후 앱 잔여 확인 기준

각 앱 차트 디렉토리에 남아야 하는 것: DB 조회, `RawTrackSample` 변환(필드 복사), 카드 레이아웃,
칩/범례 배치, 문자열 리소스 조달, 스타일 색 주입. **계산·포맷·색 산출이 남아 있으면 미완료.**
검증: 렌더러 금지 패턴 검사(50 문서)를 앱 차트 디렉토리에도 적용한 grep 체크리스트.
