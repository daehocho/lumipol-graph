# Runday_AOS — lumipol-graph SDK 사용 인벤토리

> 조사 대상: `/Users/daeho/Runday_AOS` (읽기 전용)
> 조사 일자: 2026-07-27
> SDK 버전: **0.29.0**

SDK 버전 고정 위치:
- `Day_RunDayProject/gradle/watchlibs.versions.toml:85` — `mobile-lumipol = "0.29.0"`
- `Day_RunDayProject/gradle/watchlibs.versions.toml:259` — `mobile-lumipol-renderer = { group = "com.github.daehocho.lumipol-graph", name = "renderer", version.ref = "mobile-lumipol" }`
- `Day_RunDayProject/Rundayfree/build.gradle.kts:410` — `implementation(libs.mobile.lumipol.renderer) // 기록 상세 그래프 (lumipol-graph SDK, core 전이 포함)`

SDK를 import하는 파일은 **`.../record/analysis/chart/lumipol/` 디렉토리 9개 파일 + 테스트 4개가 전부**입니다. 앱 다른 어디에서도 `com.lumipol.graph`를 참조하지 않습니다. 진입점은 Java Fragment 1곳:

- `.../analysis/view/fragment/ResultGraphFragment.java:155` — `AnalysisChartsBinder.show(composeCharts, exercise, locationList, mileUse, userAge, userGender);`
- `.../analysis/view/fragment/ResultGraphFragment.java:119` — `AnalysisChartsBinder.setTimeMode(isTime);` (거리/시간 라디오 토글)
- 계단오르기 플랜은 SDK 차트를 아예 숨깁니다 — `ResultGraphFragment.java:147-149`: `if (RundayUtil.isStepUpPlan(...)) { llChartTab.setVisibility(View.GONE); composeCharts.setVisibility(View.GONE); ...`

파일 경로 접두사(이하 생략): `/Users/daeho/Runday_AOS/Day_RunDayProject/Rundayfree/src/main/java/com/hanbit/rundayfree/ui/app/record/analysis/chart/lumipol/`

소스 파일 규모:

```
242 AnalysisChartsBinder.kt
 49 ChartMetricPreference.kt
 62 ChartSamples.kt
 41 HeartRateZoneCalculator.kt
195 HeartRateZoneCard.kt
 49 LumipolChartStyle.kt
210 SplitChartCard.kt
388 UnifiedChartCard.kt
206 UnifiedChartDataBuilder.kt
---
1485 total (main)
```

테스트: `src/test/.../chart/lumipol/` — `ChartMetricPreferenceTest.kt`(51), `ChartMetricSelectionTest.kt`(100), `SplitAndZoneBuilderTest.kt`(70), `UnifiedChartDataBuilderTest.kt`(135).

---

## 1. SDK 공개 API 사용 빈도

### 1.1 함수/엔진 (호출부 전량)

| SDK 심볼 | 호출 위치 | 인용 |
|---|---|---|
| `PaceSeriesEngine.preprocess` | `UnifiedChartDataBuilder.kt:58` | `PaceSeriesEngine.preprocess(toEngineInput(samples, runningSeconds, sumDistanceMeters, mileUse, isTimeMode))` |
| `SeriesSelection.normalized` | `UnifiedChartDataBuilder.kt:68` | `SeriesSelection.normalized(current = ..., available = ..., priority = DISPLAY_PRIORITY)` |
| `SeriesSelection.toggled` | `UnifiedChartDataBuilder.kt:95` | `val visible = SeriesSelection.toggled(current = displayed.map { it.selectionId }, toggling = toggling.selectionId)` |
| `SeriesSelection.assignSlots` | `UnifiedChartDataBuilder.kt:144` | `SeriesSelection.assignSlots(priority = PaceSeriesId.LINE_PRIORITY, selected = ..., withData = result.availableSeries)` |
| `SeriesSelection.slotAxis` | `UnifiedChartDataBuilder.kt:161` | `axis = SeriesSelection.slotAxis(slot),` |
| `HeartRateZoneEngine.calculate` | `HeartRateZoneCalculator.kt:31` | `val zoneSeconds = HeartRateZoneEngine.calculate(zoneSamples, maxBpm)` |
| `HeartRateZoneEngine.zoneBpmRanges` | `HeartRateZoneCalculator.kt:40` | `fun zoneRanges(maxBpm: Int): List<ZoneBpmRange> = HeartRateZoneEngine.zoneBpmRanges(maxBpm)` |
| `BarChartEngine.chooseTimeBucketSeconds` | `SplitChartDataBuilder.kt:38` | `splitTimeSeconds = if (isTimeMode) BarChartEngine.chooseTimeBucketSeconds(runningSeconds) else null,` |
| `BarChartEngine.layout` | `SplitChartCard.kt:53` | `val layout = remember(state.barData) { BarChartEngine.layout(state.barData) }` |
| `ChartStyle.defaults` | `LumipolChartStyle.kt:21` | `val rundayChartStyle: ChartStyle = ChartStyle.defaults(darkTheme = false).copy(` |

### 1.2 컴포저블 (3종, 각 1곳)

| 컴포저블 | 호출 위치 | 전달 파라미터 |
|---|---|---|
| `RDLineChart` | `UnifiedChartCard.kt:160-172` | `data, modifier, style, invertedAxes, backgroundArea, labelFormatter, isZoomEnabled, onScrub, onScrubBackground, onScrubEnd` |
| `RDBarChart` | `SplitChartCard.kt:129-139` | `layout, modifier, style, barLabels, xAxisLabels, yLabelFormatter` |
| `RDHeartRateZoneChart` | `HeartRateZoneCard.kt:87-95` | `data, modifier, style, onSelectSegment, selection` |
| `rememberDonutSelectionState` | `HeartRateZoneCard.kt:65` | `val selection = rememberDonutSelectionState(donutData)` |

### 1.3 상수/모델 타입

- `PaceSeriesId.PACE/HEART/CADENCE/ALTITUDE` — `UnifiedChartDataBuilder.kt:22-25`, enum `ChartMetric`의 `selectionId`로 직접 채택. `UnifiedChartDataBuilder.kt:18-19` 주석: *"selectionId는 코어가 고정한 PaceSeriesId — … 앱이 임의로 매기면 안 된다"*
- `PaceSeriesId.DISPLAY_PRIORITY` — `UnifiedChartDataBuilder.kt:37`
- `PaceSeriesId.LINE_PRIORITY` — `UnifiedChartDataBuilder.kt:145`
- 모델 타입 18개: `PaceSamplePoint`, `PaceSeriesInput`, `PaceSeriesResult`, `LineChartData`, `ChartConfig`, `Series`, `SeriesRole`, `Point`, `Axis`, `ChartAxis`, `BarChartData`, `SplitSample`, `DonutChartData`, `DonutSegment`, `DonutColorRole`, `HeartRateZoneSample`, `ZoneBpmRange`, `ChartStyle`
- `DonutColorRole`이 최다 참조(11회): `LumipolChartStyle.kt:23-28`(팔레트 5개 키), `HeartRateZoneCalculator.kt:35`(`DonutColorRole.entries[i]`), `HeartRateZoneCard.kt:102`(범례색 조회).

### 1.4 참조 횟수 집계 (전 파일 합산, import 포함)

```
DonutColorRole  11    PaceSeriesId     9    BarChartData     8    DonutChartData   8
SeriesSelection  7    PaceSeriesResult 7    HeartRateZoneSample 5  ChartAxis       5
ChartStyle       5    BarChartEngine   4    RDBarChart       4    ZoneBpmRange    4
SplitSample      4    PaceSeriesEngine 3    HeartRateZoneEngine 3  LineChartData   3
PaceSamplePoint  3    Axis             3    RDLineChart      2    RDHeartRateZoneChart 2
rememberDonutSelectionState 2  ChartConfig 2  PaceSeriesInput 2  Point 2  Series 2
SeriesRole       2    DonutSegment     2
```

---

## 2. SDK 호출 전 데이터 가공 전부

### 2.1 원본 → `ChartSample` (`ChartSamples.kt:31-45`)

입력은 SQLite `Location` 행 리스트(`ResultGraphFragment.java:179` — `dbManager.getAllLocationExerciseID(exerciseId)`).

```kotlin
if (locations.size < 2) return emptyList()
return (1 until locations.size).map { i ->
    val prev = locations[i - 1]; val cur = locations[i]
    ChartSample(
        distanceKm = cur.realDistance,
        elapsedSeconds = cur.realExerciseTime / 1000.0,
        paceMinPerKm = Util.GoogleMapUtil.getNowPaceV2(prev, cur),
        heartRate = cur.heartBeatCount.toDouble().takeIf { it > 0.0 },
        cadence = cur.cadence.toDouble().takeIf { it > 0.0 }?.coerceAtMost(MAX_CADENCE),
        altitude = cur.altitude.takeIf { it > INVALID_ALTITUDE },
    )
}
```

- **인덱스 1부터** — `location[0]`은 소비되어 라인 차트 샘플에서 빠집니다(첫 페이스 델타의 prev 역할).
- 결측 센티널 해석 (`ChartSamples.kt:21-23`): `INVALID_ALTITUDE = -100.0`, `MAX_CADENCE = 250.0`, 심박·케이던스 `0`은 결측(null).
  - 케이던스는 **결측 처리 + 상한 클램프** 두 가지. 고도는 `> -100.0` 만족해야 실측.
- 결측 승계·시리즈 가용성 판정은 앱이 하지 않습니다 — `ChartSamples.kt:28-29` 주석: *"결측 승계와 지표 가용성 판정은 코어(PaceSeriesEngine)가 단독으로 소유한다"*

`ChartSample` 정의 (`ChartSamples.kt:11-18`):

```kotlin
data class ChartSample(
    val distanceKm: Double,     // 누적 거리(km, realDistance)
    val elapsedSeconds: Double, // 누적 운동 시간(초, realExerciseTime/1000)
    val paceMinPerKm: Double,   // 인접 샘플 간 페이스(분/km). 무효면 0
    val heartRate: Double?,     // bpm, null = 미측정
    val cadence: Double?,       // spm, null = 결측
    val altitude: Double?,      // m, null = 미측정
)
```

DB 스키마 (`common/db/table/Location.java`): `altitude`(double, :16), `realExerciseTime`(long, :29), `realDistance`(double, :33), `heartBeatCount`(int, :41), `cadence`(int, :45).

### 2.2 페이스 계산식 `getNowPaceV2` (`common/util/Util.java:1281-1319`)

2점 페이스는 **앱 소유 로직**이고, 기존 구 그래프와 동일한 함수를 그대로 씁니다.

```java
float distanceGap = (float) now.getRealDistance() - (before == null ? 0 : (float) before.getRealDistance());
if (distanceGap <= 0f) {
    distanceGap = getDistance(before.getLattitude(), before.getLongitude(), now.getLattitude(), now.getLongitude());
}
distanceGap = (distanceGap * 1000f);
float timeGap = (now.getRealExerciseTime() - before.getRealExerciseTime()) / 1000f;
```

```java
if (distanceGap > 0 && timeGap > 0) {
    float speedkmh = ((float) distanceGap / timeGap) * 3.6f;
    if ((speedkmh > 1 && speedkmh < 41)) {
        return LocationInfo.KM_PER_MIN_VALUE / (distanceGap / (double) timeGap);
    } else { return 0; }
} else { return 0; }
```

수식으로:

- `dm` = `realDistance` 델타(km) × 1000. **델타가 0 이하면 Haversine(`android.location.Location.distanceBetween`) 직선거리(m)로 대체** — 워치 업로드 기록처럼 누적거리가 계단식일 때 이 폴백이 발동합니다.
- `dt` = `realExerciseTime` 델타 / 1000 (초, 일시정지 제외 시간)
- 속도 게이트: `1 < (dm/dt)*3.6 < 41` km/h 밖이면 **페이스 = 0(무효)**. 즉 걷기보다 느리거나 41km/h 초과 구간은 무효 페이스로 떨어집니다.
- 페이스(분/km) = `16.666666666666998 / (dm/dt)` — `common/service/gps/LocationInfo.java:36`: `public static final double KM_PER_MIN_VALUE = 16.666666666666998D;`
  - 정확히는 `(1000/60) / (m/s)` = `min/km`. 상수가 `1000/60 = 16.6666…`의 근사값이라 iOS가 `1000.0/60.0`을 쓴다면 **~1e-13 상대 오차**가 있습니다(무해하지만 비트 단위 파리티는 아님).
- `float` 중간 연산이 섞여 있습니다(`distanceGap`, `timeGap`, `speedkmh` 모두 `float`) — iOS가 `Double`로만 계산한다면 게이트 경계값에서 판정이 갈릴 수 있습니다.

### 2.3 `PaceSamplePoint` 조립 (`UnifiedChartDataBuilder.kt:186-205`)

```kotlin
private fun unitFactor(mileUse: Boolean) = if (mileUse) LocationUtil.MILE_PER_KM else 1.0

private fun xValue(s: ChartSample, mileUse: Boolean, isTimeMode: Boolean): Double =
    if (isTimeMode) s.elapsedSeconds / 60.0
    else if (mileUse) s.distanceKm * LocationUtil.KM_PER_MILE else s.distanceKm
```

```kotlin
PaceSamplePoint(
    x = xValue(it, mileUse, isTimeMode),
    paceSeconds = it.paceMinPerKm * 60.0 * unitFactor(mileUse),
    heartRate = it.heartRate, cadence = it.cadence, altitude = it.altitude,
)
```

**x 축 값**

- 시간 모드: `x = elapsedSeconds / 60` → **분**
- 거리 모드(km): `x = distanceKm`
- 거리 모드(mile): `x = distanceKm × 0.621371`

**paceSeconds**

- km: `paceMinPerKm × 60` → 초/km
- mile: `paceMinPerKm × 60 × 1.609344000000865` → 초/mile

**⚠️ 상수 이름이 값과 반대입니다** — `common/util/LocationUtil.java:21-22`:

```java
public static final double MILE_PER_KM = 1.609344000000865;
public static final double KM_PER_MILE = 0.621371;
```

`MILE_PER_KM`이 실제로는 "km per mile"이고 `KM_PER_MILE`이 "mile per km"입니다. 값 자체는 모든 사용처에서 차원이 맞게 쓰이고 있어 버그는 아니지만, iOS 코드와 대조할 때 이름만 보고 매칭하면 반드시 틀립니다.

**워치/GPS 분기: 차트 경로에는 존재하지 않습니다.**

`ChartSampleMapper`는 `Location` 테이블만 보고, 그 테이블에 워치 기록과 GPS 기록이 동일 스키마로 들어옵니다. 워치 기록은 `.../other/setting/watch/WatchExerciseUploadUtil.java:130-146`에서 같은 필드를 채웁니다:

```java
location.setRealExerciseTime(durationTime);
location.setRealDistance(distance);
location.setHeartBeatCount(heartRateBpm);
location.setCadence(cadence);
```

실시간 GPS 경로는 `.../exercise/view/run/activity/BaseRunningActivity.java:3025-3027`(`cLocation.setHeartBeatCount(heartBeatCount); … cLocation.setCadence(cadence);`), 서버 동기화 경로는 `common/util/NetworkUtil.java:805-806`. 따라서 **차트 빌더 레벨에서 소스 구분은 불가능**하고, 유일한 실질적 분기는 §2.2의 `distanceGap <= 0` Haversine 폴백입니다.

### 2.4 평균/최고 페이스 (`AnalysisChartsBinder.kt:85-87`, `UnifiedChartDataBuilder.kt:178`)

```kotlin
val runningSeconds = exercise.runningTime / 1000.0
val sumDistanceMeters = exercise.distance * 1000.0
val avgPace = calcAvgPace(exercise)
```

`common/util/Util.kt:1024-1031`:

```kotlin
fun calcAvgPace(exercise: Exercise): Double {
    val meter = exercise.distance * 1000
    val time = exercise.runningTime / 1000
    return if (meter > 0) { LocationInfo.KM_PER_MIN_VALUE / (meter / time) } else { 0.0 }
}
```

평균 페이스는 샘플이 아니라 **`Exercise` 요약 행**에서 나옵니다(분/km). `time`이 `Int` 나눗셈이라 밀리초 절삭이 있습니다.

최고 페이스는 코어 산출값을 되돌립니다 — `UnifiedChartDataBuilder.kt:178`:

```kotlin
bestPaceMinPerKm = result.bestPaceSeconds / 60.0 / unitFactor(mileUse),
```

즉 `sec/표시단위 → min/표시단위 → min/km`. 마일 사용자에게도 항상 분/km로 정규화해서 포매터에 넘깁니다.

### 2.5 정렬·샘플링·필터

- **정렬 없음.** DB 반환 순서를 그대로 신뢰합니다.
- **샘플링·다운샘플 없음.** 모든 `Location` 행이 1:1로 `PaceSamplePoint`가 됩니다.
- 유효 페이스 개수 하한은 코어가 소유합니다 — 테스트 `UnifiedChartDataBuilderTest.kt:70` ``fun `유효_페이스가_10개_이하면_페이스_시리즈를_제외한다`()``.
- 앱이 하는 필터는 스플릿 델타 정리 1곳뿐(§6.3).

### 2.6 스레딩 (`AnalysisChartsBinder.kt:96-136`)

3단계 파이프라인입니다.

1. `Dispatchers.IO` — 샘플 매핑 + 거리모드 `preprocess` + 도넛 + 총거리 (`:96-114`, `LaunchedEffect(exercise.id)`)
2. `Dispatchers.Default` — 모드 의존 `preprocess` + 스플릿 (`:122-136`, `LaunchedEffect(data, isTime)`). 거리 모드는 1단계 산출물 재사용: `pace = if (isTime) UnifiedChartDataBuilder.preprocess(...) else loaded.distancePre`
3. 메인 — `assemble` (`:148-155`, 칩 토글 시 이 단계만 재실행)

`AnalysisChartsBinder.kt:116-118`에 Compose 함정 주석이 있습니다: *"Composable 람다에서 early return(return@setContent) 금지 — recomposition마다 방출 그룹 수가 달라져 슬롯 테이블 그룹 불균형(IntStack.peek2)으로 크래시. 조건부 방출은 if로."*

### 2.7 지표 선택 정규화·영속

- 사용자 의도와 화면 표시가 분리되어 있습니다 (`UnifiedChartDataBuilder.kt:89-101`). `toggleSelection`은 코어 `toggled` 결과에 **가용하지 않아 칩이 숨겨진 선택을 뒤에 다시 붙입니다**:

```kotlin
val visible = SeriesSelection.toggled(current = displayed.map { it.selectionId }, toggling = toggling.selectionId).map(ChartMetric::of)
if (visible == displayed) return userSelection // 무시된 탭 — 화면 무변화면 의도도 무변화
return visible + userSelection.filter { it !in available }
```

  근거 주석(`:78-87`): *"이 기록에 데이터가 없어 칩이 숨겨진 선택은 뒤에 그대로 남긴다. 의도를 화면 결과로 덮어쓰면 심박 미측정 기록에서 케이던스를 한 번 누른 것만으로 심박수 선택이 영구히 사라진다"*, *"코어가 탭을 무시했으면(마지막 칩 해제 시도) 의도를 그대로 반환한다"*

- 저장은 **enum 이름 CSV** — `ChartMetricPreference.kt:40`: `internal fun encode(metrics: List<ChartMetric>): String = metrics.joinToString(",") { it.name }`. `ChartMetricPreference.kt:10-13` 주석이 이유를 명시: *"selectionId(코어 PaceSeriesId) 정수를 쓰지 않는 이유는 그 번호를 코어가 소유하기 때문 — SDK가 번호를 재배치하면 저장된 값이 조용히 다른 지표로 해석된다. enum 이름은 앱이 소유하므로 안전하다."*
- 손상·구버전 토큰 방어 (`ChartMetricPreference.kt:43-48`): 모르는 토큰 폐기 → `distinct()` → 비면 `DEFAULT_METRICS`.
- 저장 위치는 `MintyPreferenceManager.SETTING_PREF` + `R.string.setting_analysis_chart_metrics` 키. 로그아웃 시 지워지는 것이 의도 (`:17-18` 주석).
- 기본값 — `UnifiedChartDataBuilder.kt:33`: `val DEFAULT_METRICS = listOf(ChartMetric.PACE, ChartMetric.HEART, ChartMetric.ALTITUDE)` (*"iOS defaultSelection [.pace, .heartRate, .altitude] 동일"*)
- 폴백 우선순위는 **고도 포함** `DISPLAY_PRIORITY`, 슬롯 배정은 **고도 제외** `LINE_PRIORITY` (`:64-72`, `:143-148`).

---

## 3. 앱이 만드는 문자열

문자열은 전부 원격 스트링 테이블 기반입니다 — `common/util/ResourceUtil.kt:86-96`:

```kotlin
fun Context.getStringFromNum(stringNum: Int): String {
    return try {
        val remote = RemoteStringTable.get(this, stringNum)
        if (!remote.isNullOrEmpty()) return remote
        applicationContext.getStringIdFromTable(stringNum)
    } catch (e: Exception) { ...; getString(R.string.empty_string) }
}
```

즉 **차트 라벨은 하드코딩 한글이 아니라 서버가 바꿀 수 있는 값**입니다.

### 3.1 주입되는 문자열 ID 전량 (`AnalysisChartsBinder.kt:162-235`)

| 용도 | ID | 위치 |
|---|---|---|
| 평균 페이스 | 34 | `:162` |
| 최고 페이스 | 5099 | `:164` |
| 무데이터(통합 카드) | 5091 | `:167` |
| 페이스 / 심박수 / 고도 / 케이던스 칩 | 142 / 5093 / 5092 / 5094 | `:169-172` |
| 스플릿 카드 제목 | 142 (페이스 재사용) | `:207` — 주석: *"iOS 동일 — 전용 '구간별 페이스' 문자열 없음"* |
| 심박존 카드 제목 | 6611 | `:218` |
| 존 이름 Z1→Z5 | 6631, 6630, 6629, 6628, 6627 | `:41` — `private val ZONE_NAME_RES_IDS = intArrayOf(6631, 6630, 6629, 6628, 6627)` |
| bpm 범위 템플릿 (상한 없음 / 있음) | 6632 / 6633 | `:228-232` |
| 심박존 무데이터 | 6635 | `:235` |

bpm 범위 조립 (`AnalysisChartsBinder.kt:224-233`):

```kotlin
val rangeText =
    if (range.upper == null) context.getStringFromNum(6632).replace("{0}", range.lower.toString())
    else context.getStringFromNum(6633).replace("{0}", range.lower.toString()).replace("{1}", range.upper.toString())
"$name $rangeText"
```

`{0}`/`{1}` 치환은 `String.format`이 아니라 수동 `replace` — 로케일 API를 타지 않습니다.

### 3.2 페이스 포맷 — `formatPace` (`staticlib/.../com/ttam/staticlib/FormatUtil.kt:150-188`)

```kotlin
value = paceMinute * 60f          // min -> s
if (mileUse) value = value / Distance.UNIT_KM_TO_MILE   // 0.621371 → sec/mile
if (abs(value) > 0 && abs(value) < 60 * 99) {
    val values: IntArray = getPaceSecToMinSec(value)
    return min.toString() + "'" + String.format("%02d", sec) + "''"
} else { return "-'--''" }
```

- 입력은 **항상 분/km**. 마일 변환은 포매터 내부에서 합니다.
- 초는 **반올림이 아니라 절삭** — `FormatUtil.kt:260-267`:

```kotlin
val min = (absPace / 60.0).toInt()
var temp = (absPace - min * 60).toInt().toDouble()
temp = (Math.round(temp * 100) / 100.0)
val sec = temp.toInt()
```

  `.toInt()`가 먼저 걸려 `Math.round`는 실질 무효과입니다. 5'30.9" → `5'30''`.
- 출력 형태는 **`5'30''`(작은따옴표 2개)**, 유효 범위 밖은 **`-'--''`**. 상한은 99분/단위.
- `%02d`가 로케일 인자 없는 `String.format` — 파일에 `@Suppress("DefaultLocale")`이 붙어 있습니다(`FormatUtil.kt:149`). 아라비아-인디크 숫자 로케일에서 자릿수가 바뀔 수 있는 알려진 노출면입니다.
- 마일 상수는 staticlib 소유 — `staticlib/.../unit/Distance.kt:39`: `const val UNIT_KM_TO_MILE = 0.621371`

**⚠️ 문자열 리터럴 불일치 (실질 버그 후보)** — `UnifiedChartCard.kt:179-180`:

```kotlin
private fun normalizedStat(value: String): String =
    if (value.isEmpty() || value == "0" || value == "-'--\"") EMPTY_STAT else value
```

비교 대상이 `-'--"`(큰따옴표)인데 `formatPace`가 실제로 내보내는 무효값은 `-'--''`(작은따옴표 2개)입니다. **이 분기는 절대 매치되지 않으므로**, 페이스 무효 기록에서 `--`가 아니라 `-'--''`가 그대로 노출됩니다. `hasPace` 게이트(`UnifiedChartCard.kt:108`)가 대부분의 경우를 먼저 가려내기 때문에 드러나지 않고 있을 뿐입니다. 주석에 *"iOS normalized() 동일"*이라고 적혀 있는데, iOS의 무효 페이스 문자열이 `-'--"`라면 **양 플랫폼의 페이스 포맷 자체가 다르다**는 뜻이 됩니다 — iOS 조사 결과와 반드시 교차 확인이 필요합니다.

### 3.3 축 포매터 주입 — `RDLineChart.labelFormatter` (`UnifiedChartCard.kt:317-335`)

```kotlin
private fun formatAxisValue(axis: ChartAxis, value: Double, state: UnifiedChartUiState, paceSlot: Int?): String {
    val paceAxis = when (paceSlot) { 0 -> ChartAxis.Y_PRIMARY; 1 -> ChartAxis.Y_SECONDARY; else -> null }
    return when (axis) {
        ChartAxis.X ->
            if (state.isTimeMode) { if (value <= 0.1) "" else "${value.toInt()}:00" }
            else formatDistanceTick(value)
        paceAxis -> {
            val minPerKm = if (state.mileUse) value / LocationUtil.MILE_PER_KM else value
            formatPace(state.mileUse, false, minPerKm)
        }
        else -> "${value.toInt()}"
    }
}
```

- X축 시간 모드: `"${분}:00"`, 단 `value <= 0.1`이면 **빈 문자열**(0분 라벨 숨김).
- X축 거리 모드 — `UnifiedChartCard.kt:333-335`:

```kotlin
private fun formatDistanceTick(value: Double): String =
    if (value == floor(value)) "${value.toInt()}"
    else "%g".format(value).let { if ('.' in it) it.trimEnd('0').trimEnd('.') else it }
```

  주석: *"iOS String(format: \"%g\")처럼 정수는 소수점 없이, 그 외엔 불필요한 0 제거"*. `%g`는 로케일 의존 소수점 구분자를 낼 수 있고 `'.' in it` 검사는 `,` 로케일에서 통과하지 못합니다(유럽 로케일 잠재 이슈).
- 페이스 축: 시리즈 y가 분/표시단위이므로 **분/km로 되돌려서** `formatPace`에 넘깁니다. 심박·케이던스·기타 축은 `value.toInt()` 정수.
- 고도 스크럽 값 — `UnifiedChartCard.kt:170`: `onScrubBackground = { value -> scrubAltitude = "${value.roundToInt()}m" }` (반올림 + `m` 접미사 하드코딩, 미터 고정 — 마일 사용자도 미터).

### 3.4 스플릿 페이스 포맷 (`SplitChartCard.kt:206-210`)

```kotlin
private fun formatSplitPace(secPerUnit: Double, mileUse: Boolean): String {
    val perUnitMinutes = secPerUnit / 60.0
    val minPerKm = if (mileUse) perUnitMinutes / LocationUtil.MILE_PER_KM else perUnitMinutes
    return formatPace(mileUse, false, minPerKm)
}
```

막대 라벨·Y축·색바 범례 양단 라벨이 모두 이 함수를 씁니다.

### 3.5 존 시간 포맷 (`HeartRateZoneCard.kt:187-195`)

```kotlin
if (seconds <= 0.0 || seconds.isNaN() || seconds.isInfinite()) return "00:00"
val total = seconds.toInt(); val hour = total / 3600
val min = (total - hour * 3600) / 60; val sec = total % 60
return if (hour > 0) String.format("%d:%02d:%02d", hour, min, sec) else String.format("%02d:%02d", min, sec)
```

주석: *"iOS RunPaceUtils.stringHHMMSS 동일 — 0/무효 \"00:00\", 1시간 미만 \"MM:SS\", 이상 \"H:MM:SS\""*. 초 절삭, 로케일 미지정 `String.format`.

---

## 4. SDK 위에 덧그리는 것

앱이 직접 그리는 것이 상당량 있습니다. 세 카드 모두 **SDK 컴포저블은 카드 안의 플롯 영역 하나뿐**이고 칩·요약·범례·색바는 전부 앱 소유입니다.

### 4.1 통합 시계열 카드 (`UnifiedChartCard.kt`)

- **지표 칩 행** (`:75-96`) — `FlowRow` + `BasicText` 4개. `metricsWithData`에 있는 지표만 노출: `ChartMetric.entries.filter { it in state.result.metricsWithData }`. `softWrap = false, maxLines = 1`로 글자 단위 줄바꿈 차단 (`:90` 주석: *"글자 단위 줄바꿈(\"케이던스\"→\"케이던/스\") 금지"*).
- **평균/최고 페이스 요약 2등분** (`:109-120`) — `StatColumn` × 2, `Modifier.weight(1f)`. `hasPace` 게이트 근거 (`:106-107`): *"페이스가 지표에서 빠진 기록에서 칩만 숨기고 요약은 숫자를 그대로 두면 같은 카드 안에서 \"페이스 데이터 없음 + 평균 5'30\"\" 모순이 보인다"*
- **커스텀 범례 + 폭 적응 알고리즘** (`:122-150`, `:191-314`) — SDK 범례를 안 쓰고 직접 그립니다. 슬롯 0~2 순서로 라인 지표, 그 뒤 고도. 스크럽 중 값 표시.
  - 폭 적응 3단계 (`:227-269`, `rememberLegendMetrics`): ① 기본 12sp/gap 12dp → ② gap 8dp → ③ 폰트 9sp까지 축소 → 그래도 안 되면 `FlowRow` 줄바꿈.
  - 값 칸 폭은 `rememberTextMeasurer(cacheSize = 32)`로 **실측**하고, 지표별로 다르게 두지 않고 **최댓값 하나로 통일**(`:248` `fun slotPx(size: TextUnit) = entries.maxOfOrNull { valuePx(it, size) } ?: 0`). 근거(`:222-225`): *"지표마다 폭이 다르면 값이 비었을 때 이름 뒤 여백이 항목마다 달라져 간격이 들쭉날쭉해 보인다"*
  - 값이 없을 때도 칸 폭을 유지 — `:148-149` 주석: *"스크럽을 시작할 때 항목이 좌우로 밀리거나, 줄바꿈이 생기며 아래 차트가 손가락 밑에서 통째로 내려가는 걸 막는다"*
- **차트 높이 적응** (`:159-163`) — `BoxWithConstraints`로 부모 제약을 보고 `if (maxWidth >= WideWindowWidth) 320.dp else 240.dp`. 근거(`:156-158`): *"창 크기(LocalConfiguration)가 아니라 부모 제약을 쓰므로 분할 화면이나 좁은 컬럼에 얹혀도 맞는다"*
- **무데이터 문구** (`:98-103`) — 라인도 없고 배경 실루엣도 없을 때만. *"고도만 선택돼 라인이 없어도 배경 실루엣이 있으면 차트를 그린다(iOS 동일)"*
- **프리뷰 5종** (`:380-388`) — 폰 393dp / 폴드 커버 320dp / 분할 화면 240dp / 큰 글꼴 1.5배 / 폴드 내부 674dp.

### 4.2 스플릿 카드 (`SplitChartCard.kt`)

- **제목 + 페이스 색바 범례 헤더** (`:117-128`).
- **`PaceColorLegendBar`** (`:155-203`) — SDK를 전혀 쓰지 않는 `Canvas` 자체 구현. 40개 `drawRect` 세그먼트. `:149-153` 주석에 설계 근거: *"보간 없는 solid 세그먼트 방식은 색약 이산 보장을 위해 반드시 유지한다"* — 색약 모드에서 `PaceColorUtil.paceColor`가 이산 4색만 반환하므로 세그먼트가 자연히 계단식이 됩니다. `size = Size(segmentWidth + 1f, ...)`로 이음매 제거.
- **⚠️ 라이브러리 private 규칙의 복사본** (`:69-83`) — 범례 색 앵커를 앱이 직접 재계산합니다:

```kotlin
val fullValues = layout.bars.filter { !it.isPartial }.map { it.value }
val fullHasRange = fullValues.size >= 2 && fullValues.max() > fullValues.min()
val anchorValues = if (fullHasRange) fullValues else layout.bars.map { it.value }
Triple(anchorValues.min(), anchorValues.max(), anchorValues.average())
```

  `:73-77` 주석이 위험을 명시: *"이 블록은 라이브러리 내부(private) 규칙의 **복사본**이다. lumipol-graph는 이 규칙을 이미 두 번 바꿨고(cff647e 부분 스플릿 제외, 84dec88 전체 폴백), 어긋나도 컴파일 에러가 나지 않는다 — 어긋나면 범례색과 실제 막대색이 조용히 달라진다. **mobile-lumipol 버전을 올릴 때마다** android-renderer/RDBarChart.kt의 앵커 계산부와 반드시 대조할 것. 근본 해결: 라이브러리가 앵커(fastest/slowest/average)를 공개 API로 노출하고 양쪽이 그걸 소비 (후속 과제)."*
  **SDK 측 개선 요구사항으로 최우선 후보입니다.**
- **색약보정 설정 lifecycle 재읽기** (`:59-67`) — `ON_RESUME`마다 `readMapColorFix`. 이유는 `:56-58` 주석: ComposeView가 `DisposeOnViewTreeLifecycleDestroyed`라 컴포지션이 onPause/onResume을 넘어 살아남기 때문.

### 4.3 심박존 카드 (`HeartRateZoneCard.kt`)

- **제목** (`:53`), **무데이터 문구** (`:55-60`).
- **도넛 우측 범례 5행** (`:97-119`) — 전부 앱 구현. **Z5→Z1 역순**: `donutData.segments.indices.reversed().forEach { i ->`. 주석: *"iOS rebuildLegend 동일. 시간은 우측 정렬 세미볼드."*
- **도넛 크기·bpm 표시 폭 적응** (`:62-81`) — `rememberRequiredLegendWidth`(`:167-184`)로 가장 긴 범례 행을 `rememberTextMeasurer`로 실측한 뒤:

```kotlin
val available = maxWidth - DonutLegendGap
val idealDonut = available - requiredLegendWidth
val donutSize = idealDonut.coerceIn(MinDonutSize, DesiredDonutSize)
val showBpm = idealDonut >= MinDonutSize
val ratio = donutSize.value / DesiredDonutSize.value
val style = remember(ratio) { rundayChartStyle.copy(donutRingWidth = rundayChartStyle.donutRingWidth * ratio) }
val maxRowWidth = DesiredDonutSize + DonutLegendGap + requiredLegendWidth
```

  폭이 부족하면 라벨을 `zoneLabels`(이름+bpm) → `zoneNames`(이름만)로 강등 (`:98`).
  범례 행 기하 실측식 (`:179`): `LegendDotSize + LegendRowSpacing * 3 + nameW + valueW`
- **범례 ↔ 도넛 선택 공유 + 햅틱** (`:65`, `:108-116`) — `rememberDonutSelectionState`를 공유하고, 범례 클릭 시 `selection.toggle(i)`. 햅틱은 앱이 직접 발생시킵니다:

```kotlin
if (after != null && after != before && rundayChartStyle.donutSelectionHapticsEnabled) {
    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
}
```

  주석: *"햅틱은 선택 확정·이동에만 — 해제와 무시된 인덱스(0초 존)에는 울리지 않는다(iOS 패리티)"*. `RDHeartRateZoneChart`의 `onSelectSegment = {}`는 no-op — `:92-93` 주석: *"0.26.0: onSelectSegment가 null이면 라이브러리가 터치 자체를 비활성화한다 — 탭 토글(센터 라벨·디밍·자동해제·햅틱)만 쓰고 별도 선택 상태는 필요 없어 no-op"* (라이브러리 API 함정).
- **범례 디밍** (`:106`, `:140`) — `dimmed = selection.selectedIndex != null && !selected`, `LegendDimAlpha = 0.45f`. `:44-45` 주석: *"도넛 디밍(0.3)보다 약하게 — 텍스트 명암비 때문(3초 후 원복되는 일시 상태)"*.

### 4.4 카드 컨테이너 (`AnalysisChartsBinder.kt:158-238`)

`Column`에 세 카드 + `Spacer(Modifier.height(7.dp))` 두 개. 카드 배경/패딩은 각 카드가 소유(`Modifier.fillMaxWidth().background(Color.White).padding(16.dp)`).

---

## 5. 하드코딩 스타일/설정 값

### 5.1 `ChartStyle` 주입 — 3종 파생 (`LumipolChartStyle.kt`)

기본 스타일 (`:21-29`), `darkTheme = false` **고정**:

```kotlin
val rundayChartStyle: ChartStyle = ChartStyle.defaults(darkTheme = false).copy(
    donutColors = mapOf(
        DonutColorRole.ZONE1 to StatTitleColor,    // 0xFF757575 Color_WarmGreyTwo
        DonutColorRole.ZONE2 to Color(0xFF5AC8FA), // Color_GraphBlue
        DonutColorRole.ZONE3 to CadenceColor,      // 0xFF04DE71 Color_GraphGreen
        DonutColorRole.ZONE4 to AltitudeColor,     // 0xFFFF8100 Color_GraphOrange
        DonutColorRole.ZONE5 to Color(0xFFFA114F), // Color_GraphRed
    ),
)
```

라인 차트용 (`:42-49`):

```kotlin
fun unifiedChartStyle(): ChartStyle = rundayChartStyle.copy(
    seriesColors = mapOf("pace" to PaceColor, "heart" to HeartColor, "cadence" to CadenceColor),
    areaFillColor = AltitudeColor.copy(alpha = 0.22f),
)
```

`:39-41` 주석에 `seriesColors`를 쓰는 이유: *"0.29.0부터 심박·케이던스가 보조축을 공유하므로 슬롯색으로는 두 라인을 구분할 수 없다 — seriesColors(슬롯색보다 우선, 라인·그라데이션·터치 도트 공용 리졸버)에 지표 고정색을 직접 매핑한다"*

막대 차트용 (`SplitChartCard.kt:97-113`):

```kotlin
rundayChartStyle.copy(
    partialBarAlpha = 1f,
    barColorProvider = { input ->
        Color(PaceColorUtil.paceColor(
            avgPace = effectiveAvgPace, bestPace = input.fastest,
            worstPace = input.slowest, pace = input.value, isColorFix = isColorFix))
    },
)
```

`:99` 주석: *"부분 스플릿도 barColorProvider가 반환한 색과 동일 농도로 표시(iOS 함정 #2 — 0.6 흐림이면 범례색과 안 맞음)"*

도넛용 (`HeartRateZoneCard.kt:77-79`): `donutRingWidth = rundayChartStyle.donutRingWidth * ratio`.

### 5.2 색 리터럴 (`LumipolChartStyle.kt:8-18`)

| 상수 | 값 | iOS 대응(주석) |
|---|---|---|
| `PaceColor` | `0xFF7B7BFF` | graphOutlinePurple |
| `HeartColor` | `0xFFFF789B` | graphOutlineRed |
| `CadenceColor` | `0xFF04DE71` | Color_GraphGreen |
| `AltitudeColor` | `0xFFFF8100` | Color_GraphOrange |
| `LegendNameColor` | `0xFF373737` | Color_GreyishBrown |
| `StatTitleColor` | `0xFF757575` | Color_WarmGreyTwo |
| `ValueTextColor` | `0xFF212121` | Color_BlackTwo |
| `ChipSelectedColor` | `0xFF7460D9` | darkPeriwinkle |
| `ChipUnselectedColor` | `0xFF9A93BF` | wisteria |

존 색 추가 2개: Z2 `0xFF5AC8FA`(Color_GraphBlue), Z5 `0xFFFA114F`(Color_GraphRed).
색약 모드 팔레트는 `common/util/PaceColorUtil.kt:20-30` — `rgb(0,0,255)`, `rgb(0,158,115)`, `rgb(255,255,0)`, `rgb(213,94,0)`.

지표 색은 슬롯이 아닌 **지표 기준 고정** (`LumipolChartStyle.kt:31-37`): *"슬롯이 아닌 지표 기준이라 선택 조합이 바뀌어도 각 지표 색이 유지된다(iOS 동일)"*

### 5.3 SDK에 넘기는 설정값

| 값 | 위치 | 인용 |
|---|---|---|
| `maxTicks = 5` | `UnifiedChartDataBuilder.kt:173` | `config = ChartConfig(segmentCount = segmentCount, maxTicks = 5)` |
| `segmentCount` | `UnifiedChartDataBuilder.kt:168` | `if (isTimeMode) 0 else floor(totalDistanceUnits).toInt().coerceIn(0, 120)` — 상한 **120**. `:166-167` 주석: *"km 정수 지점 마커는 표시하지 않는다(플롯 상단 라벨·세로선 제거). 구간 통계(segmentCount)는 스크럽/랩 값에 쓰이므로 그대로 유지."* |
| `invertedAxes` | `UnifiedChartCard.kt:151-155, 165` | 페이스 슬롯이 0이면 `Axis.PRIMARY`, 1이면 `Axis.SECONDARY`. 슬롯 2 이상이면 **반전 없음** (버그 가능성 — 0.29.0에서 슬롯 1 이후가 전부 보조축이므로 페이스가 슬롯 2에 배정되면 반전이 누락됩니다. `LINE_PRIORITY`에서 페이스가 최우선이라면 실현되지 않지만, 코어 우선순위 변경에 취약) |
| `isZoomEnabled = true` | `UnifiedChartCard.kt:168` | 무조건 활성 |
| `role = SeriesRole.MAIN` | `UnifiedChartDataBuilder.kt:162` | 전 시리즈 동일 |
| `axis = SeriesSelection.slotAxis(slot)` | `UnifiedChartDataBuilder.kt:161` | 매핑은 코어 소유. `:159-160` 주석: *"0.29.0 슬롯 규약: 0=주축, 1 이후=전부 보조축(도메인 공유) — 케이던스가 축 없는 오버레이 대신 심박과 우측 축 눈금을 공유한다."* |
| `targetPaceSecPerUnit = null` | `SplitChartDataBuilder.kt:37` | `:35-36` 주석: *"iOS 분석 화면은 targetPaceSecPerUnit:0(목표 없음)으로 호출 — 기준선 = 런 평균 페이스. 목표 페이스를 넘기면 코어가 축 스케일(ys)에 ref를 포함해 y틱 구간까지 iOS와 달라진다."* |
| `splitDistanceMeters` | `SplitChartDataBuilder.kt:34` | `if (mileUse) 1609.344 else 1000.0` — `:8-9` 로컬 상수 `METERS_PER_KM = 1000.0`, `METERS_PER_MILE = 1609.344`. **`LocationUtil.MILE_PER_KM`(1.609344000000865)와 값이 다릅니다** — 앱 안에 마일 상수가 3벌(1609.344, 1.609344000000865, 0.621371) |
| `partialBarAlpha = 1f` | `SplitChartCard.kt:100` | |
| `darkTheme = false` | `LumipolChartStyle.kt:21` | 다크 테마 미지원 |

### 5.4 앱 측 치수·레이아웃 리터럴

`UnifiedChartCard.kt:47-64`:

```kotlin
private const val EMPTY_STAT = "--"
private val ItemGap = 12.dp; private val TightItemGap = 8.dp; private val LineGap = 4.dp
private val LegendDotSize = 8.dp; private val LegendDotGap = 4.dp; private val LegendValueGap = 4.dp
private val LegendFontSize = 12.sp; private val LegendMinFontSize = 9.sp
private val WideWindowWidth = 600.dp
```

- 폰트: 칩 `14.sp`(`:86`), StatColumn 제목 `12.sp`/값 `16.sp`(`:185-187`), 무데이터 `16.sp`(`:102`), 범례 이름/값 12→9sp 가변.
- 차트 높이 `320.dp`/`240.dp`(`:163`), 카드 패딩 `16.dp`(`:71`), 요약 top `12.dp`(`:109`), 범례 vertical `8.dp`(`:150`).
- 축소 여유 계수 `0.98f`(`:261`) — *"반올림 오차로 아슬아슬하게 넘치지 않도록 2% 여유를 둔다"*.
- 값 칸 템플릿 (`:203-208`): PACE `"00'00\""`, HEART/CADENCE `"000"`, ALTITUDE `"000m"`. PACE 주석: *"걷기 구간은 10분대가 흔해 분 자리를 2칸 잡는다"*
  **⚠️ PACE 템플릿이 실제 출력과 불일치**: 템플릿은 `00'00"`(6글자)인데 실출력은 `00'00''`(7글자, §3.2). `valuePx`가 `maxOf(템플릿, 실제값)`을 취하므로(`:242-245`) 스크럽 중에는 자동 보정되지만, **값이 비었을 때 예약 폭이 실제보다 좁게 잡힙니다** — 스크럽 시작 시 미세한 레이아웃 점프가 남습니다(이 코드가 막으려던 바로 그 현상).

`SplitChartCard.kt`: `LEGEND_SEGMENT_COUNT = 40`(`:144`), 색바 `120.dp × 9.dp`, `RoundedCornerShape(4.5.dp)`(`:186-188`), 라벨 `11.sp`(`:182, 201`), 제목 `19.sp` Medium(`:118`), 차트 높이 `180.dp` + top `12.dp`(`:131`).

`HeartRateZoneCard.kt:38-48`:

```kotlin
private val DesiredDonutSize = 160.dp; private val MinDonutSize = 120.dp; private val DonutLegendGap = 16.dp
private val LegendDotSize = 8.dp; private val LegendRowSpacing = 6.dp
private const val LegendDimAlpha = 0.45f
private val LegendRowMinHeight = 26.dp
```

`:46-48` 주석에 알려진 접근성 제약 명시: *"범례 행 최소 높이. 13sp 텍스트 실높이로는 터치 타겟이 좁다. 5행이 도넛(160dp) 안에 들어오는 선에서 키운 값 — Material 권장 48dp에는 못 미치는 알려진 제약."*
제목 `19.sp` Medium(`:53`), 범례 텍스트 `13.sp`(`:150, 158`), 무데이터 `16.sp`(`:59`).

**폰트 패밀리 주입 없음** — 모든 `TextStyle`이 `fontSize`/`fontWeight`/`color`만 지정하고 `fontFamily`는 시스템 기본입니다. `ChartStyle`에도 폰트를 넣지 않습니다.
**인셋/패딩도 SDK에 넘기지 않습니다** — 전부 Compose `Modifier` 레벨.

---

## 6. 심박존 도넛 / 스플릿 막대

### 6.1 최대심박 계산 — 앱 소유 (`HeartRateZoneCalculator.kt:16-20`)

```kotlin
fun maxBpm(userAge: Int, userGender: Int): Int {
    if (userAge <= 0) return 0
    val maxHR = if (userGender == 0) 206.0 - 0.88 * userAge else 220.0 - userAge
    return if (maxHR > 0) maxHR.toInt() else 0
}
```

- 여성(`gender == 0`): **Gulati 공식 `206 − 0.88 × 나이`**
- 남성(그 외): **Fox 공식 `220 − 나이`**
- `Double` 연산 — `:14` 주석: *"Float 절삭 오차로 iOS와 ±1 어긋남 방지"*
- 결과는 `.toInt()` **절삭**(반올림 아님). 나이 무효 시 0 → 존 카드 무데이터. `:15` 주석: *"나이 무효(생일 미입력 -1 등)면 0 — iOS(birthday nil → maxHR 0)와 동일하게 존 카드 무데이터 처리"*

나이·성별 출처는 `.../analysis/view/activity/AnalysisActivity.java:168-187`:

```java
private int getGender(){
    try { assert user != null; return user.isMale()?1:0; }
    catch (Exception e) { e.printStackTrace(); }
    return -1;
}
```

```java
/** 만나이. 생일 미입력·파싱 실패 시 -1 — iOS(birthday nil)와 동일하게 심박존 무데이터 처리. */
private int getAge() {
    try {
        assert user != null;
        if (user.getsBirthDay().isEmpty()) return -1;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return UtilKt.calculateAge(dateFormat.parse(user.getsBirthDay()));
    } catch (Exception e) { e.printStackTrace(); }
    return -1;
}
```

전달 경로: `AnalysisActivity.java:152` `ResultGraphFragment.newInstance(exerciseId, avgPace, getAge(), getGender())` → Bundle(`ResultGraphFragment.java:45-64`) → `AnalysisChartsBinder.show(..., userAge, userGender)` → `AnalysisChartsBinder.kt:88` `val maxBpm = HeartRateZoneCalculator.maxBpm(userAge, userGender)`.

**⚠️ 주의**: `getGender()`의 실패 경로가 `-1`을 반환하는데, `maxBpm`은 `userGender == 0`만 여성으로 보므로 **`-1`은 남성 공식**으로 처리됩니다. `ResultGraphFragment.java:63`도 기본값을 `-1`로 읽습니다(`getArguments().getInt(ARG_PARAM_USER_GENDER, -1)`) — 성별 불명 시 남성 공식이 적용되는 암묵 동작입니다.

존 bpm 경계는 **코어 소유** — `HeartRateZoneCalculator.kt:40` `HeartRateZoneEngine.zoneBpmRanges(maxBpm)`. 앱은 `ZoneBpmRange.lower/upper`만 읽어 문자열로 만듭니다(§3.1).

### 6.2 도넛 데이터 가공

집계 입력 (`ChartSamples.kt:53-61`) — **라인 차트와 다르게 `location[0]`을 포함**합니다:

```kotlin
var prevSeconds = 0.0
return locations.map { loc ->
    val t = loc.realExerciseTime / 1000.0
    val dt = t - prevSeconds
    prevSeconds = t
    HeartRateZoneSample(heartRate = loc.heartBeatCount.toDouble(), timeInterval = dt)
}
```

`:48-51` 주석: *"존 계산은 이전 점이 불필요해 location[0]도 포함(iOS trackpoint 집계와 동일). dt = realExerciseTime 델타(운동시간 기준, 일시정지 제외) — iOS timeInterval(realTime 델타)과 같은 의미이며, 각 포인트의 자기 구간이 자기 심박의 존에 귀속된다."*

- `prevSeconds = 0.0` 시드이므로 **첫 샘플의 dt = 자기 누적시간 전체**(시작~첫 기록 구간).
- **여기서는 심박 0을 null로 바꾸지 않습니다** — `heartBeatCount.toDouble()` 그대로. 센티널 해석을 코어에 맡깁니다. 테스트가 이를 명문화: `UnifiedChartDataBuilderTest.kt:98` ``fun `심박_0은_결측이_아니라_실측값이다`()``

도넛 조립 (`HeartRateZoneCalculator.kt:26-38`):

```kotlin
val zoneSeconds = HeartRateZoneEngine.calculate(zoneSamples, maxBpm)
if (zoneSeconds.all { it <= 0.0 }) return null
return DonutChartData(zoneSeconds.mapIndexed { i, sec ->
    DonutSegment(sec, DonutColorRole.entries[i], label = zoneNames.getOrNull(i))
})
```

- **필터·정렬·샘플링 없음.** 단위 환산 없음(초 그대로).
- 전 존 0이면 `null` → 카드가 무데이터 문구로 대체(`HeartRateZoneCard.kt:55`).
- `label`은 센터 라벨용 존 이름(0.26.0). `AnalysisChartsBinder.kt:109`에서 `ZONE_NAME_RES_IDS.map { context.getStringFromNum(it) }`로 주입.
- 세그먼트 순서 = `DonutColorRole.entries` 순서 = Z1→Z5. 범례만 역순 렌더(§4.3).

### 6.3 스플릿 막대 데이터 가공 (`SplitChartDataBuilder.kt:12-42`)

```kotlin
if (samples.size < 2) return null
val first = samples.first()
val splits = buildList {
    add(SplitSample(distanceMeters = first.distanceKm * METERS_PER_KM, timeSeconds = first.elapsedSeconds))
    addAll(samples.zipWithNext { a, b ->
        SplitSample(distanceMeters = (b.distanceKm - a.distanceKm) * METERS_PER_KM,
                    timeSeconds = b.elapsedSeconds - a.elapsedSeconds)
    })
}.filter { it.distanceMeters > 0 && it.timeSeconds > 0 }
```

- **첫 샘플의 누적값을 시작 구간으로 별도 추가** — `:20-22` 주석: *"시작점~첫 샘플 구간 포함 — 매퍼가 location[0]을 소비하므로 첫 샘플의 누적값이 시작 구간이다(도넛 빌더의 prevElapsed=0 시드와 동일 기준; 누락 시 총거리가 첫 구간만큼 부족해 경계가 어긋난다)"*
- 거리는 **km→m** 환산(×1000), 시간은 초 그대로.
- **무효 델타 제거**: `distanceMeters > 0 && timeSeconds > 0`. `:31` 주석: *"iOS RDSplitChartDataBuilder와 동일한 무효 델타 제거"*
- 시간 모드 버킷은 코어 위임: `splitTimeSeconds = if (isTimeMode) BarChartEngine.chooseTimeBucketSeconds(runningSeconds) else null`
- `totalDurationSeconds = runningSeconds`, `totalDistanceMeters = sumDistanceMeters` (둘 다 `Exercise` 요약 행 기반, §2.4)

### 6.4 스플릿 평균 페이스 계산 (`AnalysisChartsBinder.kt:199-203`)

```kotlin
val totalDistanceMeters = splitData.totalDistanceMeters
val avgPaceSecPerUnit =
    if (totalDistanceMeters != null && totalDistanceMeters > 0 && splitData.splitDistanceMeters > 0)
        (splitData.totalDurationSeconds ?: 0.0) / (totalDistanceMeters / splitData.splitDistanceMeters)
    else 0.0
```

= `총시간(초) / 스플릿 개수` = **초/표시단위**. `:197-198` 주석: *"지도 calcAvgPace와 동일 의미(total 기반)의 split-단위 평균 — bar.value(sec/unit)와 단위 일치. splitDistanceMeters는 barData 자체 필드에서 파생(하드코딩 복제 제거)."*

이 값은 카드에서 다시 방어됩니다 (`SplitChartCard.kt:90-93`):

```kotlin
val effectiveAvgPace = remember(state.avgPaceSecPerUnit, bestPace, worstPace, anchorMean) {
    (if (state.avgPaceSecPerUnit > 0.0) state.avgPaceSecPerUnit else anchorMean).coerceIn(bestPace, worstPace)
}
```

`:86-89` 주석에 근거: *"총합 avg는 exercise 전체 기반이라 스플릿 min/max([bestPace, worstPace]) 밖으로 나갈 수 있고, 그러면 greenPoint/yellowPoint가 전부 막대 범위 밖이 되어 색 구간이 붕괴한다(code-review #2/#3). 폴백 0.0도 중립이 아니라 greenPoint>yellowPoint로 분기를 역전시켜 전 막대가 빨강이 되므로 별도 처리한다."*

### 6.5 막대 색 공식 — 앱 소유 (`common/util/PaceColorUtil.kt:7-59`)

```kotlin
val greenPoint = avgPace - (avgPace - bestPace) * 0.7
val yellowPoint = avgPace + (worstPace - avgPace) * 0.25
val nowPace = pace.coerceIn(bestPace, worstPace)
```

- 일반 모드 — 3구간 선형 보간:
  - `nowPace < greenPoint`: `t = (greenPoint − nowPace)/(greenPoint − bestPace)`, `rgb(0, 255 − 255·t·0.4, 255)` (청록→하늘)
  - `nowPace < yellowPoint`: `t = (yellowPoint − nowPace)/(yellowPoint − greenPoint)`, `rgb(255 − 255·t, 255, 0)` (초록→노랑)
  - else: `t = (worstPace − nowPace)/(worstPace − yellowPoint)`, `rgb(255, 255·t, 0)` (노랑→빨강)
  - 각 분모 0 방어: `if (X - Y == 0.0) 0.0 else ...`
- 색약 모드 — 이산 4색: `bluePoint = bestPace + (greenPoint − bestPace)*0.2` 기준으로 `rgb(0,0,255)` / `rgb(0,158,115)`, 그 뒤 `rgb(255,255,0)` / `rgb(213,94,0)`
- 색약 설정 키: `SplitChartCard.kt:47-49` — `MintyPreferenceManager.SETTING_PREF` + `R.string.setting_map_color_fix`, 기본 `false`. *"지도(DrawOnMapTask.create)와 동일한 키로 읽는다"*

막대 라벨·축 (`SplitChartCard.kt:134-138`):

```kotlin
barLabels = layout.bars.map { formatSplitPace(it.value, state.mileUse) },
xAxisLabels = layout.bars.map { bar ->
    if (state.isTimeMode) "${bar.endMinutes ?: (bar.index + 1)}" else "${bar.index + 1}"
},
yLabelFormatter = { value -> formatSplitPace(value, state.mileUse) },
```

X축은 거리 모드 `index+1`(1-based 랩 번호), 시간 모드 `bar.endMinutes`(널이면 `index+1` 폴백).
막대 상단 라벨은 `bar.value`(초/단위) — 주석: *"막대 상단 = 표시 페이스(bar.value = 초/단위) — iOS barLabels 동일"*

### 6.6 단위 환산 요약표 (iOS 대조용)

| 값 | km 모드 | mile 모드 |
|---|---|---|
| 라인 x (거리) | `distanceKm` | `distanceKm × 0.621371` |
| 라인 x (시간) | `elapsedSeconds / 60` | 동일 |
| `paceSeconds` | `paceMinPerKm × 60` | `paceMinPerKm × 60 × 1.609344000000865` |
| 시리즈 y (코어 산출) | 분/km | 분/mile |
| 축 라벨 역변환 | `value` 그대로 | `value / 1.609344000000865` |
| `bestPaceMinPerKm` | `bestPaceSeconds / 60` | `bestPaceSeconds / 60 / 1.609344000000865` |
| `splitDistanceMeters` | `1000.0` | `1609.344` |
| `formatPace` 내부 | `min×60` | `min×60 / 0.621371` |
| 고도 | 항상 m | 항상 m (변환 없음) |

---

## 7. 유의점 정리 (SDK 개선 후보 / 파리티 리스크)

1. **`SplitChartCard.kt:69-83`의 앵커 규칙 복사본** — 라이브러리 private 로직 중복. 어긋나면 컴파일 에러 없이 범례색과 막대색이 조용히 갈립니다. 코드 주석 자체가 `fastest/slowest/average` 공개 API화를 후속 과제로 지목하고 있습니다. **가장 시급한 SDK 요구사항.**
2. **`UnifiedChartCard.kt:180`의 `"-'--\""` 리터럴이 `formatPace`의 `"-'--''"`와 불일치** — `--` 폴백이 죽어 있습니다. 주석은 "iOS normalized() 동일"이라고 주장하므로, iOS 페이스 문자열 형식과의 실제 파리티를 확인해야 합니다.
3. **`UnifiedChartCard.kt:203-208`의 PACE 값 템플릿(`00'00"`, 6자)이 실출력(`00'00''`, 7자)보다 짧음** — 값이 비었을 때 예약 폭 부족.
4. **`invertedAxes` 계산이 슬롯 0/1만 처리** (`UnifiedChartCard.kt:151-155`) — 0.29.0에서 슬롯 1 이후가 전부 보조축이 된 규약과 맞물려, 페이스가 슬롯 2 이상에 배정되면 반전이 누락됩니다.
5. **마일 상수 3벌**: `1609.344`(SplitChartDataBuilder), `1.609344000000865`(LocationUtil.`MILE_PER_KM`), `0.621371`(LocationUtil.`KM_PER_MILE` 및 staticlib `Distance.UNIT_KM_TO_MILE`). 이름이 값과 반대 방향이라 iOS 대조 시 이름 기반 매칭은 반드시 실패합니다.
6. **`LocationInfo.KM_PER_MIN_VALUE = 16.666666666666998`** — `1000/60`의 근사 리터럴. iOS가 정확한 나눗셈을 쓰면 ~1e-13 상대 오차가 페이스 전체에 실립니다(표시 자릿수에는 영향 없음). `getNowPaceV2` 내부의 `float` 중간 연산도 함께 확인 필요.
7. **성별 불명(`-1`)이 남성 공식으로 처리됨** (`HeartRateZoneCalculator.kt:18`) — 의도된 동작인지 확인 필요.
8. **다크 테마 미지원** — `ChartStyle.defaults(darkTheme = false)` 고정, 카드 배경 `Color.White` 하드코딩.
9. **`RDHeartRateZoneChart(onSelectSegment = null)`이 터치를 통째로 끄는 API 함정** (`HeartRateZoneCard.kt:92-93`) — no-op 람다를 강제로 넘겨야 탭 토글이 살아남습니다. SDK 쪽에서 선택 활성화와 콜백을 분리할 여지.
10. **로케일 API 노출면** — `formatPace`/`formatZoneTime`의 `String.format` 로케일 미지정, `formatDistanceTick`의 `"%g"` + `'.' in it` 검사(소수점 구분자가 `,`인 로케일에서 trim 실패).
11. **범례·폭 적응 로직이 두 카드에 중복 구현** — `UnifiedChartCard.rememberLegendMetrics`(3단 축소)와 `HeartRateZoneCard.rememberRequiredLegendWidth`(실측 후 도넛 축소)가 서로 다른 알고리즘. SDK가 범례를 제공하지 않아 생긴 중복.
