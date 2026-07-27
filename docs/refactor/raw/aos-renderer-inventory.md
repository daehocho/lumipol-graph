# AOS 렌더러 경계 인벤토리

경로 접두사 생략: `android-renderer/src/main/kotlin/com/lumipol/graph/renderer/`
iOS 접두사 생략: `ios-renderer/Sources/LumipolGraphUI/`
코어 접두사 생략: `core/src/commonMain/kotlin/com/lumipol/graph/`

## 총평 (먼저)

렌더러가 코어를 **호출하는 지점은 11개뿐**입니다. 코어의 `BarChartEngine`, `HeartRateZoneEngine`, `PaceSeriesEngine`, `SeriesSelection`, `stats/*`, `niceScale`, `AxisDomain`, 2-인자 `nearest`는 **안드로이드 렌더러에서 단 한 번도 호출되지 않습니다**(전수 grep 확인). 반대로 렌더러가 자체적으로 값을 만들어내는 곳이 다수 있으며, 그중 아키텍처상 가장 무거운 위반은 세 곳입니다:

1. **`AxisScale`** (`AxisScale.kt:22-46`) — 코어 `AxisDomain.normalize`의 **역함수를 렌더러가 tick 두 점에서 재구성**. 코어는 정규화만 내보내고 역변환 API를 안 주므로, 렌더러가 도메인 값을 역산하는 유일한 경로이고 줌·스크럽·실루엣 전부가 여기에 매달려 있습니다.
2. **막대 색 앵커 계산** (`RDBarChart.kt:243-248`) — `fastest/slowest/average` + "온전 스플릿만" 폴백 규칙. 순수 데이터 축약인데 코어에 대응 API가 전혀 없고 iOS(`RDBarChartView.swift:104-109`)에 그대로 복제되어 있습니다.
3. **`ZoomState`** (`ZoomState.kt`) — 도메인 창 계산 전체가 렌더러. 코어에 대응 없음.

---

## 1. `ChartStyle.kt` (242줄) — iOS `ChartStyle.swift`(101줄)

### 자체 계산 로직

**dp→px 밀도 환산** — 안드로이드 전용. iOS엔 대응 없음(CoreGraphics가 pt 좌표).

`ChartStyle.kt:223-242`:
```kotlin
internal fun ChartStyle.scaledForDensity(density: Float): ChartStyle {
    fun FloatArray.scaled(): FloatArray = FloatArray(size) { this[it] * density }
    return copy(
        lineWidth = lineWidth * density,
```
스케일 대상: `lineWidth`, `gridLineDashPattern`, `gridLineWidth`, `overlayLineWidth`, `refLineDashPattern`, `barCornerRadius`, `barMinHeight`, `donutRingWidth`, `plotInsets`(4변), `touchDotRadius`. **스케일 안 하는 것**: 색·알파·비율(`gradientMaxAlpha`, `areaHeightFraction`)·`axisLabelFontSize`(sp).

**헤어라인 하한** — 안드로이드 전용 파생 계산. `ChartStyle.kt:228`:
```kotlin
gridLineWidth = maxOf(gridLineWidth * density, ChartStyle.HAIRLINE_MIN_PX),
```
코어 대응: 없음(플랫폼 픽셀 문제).

### 상수/기본값 전체 표 (상수 대조표 Android 열)

| 필드 | Android 기본값 | 줄 | iOS 기본값 | 줄(ChartStyle.swift) | 일치 |
|---|---|---|---|---|---|
| `lineWidth` | `2f` | 37 | `2` | 6 | 같음 |
| `gradientMaxAlpha` | `0.25f` | 50 | `0.25` | 17 | 같음 |
| `seriesColors` | `emptyMap()` | 56 | `[:]` | 20 | 같음 |
| `gridLineDashPattern` | `floatArrayOf(3f, 3f)` | 60 | `[3, 3]` | 24 | 같음 |
| `gridLineWidth` | `0.5f` | 61 | `0.5` | 25 | 같음 |
| `overlayLineWidth` | `1.5f` | 66 | `1.5` | 30 | 같음 |
| `refLineDashPattern` | `floatArrayOf(6f, 3f)` | 69 | `[6, 3]` | 33 | 같음 |
| `areaHeightFraction` | `0.35f` | 74 | `0.35` | 38 | 같음 |
| `areaMinValueSpan` | `0.5` | 77 | `0.5` | 41 | 같음 |
| `barWidthRatio` | `0.6f` | 85 | **스타일 필드 없음** — `slot * 0.6` 하드코딩 | RDBarChartView.swift:98 | 값 같고 **주입 가능성만 다름** |
| `partialBarAlpha` | `0.6f` | 86 | `barPartialOpacity = 0.6` | 59 | 값 같음(이름 다름) |
| `barCornerRadius` | `3f` | 87 | `3` | 53 | 같음 |
| `barShowYAxisLabels` | `true` | 88 | `true` | 54 | 같음 |
| `barShowXAxisLabels` | `true` | 89 | `true` | 55 | 같음 |
| `barMinHeight` | `2f` | 91 | `2` | 57 | 같음 |
| `barDimOpacity` | `0.35f` | 92 | `0.35` | 58 | 같음 |
| `barCalloutFontSize` | `12f` | 98 | `.systemFont(ofSize: 12, weight: .semibold)` | 65 | 같음(분해 보관) |
| `barCalloutFontWeight` | `FontWeight.SemiBold` | 99 | 위와 동일 필드 | 65 | 같음 |
| `donutRingWidth` | `28f` | 103 | `28` | 75 | 같음 |
| `donutDimmedAlpha` | `0.3f` | 107 | `0.3` | 79 | 같음 |
| `donutCenterLabelFontSize` | `13f` | 108 | `.systemFont(ofSize: 13)` | 80 | 같음 |
| `donutCenterPercentFontSize` | `28f` | 110 | `.systemFont(ofSize: 28, weight: .bold)` | 82 | 같음 |
| `donutCenterPercentFontWeight` | `FontWeight.Bold` | 111 | 위 | 82 | 같음 |
| `donutAutoDeselectDelaySeconds` | `3f` | 113 | `donutAutoDeselectDelay: TimeInterval = 3.0` | 84 | 같음 |
| `donutSelectionHapticsEnabled` | `true` | 114 | `true` | 85 | 같음 |
| `axisLabelFontSize` | `10f` | 118 | `.systemFont(ofSize: 10)` | 88 | 같음 |
| `axisLabelFontFamily` | `null` | 119 | (UIFont 통합) | 88 | 분해 보관 |
| `axisLabelFontWeight` | `null` | 120 | (UIFont 통합) | 88 | 분해 보관 |
| `plotInsets` | `Insets(16f, 44f, 20f, 44f)` (top/left/bottom/right) | 124 | `UIEdgeInsets(top:16, left:44, bottom:20, right:44)` | 92 | 같음 |
| `touchDotRadius` | `4f` | 128 | `4` | 96 | 같음 |
| `fallbackDataColor` | `Color(0xFF8E8E93)` | 131 | (호출부에서 `?? .systemGray`) | RDHeartRateZoneView.swift:79 | 같음 |
| `HAIRLINE_MIN_PX` | `1f` | 135 | **없음** | — | **Android 전용** |

### 색 RGB 라이트/다크 쌍 (`ChartStyle.kt:141-203`)

iOS는 동적 `UIColor`(시스템 자동), 안드로이드는 두 세트를 **실측 RGB로 고정**. 판정은 호출부(`isSystemInDarkTheme()`).

| 역할 | Light | Dark | 줄(L/D) |
|---|---|---|---|
| `primaryLineColor` | `0xFF007AFF` | `0xFF0A84FF` | 142/174 |
| `secondaryLineColor` | `0xFFFF3B30` | `0xFFFF453A` | 143/175 |
| `gridLineColor` | `0xFFD1D1D6` α0.7 | `0xFF3A3A3C` α0.7 | 144/176 |
| `overlayLineColor` | `0xFFAF52DE` α0.8 | `0xFFBF5AF2` α0.8 | 145/177 |
| `refBandColor` | `0xFFFF9500` α0.12 | `0xFFFF9F0A` α0.12 | 146/178 |
| `areaFillColor` | `0xFFC7C7CC` α0.35 | `0xFF48484A` α0.35 | 147/179 |
| `markerLineColor` | `0xFFD1D1D6` | `0xFF3A3A3C` | 148/180 |
| `markerEmphasisLineColor` | `0xFF8E8E93` | `0xFF8E8E93` | 149/181 |
| `barColors[FASTER]` | `0xFF34C759` | `0xFF30D158` | 151/183 |
| `barColors[ON_TARGET]` | `0xFF8E8E93` | `0xFF8E8E93` | 152/184 |
| `barColors[SLOWER]` | `0xFFFF9500` | `0xFFFF9F0A` | 153/185 |
| `barReferenceLineColor` | `0x000000` α0.6 | `0xFFFFFF` α0.6 | 155/187 |
| `barSelectionLineColor` | `0x000000` α0.55 | `0xFFFFFF` α0.55 | 156/188 |
| `barCalloutBackgroundColor` | `0xFF000000` | `0xFFFFFFFF` | 157/189 |
| `barCalloutTextColor` | `0xFFFFFFFF` | `0xFF000000` | 158/190 |
| `donutColors[ZONE1]` | `0xFF007AFF` | `0xFF0A84FF` | 160/192 |
| `donutColors[ZONE2]` | `0xFF34C759` α0.7 | `0xFF30D158` α0.7 | 161/193 |
| `donutColors[ZONE3]` | `0xFFFFCC00` | `0xFFFFD60A` | 162/194 |
| `donutColors[ZONE4]` | `0xFFFF9500` | `0xFFFF9F0A` | 163/195 |
| `donutColors[ZONE5]` | `0xFFFF3B30` | `0xFFFF453A` | 164/196 |
| `donutEmptyColor` | `0xFFD1D1D6` α0.5 | `0xFF3A3A3C` α0.5 | 166/198 |
| `donutCenterLabelColor` | `0xFF3C3C43` α0.6 | `0xFFEBEBF5` α0.6 | 167/199 |
| `donutCenterPercentColor` | `0xFF000000` | `0xFFFFFFFF` | 168/200 |
| `axisLabelColor` | `0xFF3C3C43` α0.6 | `0xFFEBEBF5` α0.6 | 169/201 |
| `touchLineColor` | `0xFF000000` | `0xFFFFFFFF` | 170/202 |

**주목**: **`gradientMaxAlpha`가 색 알파와 별개로 `√n` 감쇠된다는 규칙이 스타일 주석에만 산문으로 남아 있고**(`ChartStyle.kt:41-48`) 실제 계산은 `LineChartDrawing.kt:180`에 있습니다.

### 코어 API 호출 지점
`ChartStyle.kt:8-9` — 타입만 임포트: `model.BarColorRole`, `model.DonutColorRole`. 함수 호출 없음.

---

## 2. `AxisScale.kt` (46줄) — iOS `AxisScale.swift`(28줄)

### 자체 계산 로직 — **가장 중요한 경계 위반**

**tick 두 점에서 선형관계 재구성 → 정규화↔도메인 양방향 변환.** `AxisScale.kt:27-31`:
```kotlin
    fun value(atPosition: Double): Double =
        baseValue + (atPosition - basePosition) * valuePerPosition

    fun position(ofValue: Double): Double =
        basePosition + (ofValue - baseValue) / valuePerPosition
```
기울기 산출 `AxisScale.kt:39-43`:
```kotlin
            return AxisScale(
                baseValue = first.value,
                basePosition = first.position,
                valuePerPosition = (last.value - first.value) / (last.position - first.position),
            )
```
**코어 대응**: `scale/AxisDomain.kt:9-10`의 `normalize`가 정방향(`(v-min)/(max-min)`)이지만 **역변환도, 도메인(min/max) 노출도 없습니다**. `LineChartLayout`(`model/Output.kt:19-25`)에 `AxisDomain`이 실려 나오지 않으므로 렌더러가 tick에서 역산할 수밖에 없는 구조입니다. → **코어에 `AxisDomain` 출력 또는 역변환 API를 추가하면 이 클래스는 전부 사라집니다.**

**축 tick 조회 헬퍼** `AxisScale.kt:12-13`:
```kotlin
internal fun LineChartLayout.ticksFor(axis: ChartAxis): List<AxisTick>? =
    axisTicks.firstOrNull { it.axis == axis }?.ticks
```
코어 대응: 없음(`LineChartLayout.axisTicks`가 List라 선형 탐색). iOS는 각 호출부에 인라인(`RDChartView.swift:522` `axisTicks.first(where: { $0.axis == .x })`).

### iOS 대비
로직·실패 조건(2점 미만/간격 0 → null) **완전 동일**. 차이: 안드로이드는 `ticksFor` 헬퍼를 추가해 4곳 중복을 제거했고 iOS엔 없음, iOS는 `RDChartView.swift:518-525`에서 `cachedXAxisScale`로 캐시하는데 안드로이드는 매번 새로 만듭니다(`LineGestures.kt:66`, `TouchMarker.kt:60`).

### 서브시스템
B(스케일·틱). 전량.

---

## 3. `PlotArea.kt` (62줄) — iOS `PlotArea.swift`(46줄)

### 자체 계산 로직 (전부 좌표 변환 — 렌더러 고유 책임으로 타당)

플롯 사각형 파생 `PlotArea.kt:25-29`:
```kotlin
    val width: Double = sizeWidth - insets.left - insets.right
    val height: Double = sizeHeight - insets.top - insets.bottom
```

**Y축 반전 처리 — 정규화 0~1 → 화면 좌표 반전의 유일한 정식 지점** `PlotArea.kt:39-42`:
```kotlin
    fun y(ny: Double, axis: Axis): Double {
        val fractionFromTop = if (invertedAxes.contains(axis)) ny else 1.0 - ny
        return minY + fractionFromTop * height
    }
```
X 변환 `PlotArea.kt:37`: `fun x(nx: Double): Double = minX + nx * width`

오버레이 전용 반전 무시 변환 `PlotArea.kt:51`:
```kotlin
    fun yIgnoringInversion(ny: Double): Double = minY + (1.0 - ny) * height
```

**히트테스트 역변환(픽셀 x → 정규화 x) + 클램프** `PlotArea.kt:58-61`:
```kotlin
    fun normalizedX(px: Double): Double {
        if (width <= 0) return 0.0
        return ((px - minX) / width).coerceIn(0.0, 1.0)
    }
```
코어 대응: 없음(픽셀 좌표계는 플랫폼 관심사).

렌더 가능 판정 `PlotArea.kt:35`: `val isRenderable: Boolean get() = width > 0 && height > 0`

### Density 환산 지점
`PlotArea`는 density를 **모릅니다** — 이미 px로 스케일된 `Insets`를 받습니다(주입: `RDLineChart.kt:171`, `RDBarChart.kt:98/195/329`).

### iOS 대비
식·클램프·`isRenderable` 조건 **완전 동일**. 차이: iOS는 `CGRect`/`CGPoint`를 쓰고(`PlotArea.swift:7` `let rect: CGRect`), 안드로이드는 arch 계약대로 Compose 기하 타입을 배제하고 `Double`+로컬 `PlotPoint`(`PlotArea.kt:8`).

### 서브시스템
C(좌표변환·레이아웃) 전량, E(상호작용)에 `normalizedX`.

---

## 4. `AreaSilhouette.kt` (69줄) — iOS `AreaSilhouette.swift`(56줄)

### 자체 계산 로직

**실루엣 높이 정규화는 코어 위임** `AreaSilhouette.kt:24-25`:
```kotlin
    fun heightFractions(values: List<Double>, minSpan: Double = 0.0): List<Double> =
        com.lumipol.graph.query.heightFractions(values, minSpan)
```
코어 대응: **있음** — `query/HeightFractions.kt:13-19`. (축퇴 시 0 = 평지, `AxisDomain.normalize`의 0.5와 다른 전용 의미론.)

**실루엣 픽셀 매핑은 렌더러 자체** — `PlotArea.y`를 안 쓰고 바닥 기준 자체 매핑. `AreaSilhouette.kt:42-48`:
```kotlin
        val baseY = plot.maxY
        val usableHeight = style.areaHeightFraction * plot.height

        fun pixel(index: Int): PlotPoint {
            val nx = xScale.position(points[index].x).coerceIn(0.0, 1.0)
            return PlotPoint(x = plot.x(nx), y = baseY - fractions[index] * usableHeight)
        }
```
- 코어 대응: 픽셀 매핑은 없음(타당). 단 `xScale.position` 의존 = 위 `AxisScale` 문제 상속.
- **0~1 밖 클램프**(`coerceIn(0.0, 1.0)`)는 렌더러 판단 — 코어의 windowed layout이 이웃 포인트를 0~1 밖으로 내보내는 것과 같은 문제를 area에선 렌더러가 클램프로 처리.

폴리곤 닫기(바닥 두 점 추가) `AreaSilhouette.kt:50-55`:
```kotlin
        val polygon = buildList {
            add(pixel(0))
            for (i in 1 until points.size) add(pixel(i))
            add(PlotPoint(pixel(points.size - 1).x, baseY))
            add(PlotPoint(pixel(0).x, baseY))
        }
```
2점 미만 가드 `AreaSilhouette.kt:40`: `if (points.size < 2 || !plot.isRenderable) return null`

### 코어 API 호출 지점
- `AreaSilhouette.kt:25` → `com.lumipol.graph.query.heightFractions`
- `AreaSilhouette.kt:6` → `model.Point` 타입 재사용

### iOS 대비
계산 **완전 동일**(`AreaSilhouette.swift:31-48`). 차이 두 개: iOS는 별도 공개 타입 `AreaPoint`를 정의(`AreaSilhouette.swift:5-12`)하지만 **안드로이드는 코어 `Point`를 재사용**(주석 `AreaSilhouette.kt:14-15`: "공개 입력 타입 중복 제거"), iOS는 `CAShapeLayer`를 직접 반환하고 안드로이드는 순수 `AreaFillLayer`를 반환한 뒤 `render`가 그림.

### 서브시스템
A(정규화)에 `heightFractions` 위임, C(좌표변환)에 `build` 픽셀 매핑·클램프·폴리곤 조립.

---

## 5. `PaceColormap.kt` (45줄) — iOS `PaceColormap.swift`(44줄)

### 자체 계산 로직 — **색 보간 전량 렌더러, 코어 대응 없음**

앵커 산출(매직 넘버 0.70 / 0.25) `PaceColormap.kt:27-29`:
```kotlin
    val pace1 = a - (a - f) * 0.70
    val pace2 = a + (s - a) * 0.25
    val length1 = pace1 - f; val length2 = pace2 - pace1; val length3 = s - pace2
```
3구간 보간 `PaceColormap.kt:31-44`:
```kotlin
        p < pace1 -> {                                  // 파랑↔청록
            val cv = if (length1 > 0) clamp((pace1 - maxOf(f, p)) / length1) else 0f
            Color(red = 0f, green = 1f - 0.4f * cv, blue = 1f)
        }
        p < pace2 -> {                                  // 초록↔노랑
            val cv = if (length2 > 0) clamp((pace2 - p) / length2) else 0f
            Color(red = 1f - cv, green = 1f, blue = 0f)
        }
        else -> {                                       // 노랑↔빨강
            val cv = if (length3 > 0) clamp((s - minOf(s, p)) / length3) else 0f
            Color(red = 1f, green = cv, blue = 0f)
        }
```
축퇴 폴백 `PaceColormap.kt:26`: `if (s <= f) return Color(red = 0f, green = 1f, blue = 0f)`

**코어 대응 추정: 없음.** 코어에 컬러맵/보간 API가 전무합니다(`query/` 4개 + `scale/` 2개 + `stats/` 어디에도). 색 자체는 플랫폼 타입이지만 **"값 → 0~1 3구간 보간 계수 cv"는 플랫폼 중립 수학**이고 지금 양 플랫폼에 동일 공식이 복제되어 있습니다.

### 상수
`0.70`(pace1 앵커), `0.25`(pace2 앵커), `0.4f`(청록 green 감쇠 폭), 축퇴 폴백 RGB `(0,1,0)`.

### iOS 대비
공식·매직넘버·분기 순서·클램프 **완전 동일**(`PaceColormap.swift:29-42`). `BarPaceColorInput` 7필드도 동일(`PaceColormap.kt:8-16` vs `PaceColormap.swift:5-19`).

### 서브시스템
D(스타일 상수)에 앵커 비율, 그리고 **분류에 안 맞는 잔여**: 값→색 보간은 A/B/D 어디에도 깔끔히 안 들어갑니다 — 별도 취급 필요.

---

## 6. `ZoomState.kt` (84줄) — iOS `ZoomState.swift`(62줄)

### 자체 계산 로직 — **줌 도메인 계산 전량 렌더러, 코어 대응 없음**

배율·폭 파생 `ZoomState.kt:22-25`:
```kotlin
    val scale: Double get() = fullSpan / span
    private val span: Double get() = window.endInclusive - window.start
    private val fullSpan: Double get() = fullDomain.endInclusive - fullDomain.start
```

핀치(단발) `ZoomState.kt:34-39`:
```kotlin
    fun pinch(gestureScale: Double, anchor: Double, maxScale: Double): ZoomState {
        if (gestureScale <= 0) return this
        val targetSpan = min(max(span / gestureScale, fullSpan / maxScale), fullSpan)
        val anchorValue = window.start + anchor * span
        return place(lower = anchorValue - anchor * targetSpan, span = targetSpan)
    }
```
핀치(누적, 프로덕션 경로) `ZoomState.kt:52-55`:
```kotlin
        val startSpan = startWindow.endInclusive - startWindow.start
        val targetSpan = min(max(startSpan / cumulativeScale, fullSpan / maxScale), fullSpan)
        val anchorValue = startWindow.start + anchor * startSpan
```
팬 `ZoomState.kt:59-60`:
```kotlin
    fun pan(fraction: Double): ZoomState =
        place(lower = window.start - fraction * span, span = span)
```
창 클램프 + **ulp 방어** `ZoomState.kt:71-77`:
```kotlin
    private fun place(lower: Double, span: Double): ZoomState {
        if (span >= fullSpan) return copy(window = fullDomain)
        val clamped = min(max(lower, fullDomain.start), fullDomain.endInclusive - span)
        return copy(window = clamped..(clamped + span))
    }
```
`setWindow` 폭 클램프 `ZoomState.kt:64`: `val clampedSpan = min(target.endInclusive - target.start, fullSpan)`

**코어 대응 추정: 없음.** 코어는 `LineChartEngine.layout(data, xMin, xMax)`(`LineChartEngine.kt:45`)로 **주어진 창을 렌더링**할 뿐, 창을 **계산**하지 않습니다. 핀치 앵커 고정·maxScale 클램프·전체범위 클램프는 전부 플랫폼 중립 수학이며 양 렌더러에 복제 중.

### iOS 대비
식·클램프 순서·ulp 주석까지 **완전 동일**(`ZoomState.swift:23-62`). 유일한 구조 차이: iOS `mutating struct`, 안드로이드 불변 `data class` + `copy`(`ZoomState.kt:11-12` 주석). `full(fullDomain)` 팩토리(`ZoomState.kt:81-82`)가 iOS `init(fullDomain:)` 대응.

### 서브시스템
E(상호작용) 전량 — 단, 실체는 "도메인 창 산술"이라 B에 더 가깝습니다.

---

## 7. `LineChartDrawing.kt` (722줄) — iOS `ChartLayerBuilder.swift`(275줄)

### 자체 계산 로직

**그라데이션 알파 √n 감쇠** `LineChartDrawing.kt:178-180`:
```kotlin
    val n = drawableSeries.size
    if (style.gradientMaxAlpha > 0f && n > 0) {
        val alpha = style.gradientMaxAlpha / kotlin.math.sqrt(n.toFloat())
```
코어 대응: 없음(순수 프레젠테이션). iOS 동일: `ChartLayerBuilder.swift:41` `style.gradientMaxAlpha / CGFloat(n).squareRoot()`.

**밴드 사각형 상하 정렬 + 절대값 높이** — 축 반전 시 y1/y2가 뒤집히는 것을 렌더러가 흡수. `LineChartDrawing.kt:155-163`:
```kotlin
        val y1 = plot.y(band.lower, band.axis)
        val y2 = plot.y(band.upper, band.axis)
                minY = minOf(y1, y2),
                height = kotlin.math.abs(y1 - y2),
```

**시리즈 → 축 매핑의 "첫 시리즈 우선" 규칙** — 코어가 id 유일성을 강제하지 않아 렌더러가 규칙을 정함. `LineChartDrawing.kt:223-230`:
```kotlin
internal inline fun <T> firstWinsBy(
    data: LineChartData,
    selector: (com.lumipol.graph.model.Series) -> T,
): Map<String, T> {
    val map = LinkedHashMap<String, T>()
    for (series in data.series) map.putIfAbsent(series.id, selector(series))
```
**코어 대응: 없음** — 이건 코어가 계약(유일성)을 강제하거나 매핑을 실어 보내야 할 사안입니다. 지금은 렌더러 2곳(`LineChartDrawing.kt:232`, `LineChartInteraction.kt:53-54`)이 공유.

**시리즈 색 리졸버(맵 우선 → 역할 → 축 폴백)** `LineChartDrawing.kt:235-239`:
```kotlin
internal fun seriesColor(id: String, role: SeriesRole, axis: Axis, style: ChartStyle): Color {
    style.seriesColors[id]?.let { return it }
    if (role == SeriesRole.OVERLAY) return style.overlayLineColor
    return if (axis == Axis.SECONDARY) style.secondaryLineColor else style.primaryLineColor
}
```

**2점 미만 시리즈 제외** `LineChartDrawing.kt:242-243` / `248-249`: `if (points.size < 2) return null` (iOS `ChartLayerBuilder.swift:70` 동일).

**그리드 Y축 폴백 선택** — primary 없으면 secondary. `LineChartDrawing.kt:274-280`:
```kotlin
    if (primaryTicks.isEmpty()) {
        yTicks = ticks(ChartAxis.Y_SECONDARY)
        yAxis = Axis.SECONDARY
    } else {
```

**area 폴리곤 닫기(그라데이션)** `LineChartDrawing.kt:341-345`:
```kotlin
    val polygon = buildList {
        addAll(linePoints)
        add(PlotPoint(linePoints.last().x, plot.maxY))
        add(PlotPoint(linePoints.first().x, plot.maxY))
    }
```

**텍스트 정렬 앵커 → 픽셀 원점 환산** `LineChartDrawing.kt:672-681`:
```kotlin
    val originX = when (label.hAlign) {
        HAlign.LEFT -> label.anchorX.toFloat()
        HAlign.CENTER -> label.anchorX.toFloat() - w / 2f
        HAlign.RIGHT -> label.anchorX.toFloat() - w
    }
    val originY = when (label.vAlign) {
        VAlign.ABOVE -> label.anchorY.toFloat() - h
        VAlign.BELOW -> label.anchorY.toFloat()
        VAlign.CENTER -> label.anchorY.toFloat() - h / 2f
    }
```

**접근성 글꼴 배율 상한 — Android 전용 계산** `LineChartDrawing.kt:691-692`:
```kotlin
internal fun effectiveLabelSp(fontSizeSp: Float, fontScale: Float): Float =
    if (fontScale > MAX_FONT_SCALE) fontSizeSp * (MAX_FONT_SCALE / fontScale) else fontSizeSp
```
iOS 대응 없음(Dynamic Type 미적용).

**Path 트림(등장 애니)** `LineChartDrawing.kt:649-655`:
```kotlin
private fun trimPath(path: Path, progress: Float): Path {
    val measure = PathMeasure()
    measure.setPath(path, false)
    measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), dst, true)
```
iOS는 `strokeEnd` CABasicAnimation(`RDChartView.swift:219-223`)으로 **프레임워크가 처리** — 안드로이드는 렌더러가 길이비율을 직접 계산.

**클립 사각형 결정(확대 시에만, top=0)** `LineChartDrawing.kt:491-498`:
```kotlin
    if (isZoomed) {
        clipRect(
            left = plot.minX.toFloat(),
            top = 0f, // 뷰 상단까지 열어 상단 여백의 마커/기준선 라벨 보존
            right = plot.maxX.toFloat(),
            bottom = plot.maxY.toFloat(),
```

**캐시 키 구성(값 동등성)** `LineChartDrawing.kt:540-545`:
```kotlin
        val newKey = listOf(
            layout, data, style,
            plot.minX, plot.minY, plot.width, plot.height, plot.invertedAxes,
            formatter, density, sortedArea,
        )
```
Android 전용(iOS는 CALayer 유지 트리 + `lastRebuildKey`, `RDChartView.swift:214`).

**라벨 폭 측정(솎아내기 입력)** `LineChartDrawing.kt:699-714` — `measureLabelWidthPx`, `tnum` 피처 고정. iOS는 `NSString.size(withAttributes:)`(`RDBarChartView.swift:118-119`)로 하되 **tnum 미지정** → **폭 측정 규칙이 미세하게 다릅니다**.

### 포맷팅
이 파일엔 문자열 **생성**이 없습니다. 주입된 `formatter(ChartAxis, Double) -> String`만 호출: `LineChartDrawing.kt:365` `val text = formatter(ticksLayout.axis, tick.value)`. 레이어 이름 문자열은 테스트 계약(`"grid"`, `"band.$index"`, `"marker.$index.line"`, `"series.main.${series.id}"`, `"series.overlay.${series.id}"`, `"series.gradient.$seriesId"`, `"axisLabels.x.$i"` 등).

### 상수 (`LineChartDrawing.kt:717-722`)
```kotlin
private const val LABEL_GAP = 2.0            // 마커/기준선 라벨과 선 사이 여백(iOS -2)
private const val AXIS_LABEL_GAP = 4.0       // 축 라벨과 플롯 경계 여백(iOS ±4)
private const val MARKER_WIDTH = 1f
private const val MARKER_EMPHASIS_WIDTH = 1.5f
private const val MAX_FONT_SCALE = 1.3f
```
iOS 대조: `LABEL_GAP` = `ChartLayerBuilder.swift:215` `minY - text.frame.height - 2` ✅ / `AXIS_LABEL_GAP` = `swift:239,244,249` `maxY + 4`, `minX - size.width - 4`, `maxX + 4` ✅ / 마커 폭 = `swift:209` `marker.emphasis ? 1.5 : 1` ✅ / `MAX_FONT_SCALE` **Android 전용**.
기타 인라인 상수: `StrokeCap.Round`/`StrokeJoin.Round`(main 라인, `:205-206`), `StrokeJoin.Round`(overlay, `:195`), 그라데이션 bottom 알파 `0f`(`:351`).

### 코어 API 호출 지점
**없음**(함수 호출 0). 타입만: `model.Axis`, `ChartAxis`, `LineChartData`, `LineChartLayout`, `NormalizedPoint`, `SeriesLayout`, `SeriesRole`, `AxisTick`, `MarkerLayout`, `AxisTicksLayout`, `Point`, `Series`.

### Y축 방향 / Density
- 반전은 전부 `plot.y(...)` 위임(`:155-156`, `:282`, `:383`, `:395`) — 자체 반전 식 없음. **단** 오버레이는 `plot.pointIgnoringInversion`(`:250`).
- Density 환산 지점: `drawLineChart`가 `DrawScope.density`를 읽어 스타일 밖 상수에 곱함 — `:451` `buildLineChartLayers(..., density)`, 실제 곱셈은 `:309` `(if (marker.emphasis) MARKER_EMPHASIS_WIDTH else MARKER_WIDTH) * density`, `:318` `plot.minY - LABEL_GAP * density`, `:371/382/394` `AXIS_LABEL_GAP * density`.

### iOS 대비
레이어 이름·z순서·기하 동일. 구조 차이: iOS는 CALayer 트리를 직접 조립, 안드로이드는 **순수 중간 모델**(`LineChartLayer` sealed interface, `:46-124`) → `render` 디스패치(`:569-628`). 캐시(`LineChartDrawCache`, `:513-565`)·`trimPath`·`effectiveLabelSp`·`HAIRLINE_MIN_PX`는 Android 전용.

---

## 8. `TouchMarker.kt` (195줄) — iOS `TouchMarker.swift`(174줄)

### 자체 계산 로직

**근접 후보 창을 epsilon만큼 넓혀 코어에 넘김** `TouchMarker.kt:64-69`:
```kotlin
        val results = LineChartEngine.nearest(
            context.data,
            rawX,
            xMin = xScale.value(-WINDOW_EPSILON),
            xMax = xScale.value(1 + WINDOW_EPSILON),
        )
```
코어 대응: 근접 판정 자체는 **`query/Nearest.kt:14-19`** (있음). 다만 **창 경계 epsilon 관대화는 렌더러 결정**.

**스냅 소스 선택 규칙 — main 우선, 없으면 첫 시리즈** `TouchMarker.kt:73-75`:
```kotlin
        val snapSource = results.firstOrNull { roleBySeriesId[it.seriesId] == SeriesRole.MAIN }
            ?: results.firstOrNull()
        val snappedX = snapSource?.x ?: return null
```
**코어 대응: 없음.** "여러 시리즈의 근접점 중 무엇을 수직선 기준으로 쓸지"는 플랫폼 중립 정책인데 렌더러가 소유하고 있습니다.

**창 밖 판정 + 클램프** `TouchMarker.kt:77-79`:
```kotlin
        val rawNx = xScale.position(snappedX)
        if (rawNx < -WINDOW_EPSILON || rawNx > 1 + WINDOW_EPSILON) return null
        val nx = rawNx.coerceIn(0.0, 1.0)
```
시리즈별 창 밖 생략 `TouchMarker.kt:87-88`:
```kotlin
            val seriesNx = xScale.position(result.x)
            if (seriesNx < -WINDOW_EPSILON || seriesNx > 1 + WINDOW_EPSILON) continue
```

**오버레이 도트 위치 = layout 정규화 포인트 재-최근접 탐색** `TouchMarker.kt:153-155`:
```kotlin
        val layoutPoint = layoutSeries.points.minByOrNull { abs(it.x - seriesNx) } ?: return null
        val point = context.plot.pointIgnoringInversion(NormalizedPoint(x = nx, y = layoutPoint.y))
```
**코어 대응: 부분적으로 없음** — 오버레이는 축이 없어 `AxisScale` 역산이 불가하니 렌더러가 정규화 공간에서 두 번째 최근접 탐색을 수행합니다. 코어 `nearest`가 정규화 y도 함께 실어주면 사라질 로직.

**Y 역산으로 도트 y 좌표 산출** `TouchMarker.kt:99-104`:
```kotlin
            val yTicks = ticks(chartAxis, context.layout) ?: continue
            val yScale = AxisScale.from(yTicks) ?: continue
            val point = context.plot.point(
                NormalizedPoint(x = nx, y = yScale.position(result.y)),
                axis,
            )
```
→ **원본 y를 Y tick 선형관계로 재정규화**. 코어가 이미 정규화한 값을 갖고 있는데(`SeriesLayout.points`) 렌더러가 역산으로 다시 구합니다.

`Axis` → `ChartAxis` 매핑 `TouchMarker.kt:182-185`:
```kotlin
    private fun chartAxis(axis: Axis): ChartAxis? = when (axis) {
        Axis.PRIMARY -> ChartAxis.Y_PRIMARY
        Axis.SECONDARY -> ChartAxis.Y_SECONDARY
```
코어 대응: 없음(입력축 `Axis`와 출력축 `ChartAxis`가 별 enum이라 렌더러가 브리지).

### 포맷팅
문자열 **생성 없음**, 주입 포매터 호출 2곳:
- `TouchMarker.kt:94` `valuesBySeriesId[result.seriesId] = context.formatter(ChartAxis.Y_OVERLAY, result.y)`
- `TouchMarker.kt:120` `valuesBySeriesId[result.seriesId] = context.formatter(chartAxis, result.y)`

### 상수
`TouchMarker.kt:188-189`:
```kotlin
    const val WINDOW_EPSILON = 1e-9
    private const val TOUCH_LINE_WIDTH = 1f
```
iOS: `TouchMarker.swift:31,71,84` `1e-9` ✅, `TouchMarker.swift:161` `line.lineWidth = 1` ✅.

### 코어 API 호출 지점
- `TouchMarker.kt:64` → `LineChartEngine.nearest(data, x, xMin, xMax)` (`LineChartEngine.kt:148`)

### Y축 방향 / Density
`plot.point(..., axis)`(반전 적용, `:101`), `plot.pointIgnoringInversion`(오버레이, `:155`). Density: `TouchMarker.kt:175` `width = TOUCH_LINE_WIDTH * context.density` — `TouchMarkerContext.density`로 주입(`:33`, 기본 `1f`), 도트 반경은 이미 스케일된 `style.touchDotRadius`(`:116`, `:159`).

### iOS 대비
epsilon·스냅 규칙·창 밖 생략 **완전 동일**. 차이: 안드로이드는 `axisBySeriesId`/`roleBySeriesId`를 **컨텍스트로 주입받아 캐시**(`:34-35`, 스크럽 60~120Hz 대응), iOS `TouchMarker.Context`는 `data/layout/style/plotArea/formatter`만(`TouchMarker.swift:12` 주변) — 매 호출 재계산.

---

## 9. `LineChartInteraction.kt` (229줄) — iOS `RDChartView.swift` 상태부

### 자체 계산 로직

**줌 창 → layout 선택 분기** `LineChartInteraction.kt:73-81`:
```kotlin
    fun layoutForCurrentWindow(): LineChartLayout {
        val z = zoom
        if (z != null && z.isZoomed) {
            val lo = z.window.start
            val hi = z.window.endInclusive
            if (hi > lo) return LineChartEngine.layout(data, lo, hi)
        }
        return makeFullLayout()
    }
```

**창 경계 epsilon 클램프(스크럽 진입 가드)** `LineChartInteraction.kt:104-109`:
```kotlin
        if (z != null && z.isZoomed) {
            val epsilon = (z.window.endInclusive - z.window.start) * TouchMarker.WINDOW_EPSILON
            if (x < z.window.start - epsilon || x > z.window.endInclusive + epsilon) return false
            x = x.coerceIn(z.window.start, z.window.endInclusive)
        }
```

**시리즈 없음 → 배경 단독 폴백 판정** `LineChartInteraction.kt:114-118`:
```kotlin
        val result = if (data.series.isEmpty()) {
            if (sortedArea?.isEmpty() == false) TouchMarker.makeBackgroundOnly(x, context) else null
        } else {
            TouchMarker.make(x, context)
        }
```

**팬 누적 이동 → 목표 창 산출** `LineChartInteraction.kt:201-204`:
```kotlin
        val span = start.endInclusive - start.start
        val targetLower = start.start - fraction * span
        zoom = zoom?.setWindow(targetLower..(targetLower + span))
```

**줌 초기화: 전체 도메인을 X tick에서 역산** — `LineChartInteraction.kt:220-227`:
```kotlin
    private fun ensureZoom() {
        if (zoom != null) return
        val xTicks = makeFullLayout().ticksFor(ChartAxis.X) ?: return
        val scale = AxisScale.from(xTicks) ?: return
        val lower = scale.value(0.0)
        val upper = scale.value(1.0)
        if (upper <= lower) return
        zoom = ZoomState.full(lower..upper)
    }
```
**이게 `AxisScale` 존재 이유의 핵심**입니다. 코어는 `AxisDomain(xNice.niceMin, xMax)`를 이미 계산했지만(`LineChartEngine.kt:23`) 출력하지 않아, 렌더러가 tick 두 점으로 역산합니다. `LineChartEngine.kt:24`에서 마지막 tick이 `xMax` 안쪽일 수 있으므로 `scale.value(1.0)`은 **외삽**입니다.

**"비줌 = null" 불변식 정규화 3곳** — `:161` `if (zoom?.isZoomed != true) zoom = null`, `:187`(pinchEnded), `:210`(panEnded).

**콜백 짝맞춤 불변식** `:120-127`(마커 실패 시 hadMarker만 endScrub), `:143-148`(endScrub 1회).

### 포맷팅
없음. 포매터는 `TouchMarkerContext`로 통과.

### 상수
`LineChartInteraction.kt:49`: `var maxZoomScale: Double = 10.0` — iOS `RDChartView.swift:51` `maxZoomScale: CGFloat = 10` ✅.

### 코어 API 호출 지점
- `:78` `LineChartEngine.layout(data, lo, hi)` (`LineChartEngine.kt:45`)
- `:87` `LineChartEngine.layout(data, sortedArea)` (`LineChartEngine.kt:34`)
- `:134` `LineChartEngine.interpolatedY(area, result.snappedX)` (`LineChartEngine.kt:152` → `query/AreaInterpolation.kt:10`)

### iOS 대비
줌 수학·콜백 계약 동일. 차이: iOS는 `notifyingDelegate:false`로 relayout 마커를 명령형 복원(`RDChartView.swift:236`)하지만 **안드로이드는 `activeMarkerRawX`에서 draw가 파생**해 재발화가 구조적으로 불가(`:29-31` 주석). iOS의 `isScrubbing` 팬 잠금(`RDChartView.swift:472-476`)은 안드로이드에선 제스처 상태머신 분기로 대체.

---

## 10. `LineGestures.kt` (267줄) — iOS `RDChartView.swift` 제스처부(**직역 아님, 재작성**)

### 자체 계산 로직

**가로 우세 판정** `LineGestures.kt:31`:
```kotlin
internal fun isHorizontalDominant(dx: Float, dy: Float): Boolean = abs(dx) > abs(dy)
```
iOS `RDChartView.swift:439-440` `abs(translation.x) > abs(translation.y)` ✅.

**손가락 픽셀 → 도메인 x** `LineGestures.kt:63-68`:
```kotlin
    fun rawXAt(px: Float): Double? {
        val plot = plotProvider() ?: return null
        val xTicks = layoutProvider().ticksFor(ChartAxis.X) ?: return null
        val xScale = AxisScale.from(xTicks) ?: return null
        return xScale.value(plot.normalizedX(px.toDouble()))
    }
```
iOS `RDChartView.swift:528-530` 동일.

**슬롭 초과 판정** `LineGestures.kt:94-97`:
```kotlin
                    val delta = change.position - down.position
                    if (delta.getDistance() > slop) {
```

**팬 비율 계산 (분모 = 플롯 폭)** `LineGestures.kt:220-221`:
```kotlin
                val fraction = (change.position.x - startX!!) / width
                interaction.panChanged(fraction.toDouble())
```
iOS `translation.x / plotArea.rect.width` (`:201-202` 주석 명시).

**핀치 누적 배율** `LineGestures.kt:245-248`:
```kotlin
            val zoom = event.calculateZoom()
            if (zoom > 0f && zoom.isFinite()) cumulativeScale *= zoom.toDouble()
            val centroid = event.calculateCentroid(useCurrent = true)
            val anchor = if (centroid != Offset.Unspecified) anchorOf(centroid.x) else 0.5
```
앵커 폴백 `0.5`가 3곳 더: `:108`, `:120`, `:142` `plotProvider()?.normalizedX(px.toDouble()) ?: 0.5`.

### 제스처 인식 파라미터 — 의미 해석 위치

`LineGestures.kt:49-51` — **하드코딩 아님, 시스템 값**:
```kotlin
    val slop = viewConfiguration.touchSlop
    val longPressTimeoutMs = viewConfiguration.longPressTimeoutMillis
    val doubleTapTimeoutMs = viewConfiguration.doubleTapTimeoutMillis
```
iOS는 **하드코딩 0.5초**: `RDChartView.swift:465` `longPressRecognizer.minimumPressDuration = 0.5` → **AOS/iOS 롱프레스 임계 불일치**(Android 기본 500ms이므로 실질 근사, 단 사용자 접근성 설정에 따라 갈립니다).

**단일 상태머신 분기** — 5-인식기 조율을 대체. `LineGestures.kt:106-157`:
- `secondPointer` → `pinchLoop` (`:107`)
- `longPress` → 확대 시 햅틱 + `scrubLoop`, 승격 시 `pinchLoop` (`:111-123`)
- 가로 우세 드래그 → 확대면 `panLoop`, 아니면 `scrubLoop` (`:130-146`)
- 탭 → 더블탭 대기 후 `resetZoom`, 아니면 `scrub` (`:151-155`)

2포인터 감지 임계 `:88` `event.changes.count { it.pressed } >= 2`, 핀치 승격 `:183` 동일.

**세로 우세는 미진입·미소비** — 부모 스크롤 양보(`:130` `if (isHorizontalDominant(...))`). iOS는 `shouldRecognizeSimultaneouslyWith=true`로 동시 인식(`:126-129` 주석에 의도적 차이 명시).

**consume 정책**: 스크럽 중 `:187` `change?.let { if (it.positionChanged()) it.consume() }`, 팬 `:216`, 핀치 `:250`.

**햅틱** `LineGestures.kt:113`:
```kotlin
                if (zoomedAtStart) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
```
iOS `RDChartView.swift:498` `UIImpactFeedbackGenerator(style: .medium).impactOccurred()` — **medium ≈ LongPress 근사**(`:38` 주석).

**더블탭 근사** `LineGestures.kt:258-266` — 업 후 `doubleTapTimeoutMs` 안 두 번째 down. iOS는 `markerTapRecognizer.require(toFail: doubleTapRecognizer)`(`RDChartView.swift:462`).

**`endScrub`를 `finally`에 둠** `LineGestures.kt:179-193` — 코루틴 취소 시 짝 불변식 보존. iOS엔 대응 개념 없음.

### 코어 API 호출 지점
**없음.** 타입만 임포트.

### 서브시스템
E(상호작용) 전량.

---

## 11. `RDLineChart.kt` (262줄) — iOS `RDChartView.swift` render 파이프라인

### 포맷팅 — **`defaultLineChartFormatter` 규칙 전체**

`RDLineChart.kt:45-61`:
```kotlin
fun defaultLineChartFormatter(axis: ChartAxis, value: Double): String {
    // C %g의 nan/inf 표기(iOS 동일) — Java는 "NaN"/"Infinity"라 별도 처리.
    if (value.isNaN()) return "nan"
    if (value.isInfinite()) return if (value > 0) "inf" else "-inf"
    // Locale 고정 — 소수점 문자가 기기 지역 설정을 타지 않는다(iOS %g도 C 로케일 표기).
    val formatted = String.format(Locale.ROOT, "%g", value)
    val exponent = formatted.indexOf('e')
    return if (exponent >= 0) {
        trimTrailingZeros(formatted.substring(0, exponent)) + formatted.substring(exponent)
    } else {
        trimTrailingZeros(formatted)
    }
}

private fun trimTrailingZeros(text: String): String =
    if ('.' in text) text.trimEnd('0').trimEnd('.') else text
```
규칙 정리:
- **유효숫자 6자리**(C `%g` 기본), 지수 < −4 또는 ≥ 6이면 지수 표기(예 `1234567.89` → `"1.23457e+06"`)
- **트레일링 0 제거 + 남은 소수점 제거** (Java `%g`는 0을 유지하므로 직접 정리)
- **`Locale.ROOT` 고정** — 로케일 API를 의도적으로 **회피**(기기 지역의 소수점 문자 배제)
- NaN/Inf → `"nan"`/`"inf"`/`"-inf"` (Java 기본 `"NaN"`/`"Infinity"` 우회)
- 축(`axis`) 파라미터는 **받기만 하고 사용 안 함** — 축별 규칙 없음

iOS: `RDChartView.swift:529-531` `String(format: "%g", value)` — **한 줄**. 안드로이드는 이 한 줄과 출력을 맞추기 위해 15줄을 씁니다. **페이스 `mm:ss` 포매터는 SDK에 없습니다** — 앱이 `labelFormatter`로 주입해야 합니다(`RDLineChart.kt:93` 기본값이 `::defaultLineChartFormatter`).

**TalkBack 요약 — 한국어 하드코딩, Android 전용** `RDLineChart.kt:255-262`:
```kotlin
private fun lineChartDescription(data: LineChartData, sortedArea: List<Point>?): String {
    val seriesCount = data.series.size
    return when {
        seriesCount == 0 && sortedArea.isNullOrEmpty() -> "라인 차트, 데이터 없음"
        seriesCount == 0 -> "라인 차트, 배경 고도 영역"
        else -> "라인 차트, 시리즈 ${seriesCount}개" + if (!sortedArea.isNullOrEmpty()) ", 배경 고도 영역 포함" else ""
    }
}
```
iOS 라인차트엔 `accessibilityLabel` 없음(도넛만 있음). **로컬라이즈 불가 문자열이 렌더러에 하드코딩**.

### 자체 계산 로직
**area x 오름차순 정렬** `RDLineChart.kt:105`:
```kotlin
    val sortedArea = remember(backgroundArea) { backgroundArea?.sortedBy { it.x } }
```
코어 `interpolatedY`의 이진탐색 전제를 렌더러가 충족(`query/AreaInterpolation.kt:7-8`이 "렌더러가 저장 시 정렬" 명시). → **정렬 책임이 코어 계약 문서에 있고 구현은 렌더러**.

**PlotArea 단일 생성자(제스처·그리기 공유)** `RDLineChart.kt:166-174`:
```kotlin
    val buildPlot = remember(scaledStyle, invertedAxes) {
        { w: Double, h: Double ->
            if (w <= 0.0 || h <= 0.0) { null } else {
                PlotArea(w, h, scaledStyle.plotInsets, invertedAxes).takeIf { it.isRenderable }
```
**Density 환산 지점** `RDLineChart.kt:122-123`:
```kotlin
    val density = LocalDensity.current.density
    val scaledStyle = remember(style, density) { style.scaledForDensity(density) }
```

**줌 상태 → isZoomed 전달** `:234` `isZoomed = interaction.zoom?.isZoomed == true`.

### 제스처/애니메이션
**라인 등장 애니** `RDLineChart.kt:37`:
```kotlin
private const val ENTRANCE_DURATION_MS = 600
```
`RDLineChart.kt:154-157`:
```kotlin
    val progress = remember { Animatable(if (animateEntrance) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animateEntrance) progress.animateTo(1f, tween(ENTRANCE_DURATION_MS, easing = EmphasizedDecelerate))
    }
```
- 지속시간 **600ms = iOS 0.6s** ✅ (`RDChartView.swift:223` `animation.duration = 0.6`)
- **이징 불일치**: Android `EmphasizedDecelerate` = `CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)` (`RDBarChart.kt:402`) vs iOS `.easeOut` (`RDChartView.swift:224`)
- `animateEntrance` 기본값 **`true`**(`:99`) — iOS는 `needsEntranceAnimation` 플래그(`RDChartView.swift:206`)
- 키가 `Unit`(`:155`) — 컴포저블 수명당 1회, 데이터 갱신·줌 relayout 시 재생 안 함

기타 기본값: `maxZoomScale: Float = 10f`(`:95`), `isZoomEnabled: Boolean = false`(`:94`).

### 코어 API 호출 지점
**직접 호출 없음** — `interaction.layoutForCurrentWindow()`(`:213`, `:191`) 경유. 타입만 임포트(`:30-33`).

---

## 12. `RDBarChart.kt` (402줄) — iOS `RDBarChartView.swift`(299줄)

### 자체 계산 로직

**색 앵커 계산 — 코어 대응 없는 데이터 축약** `RDBarChart.kt:243-248`:
```kotlin
    val fullValues = layout.bars.filter { !it.isPartial }.map { it.value }
    val fullHasRange = fullValues.size >= 2 && fullValues.max() > fullValues.min()
    val anchorValues = if (fullHasRange) fullValues else layout.bars.map { it.value }
    val fastest = anchorValues.min()
    val slowest = anchorValues.max()
    val average = anchorValues.sum() / anchorValues.size
```
**렌더러가 "무엇을 기준으로 색을 칠할지"를 고르고 있습니다** — 부분 스플릿 배제 정책, 2개 미만 폴백, 평균 산출 전부. 코어 `stats/Stats.kt:7-11`의 `seriesStat`이 min/max/avg를 내지만 **막대 layout에는 적용되지 않고**(`BarChartLayout`에 stats 없음, `model/Output.kt:40-44`) 이 규칙도 없습니다. iOS `RDBarChartView.swift:104-109`에 동일 복제.

**막대 높이 clamp + 성장 애니** `RDBarChart.kt:254-255`:
```kotlin
        val fullH = min(max(style.barMinHeight.toDouble(), bar.heightFraction * plot.height), plot.height)
        val h = fullH * growth.coerceIn(0f, 1f)
```
`barMinHeight` clamp는 **코어가 준 `heightFraction`을 렌더러가 덮어쓰는 지점**입니다(가장 빠른 막대는 인코딩된 값과 다른 높이로 그려짐). iOS `RDBarChartView.swift:127` 동일(단 `growth` 없음).

**슬롯 폭·막대 위치** `RDBarChart.kt:251-256`:
```kotlin
    val slot = plot.width / n
    val barWidth = slot * style.barWidthRatio
        val x = plot.minX + slot * i + (slot - barWidth) / 2
```
iOS `RDBarChartView.swift:97-98,128` 동일(비율만 하드코딩 `0.6`).

**Y 위치 — `PlotArea.y`를 안 쓰고 자체 반전** `RDBarChart.kt:209`:
```kotlin
        val y = plot.maxY - tick.position * plot.height
```
`:265` `minY = plot.maxY - h`, `:297` `val y = plot.maxY - refPos * plot.height`.
**주목**: 코어가 이미 `1.0 - dom.normalize(...)`로 반전해서 내보내므로(`BarChartEngine.kt:66,68,69`) 렌더러는 `maxY - position*height`로 매핑합니다. 라인차트가 쓰는 `PlotArea.y(ny, axis)`와 **다른 두 번째 좌표 경로**이고, `invertedAxes`도 안 씁니다(`PlotArea(sizeWidth, sizeHeight, style.plotInsets)` — `:195`, 4번째 인자 생략).

**말풍선 클램프** `RDBarChart.kt:363-366`:
```kotlin
        var bx = midX - bw / 2
        bx = minOf(bx, plot.maxX - bw)
        bx = maxOf(plot.minX, bx)
        val by = plot.minY
```
iOS `RDBarChartView.swift:239-241` `bx = max(plot.minX, min(bx, plot.maxX - bw))` — **연산 순서만 다르고 결과 동일**(주석 `:362`에 `coerceIn(min>max)` 예외 회피 이유 명시).

**미선택 막대 dim** `RDBarChart.kt:335-340`:
```kotlin
        if (layer is RectLayer && layer.name.startsWith("bar.") && layer.name != selName) {
            layer.copy(alpha = layer.alpha * style.barDimOpacity)
```
iOS `RDBarChartView.swift:207-210` `layer.opacity = dim ? base : base * style.barDimOpacity` — **곱셈 구조 동일**.

**라벨 최대 폭 측정** `RDBarChart.kt:162-176` (`maxLabelWidthPx`) — `measureLabelWidthPx` 위임. iOS는 `NSString.size(withAttributes:)` + `ceil`(`RDBarChartView.swift:117-120`), **`prefix(n)` 제한이 iOS에만 있음**(안드로이드는 전체 리스트 `maxOfOrNull`).

### 포맷팅
**Y틱 라벨 폴백 — 정수 반올림** `RDBarChart.kt:222`:
```kotlin
            val text = yLabelFormatter?.invoke(tick.value) ?: tick.value.roundToInt().toString()
```
**로케일 미사용**. iOS 대응은 `RDBarChartView.swift:88-91` 주변.

**TalkBack 요약 — 한국어 하드코딩** `RDBarChart.kt:154-159`:
```kotlin
    if (layout.bars.isEmpty()) return "막대 차트, 데이터 없음"
    val detail = barLabels?.takeIf { it.isNotEmpty() }
        ?.let { labels -> layout.bars.indices.joinToString(", ") { "구간 ${it + 1} ${labels.getOrNull(it) ?: ""}".trim() } }
    return "막대 차트, 구간 ${layout.bars.size}개" + (detail?.let { ". $it" } ?: "")
```
iOS 막대차트에 `accessibilityLabel` 없음 → **Android 전용**.

레이어 이름: `"barGrid.$i"`, `"barYLabel.$i"`, `"bar.$i"`, `"barXLabel.$i"`, `"barRefLine"`, `"bar.selection.line"`, `"bar.selection.bubble"`, `"bar.selection.text"`.

### 상수 (`RDBarChart.kt:392-402`)
```kotlin
private const val BAR_LABEL_GAP = 4.0               // y틱 라벨과 축 사이(iOS insets.left-4)
private const val BAR_X_LABEL_GAP = 4.0             // x축 라벨과 막대 바닥 사이(iOS maxY+4)
private const val BAR_LABEL_MIN_GAP = 6.0           // 솎아낸 이웃 라벨 사이 최소 여백(dp)
private const val BAR_CALLOUT_PAD_H = 8.0           // 선택 말풍선 좌우 내부 여백(dp)
private const val BAR_CALLOUT_PAD_V = 4.0           // 선택 말풍선 상하 내부 여백(dp)
private const val BAR_CALLOUT_CORNER = 6f           // 선택 말풍선 모서리 반경(dp)
private const val BAR_GROWTH_DURATION_MS = 300
internal val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
```
iOS 대조: `BAR_LABEL_GAP` = `RDBarChartView.swift:90` `insets.left - 4` ✅ / `BAR_X_LABEL_GAP` = `:143` `plot.maxY + 4` ✅ / `BAR_LABEL_MIN_GAP` = `:9` `labelMinGap = 6.0` ✅ / `padH/padV` = `:236` `padH: CGFloat = 8, padV: CGFloat = 4` ✅ / 말풍선 코너 = `:249` `cornerRadius: 6` ✅ / **`BAR_GROWTH_DURATION_MS`·`EmphasizedDecelerate` Android 전용**(iOS 정적).
인라인: 참조선 폭 `1f * density`(`:303`) = iOS `line.lineWidth = 1`(`:158`) ✅, 선택 가이드선 폭 `1f * density`(`:351`) = iOS `:227` ✅.

### 제스처/애니메이션
**롱프레스 스크럽** `RDBarChart.kt:101-106`:
```kotlin
            detectDragGesturesAfterLongPress(
                onDragStart = { offset -> scrub(offset.x) },
                onDrag = { change, _ -> change.consume(); scrub(change.position.x) },
                onDragEnd = { setSelection(null, haptic = false) },
                onDragCancel = { setSelection(null, haptic = false) },
```
임계는 Compose 기본(≈500ms, 파라미터 없음). iOS `RDBarChartView.swift:36` `longPress.minimumPressDuration = 0.5` — 근사 일치.

**햅틱** `RDBarChart.kt:86`:
```kotlin
        if (idx != null && haptic) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
```
iOS `UISelectionFeedbackGenerator.selectionChanged()`(`RDBarChartView.swift:15`) — **TextHandleMove ≈ selection 근사**.

**성장 애니**: `animateEntrance: Boolean = false`(`:58`, iOS 정적 패리티), `rememberEntranceProgress(layout, animateEntrance, BAR_GROWTH_DURATION_MS)`(`:63`).

### 코어 API 호출 지점
- `RDBarChart.kt:99` → `barIndexAtX(px, plot.minX, plot.width, layout.bars.size)` (`query/BarHitTest.kt:13`)
- `RDBarChart.kt:205` → `labelStride(n, plot.width, xLabelWidthPx, gap)` (`query/LabelThinning.kt:22`)
- `RDBarChart.kt:275` → `isLabelVisible(i, n, xLabelStride)` (`query/LabelThinning.kt:41`)
- `RDBarChart.kt:261` → `ChartStyle.defaultPaceColor(colorInput)` (렌더러 자체)
- **`BarChartEngine.layout`은 호출하지 않습니다** — 앱이 `layout`을 주입(`:42` 주석 "iOS 비대칭 계약 유지")

### Y축 방향 / Density
반전은 코어가 이미 적용(`heightFraction`·`position`이 반전 값). 렌더러는 `plot.maxY - position*height`. Density: `:65-67` `LocalDensity.current.density` → `scaledForDensity`, 그 외 `density` 곱셈 6곳(`:204`, `:227`, `:282`, `:303`, `:351`, `:358-359`, `:372`).

---

## 13. `RDHeartRateZoneChart.kt` (299줄) — iOS `RDHeartRateZoneView.swift`(257줄)

### 자체 계산 로직

**반경·중심 계산** `RDHeartRateZoneChart.kt:203-207`:
```kotlin
    val ring = style.donutRingWidth
    val radius = (min(width, height) - ring) / 2f
    if (radius <= 0f) return emptyList()
    val cx = width / 2f
    val cy = height / 2f
```
iOS `RDHeartRateZoneView.swift:64-67` 동일.

**fraction → 각도 변환** `RDHeartRateZoneChart.kt:222-223`:
```kotlin
            startAngleDeg = DONUT_START_DEG + FULL_CIRCLE_DEG * seg.startFraction.toFloat(),
            sweepAngleDeg = FULL_CIRCLE_DEG * seg.sweepFraction.toFloat() * progress,
```
iOS는 라디안(`RDHeartRateZoneView.swift:79-80` `start + 2 * .pi * CGFloat(seg.startFraction)`) — **단위만 다르고 동일**.

**도넛 히트테스트 — 각도 역산, 코어 대응 없음** `RDHeartRateZoneChart.kt:267-277`:
```kotlin
    val distance = hypot(px - cx, py - cy)
    val halfBand = hitBandWidth / 2f
    if (distance < radius - halfBand || distance > radius + halfBand) return null

    var angle = kotlin.math.atan2(py - cy, px - cx) + (Math.PI / 2).toFloat() // 12시 기준
    if (angle < 0f) angle += (2 * Math.PI).toFloat()
    val frac = angle / (2 * Math.PI).toFloat()

    val segment = layout.segments.firstOrNull {
        frac >= it.startFraction && frac < it.startFraction + it.sweepFraction
    } ?: return null
```
**코어 대응: 없음.** 막대는 `query/BarHitTest.kt`로 공유했지만 도넛엔 `DonutHitTest`가 없어 "각도 → fraction → 세그먼트 탐색"이 양 플랫폼에 복제되어 있습니다(iOS `RDHeartRateZoneView.swift:243-253` 동일). `sourceIndex` 보고는 코어 규칙 준수(`:278` `segment.sourceIndex.takeIf { it >= 0 }`).

**히트 대역 확장 — Android 전용** `RDHeartRateZoneChart.kt:92`:
```kotlin
    val hitBandPx = max(ringPx, MIN_HIT_TARGET_DP * density)
```
iOS는 시각 링 폭 그대로: `RDHeartRateZoneView.swift:245` `distance >= radius - ring / 2, distance <= radius + ring / 2`. → **얇은 링에서 AOS가 iOS보다 관대하게 탭을 받습니다**(의도적, `:91` 주석 UX Major-3).

**센터 라벨 내접원 제한** `RDHeartRateZoneChart.kt:146-151`:
```kotlin
        val radius = (min(size.width, size.height) - ring) / 2f
        val innerRadius = radius - ring / 2f
        if (innerRadius <= 0f) return@Canvas
        val maxWidthPx = (innerRadius * 2f * 0.9f).toInt().coerceAtLeast(1)
```
iOS `RDHeartRateZoneView.swift:222` `let maxWidth = max(0, (radius - ring / 2) * 2 * 0.9)` — **0.9 배율 일치**.

**센터 2줄 수직 중앙 정렬** `RDHeartRateZoneChart.kt:172-178`:
```kotlin
        val totalH = percentLayout.size.height + (labelLayout?.size?.height ?: 0)
        var top = (size.height - totalH) / 2f
        labelLayout?.let {
            drawText(it, topLeft = Offset((size.width - it.size.width) / 2f, top))
            top += it.size.height
        }
```
iOS는 `percentLabel.font.lineHeight` 기반 프레임 계산(`RDHeartRateZoneView.swift:224-229`) — **측정 방식이 다릅니다**(Android: 실측 layout height, iOS: 폰트 lineHeight).

**비선택 디밍** `RDHeartRateZoneChart.kt:218-220`:
```kotlin
        val color = if (selectedIndex != null && seg.sourceIndex != selectedIndex) {
            base.copy(alpha = style.donutDimmedAlpha)
        } else base
```

**자동 해제 초→ms 환산** `RDHeartRateZoneChart.kt:127`:
```kotlin
    val autoDeselectMs = (scaledStyle.donutAutoDeselectDelaySeconds * 1000f).toLong()
```

### 포맷팅
**센터 퍼센트** `RDHeartRateZoneChart.kt:239`:
```kotlin
    return DonutCenterLines(seg.label, "${(seg.sweepFraction * 100).roundToInt()}%")
```
iOS `RDHeartRateZoneView.swift:205` `"\(Int((seg.sweepFraction * 100).rounded()))%"` — **동일**(`roundToInt` = `.rounded()` + Int). 로케일 미사용.

**TalkBack 요약 — 한국어 + enum 이름 낭독** `RDHeartRateZoneChart.kt:183-189`:
```kotlin
    if (layout.total <= 0.0 || layout.segments.isEmpty()) return "심박존 도넛, 데이터 없음"
    val zones = layout.segments.joinToString(", ") { seg ->
        "${seg.colorRole.name} ${(seg.sweepFraction * 100).roundToInt()}%"
    }
    return "심박존 분포 도넛. $zones"
```
**`colorRole.name`을 그대로 낭독** — `"ZONE1 42%"`처럼 읽힙니다. iOS는 `accessibilityLabel = "심박존 도넛"`(`RDHeartRateZoneView.swift:181`) + 선택 시 `[seg.label, percentText]`(`:214`) — **AOS가 전체 분포를 낭독하고 iOS는 선택분만**. 불일치.

### 상수 (`RDHeartRateZoneChart.kt:293-299`)
```kotlin
private const val DONUT_START_DEG = -90f       // 12시 시작(0°=3시 → −90°)
private const val FULL_CIRCLE_DEG = 360f
private const val MIN_HIT_TARGET_DP = 48f
private const val DONUT_SWEEP_DURATION_MS = 550
```
iOS 대조: `-90°` = `RDHeartRateZoneView.swift:68` `let start = -CGFloat.pi / 2` ✅ / `MIN_HIT_TARGET_DP`·`DONUT_SWEEP_DURATION_MS` **Android 전용**.
인라인: `StrokeCap.Butt`(`:289`, 주석 "butt 필수(round면 세그먼트 겹침)") = iOS `shape.lineCap = .butt`(`:98`) ✅, 센터 폭 배율 `0.9f`(`:150`), `maxLines = 1` + `TextOverflow.Ellipsis`(`:160`, `:169`) = iOS `lineBreakMode = .byTruncatingTail`(`:176`).

### 제스처/애니메이션
**탭** `RDHeartRateZoneChart.kt:106-121` — `detectTapGestures` (임계 파라미터 없음, Compose 기본). iOS는 `touchesEnded` 오버라이드(`RDHeartRateZoneView.swift:102-105`).
**햅틱** `RDHeartRateZoneChart.kt:115-117`:
```kotlin
                    if (next != null && hapticsEnabled) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
```
iOS `UIImpactFeedbackGenerator(style: .light)`(`RDHeartRateZoneView.swift:28`) — **light impact ≈ TextHandleMove 근사**.
**sweep 애니**: `animateEntrance: Boolean = false`(`:77`), 550ms `EmphasizedDecelerate`(`:85`). iOS 정적.

### 코어 API 호출 지점
- `RDHeartRateZoneChart.kt:83` → `DonutEngine.layout(data)` (`DonutEngine.kt:9`)
- `RDHeartRateZoneChart.kt:112` → `DonutEngine.toggleSelection(selection.selectedIndex, tapped)` (`DonutEngine.kt:36`)
- **`HeartRateZoneEngine`은 호출하지 않습니다** — 집계는 앱 책임(`DonutChartData` 주입)

### Y축 방향 / Density
Y축 없음(극좌표). Density: `:88-92` `LocalDensity.current.density` → `scaledForDensity` + `MIN_HIT_TARGET_DP * density`. **주의**: 센터 라벨은 `style`(미스케일)을 쓰고(`:157-158`, `:167`) arc는 `scaledStyle`을 씁니다(`:141`) — sp는 TextMeasurer가 density를 처리하므로 의도된 것이나 두 스타일 객체를 섞어 쓰는 구조.

---

## 14. `DonutSelectionState.kt` (43줄) — iOS `RDHeartRateZoneView.selectSegment(at:)`

### 자체 계산 로직
**선택 가능 인덱스 집합 사전 계산** `DonutSelectionState.kt:41-43`:
```kotlin
    remember(data) {
        DonutSelectionState(null, DonutEngine.layout(data).segments.map { it.sourceIndex }.toSet())
    }
```
`toggle` 가드 `DonutSelectionState.kt:33-34`:
```kotlin
        if (index != null && index !in selectableIndices) return
        selectedIndex = DonutEngine.toggleSelection(selectedIndex, index)
```
iOS `layoutContainsSegment`는 **매 호출 선형 탐색**(`RDHeartRateZoneView.swift:126-128` `currentLayout?.segments.contains { ... }`), 안드로이드는 **Set 사전 계산** — 결과 동일, 성능 다름.

### 코어 API 호출 지점
- `:34` `DonutEngine.toggleSelection` / `:42` `DonutEngine.layout`

### 상수/포맷팅
없음.

### iOS 대비
**구조 차이**: iOS는 뷰 메서드가 햅틱·통지·타이머까지 수행(`RDHeartRateZoneView.swift:136-144`), 안드로이드 홀더는 **상태만** 소유하고 햅틱·통지는 호출부 몫(`:29-31` 주석: "컴포지션 로컬을 홀더가 소유할 수 없다"). → **앱이 `toggle()`로 구동한 변경은 `onSelectSegment`가 안 울립니다**(`RDHeartRateZoneChart.kt:64-66` 문서화) — iOS는 울림. **패리티 차이**.

---

## 15. `EntranceAnimation.kt` (29줄) — iOS 대응 없음 (**Android 전용 파일**)

### 파라미터 값
`EntranceAnimation.kt:17-28`:
```kotlin
internal fun rememberEntranceProgress(trigger: Any?, animate: Boolean, durationMs: Int): State<Float> {
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(trigger, animate) {
        if (animate) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMs, easing = EmphasizedDecelerate))
        } else {
            progress.snapTo(1f)
        }
    }
    return progress.asState()
}
```
- 이징: `EmphasizedDecelerate` = `CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)` (`RDBarChart.kt:402`에 정의)
- 지속시간은 호출부 주입: 바 **300ms**(`RDBarChart.kt:400`), 도넛 **550ms**(`RDHeartRateZoneChart.kt:299`)
- 라인은 **이 헬퍼를 쓰지 않습니다** — `RDLineChart.kt:154-157`이 직접 `Animatable`을 관리(키가 `Unit`이라 재생 조건이 다름: 헬퍼는 `trigger` 교체 시 0부터 재생, 라인은 컴포저블 수명당 1회)
- `snapTo(0f)` 명시(`:22`) — 정착값에서 `animateTo(1f)`가 no-op이 되는 문제 회피

### iOS 대비
iOS는 바·도넛 모두 **정적**(등장 애니 없음). 라인만 `CABasicAnimation(strokeEnd)` 0.6s `.easeOut`(`RDChartView.swift:219-224`). → **바 300ms / 도넛 550ms / EmphasizedDecelerate 이징은 순수 Android 추가**이며 기본값이 `false`로 꺼져 있어 패리티는 유지(`RDBarChart.kt:58`, `RDHeartRateZoneChart.kt:77`).

### 서브시스템
G(애니메이션) 전량.

---

## 16. `LineChartZoomController.kt` (41줄) / 17. `LineChartMarkerController.kt` (46줄)

### 자체 계산 로직
계산 없음 — **요청 봉투 항등성 패턴**만.

`LineChartZoomController.kt:23`:
```kotlin
    internal class Request(val range: ClosedFloatingPointRange<Double>?)
```
`:20-21` 주석: "일부러 equals를 정의하지 않는다(항등성 비교)". `:31` `request = Request(range.start..range.endInclusive)`.

`LineChartMarkerController.kt:31`:
```kotlin
    internal class Request(val rawX: Double?)
```

### 코어 API / 상수 / 포맷팅
전부 없음.

### iOS 대비
**Android 전용 구조**. iOS는 명령형 메서드 직접 호출(`RDChartView.swift:232` `showTouchMarker(atX:)`, `zoom(toXRange:)`) — Compose 선언형에서 "같은 값 재요청"을 관측하려고 도입한 어댑터 계층입니다. 소비 지점: `RDLineChart.kt:137-145`(zoom), `:182-199`(marker).

### 서브시스템
E(상호작용).

---

## 18. `ChartCallbacks.kt` (27줄) — iOS 델리게이트 프로토콜

typealias 4개만. 계산·상수·포맷팅 없음.
`ChartCallbacks.kt:13,16,19,27`:
```kotlin
typealias OnScrub = (values: Map<String, String>) -> Unit
typealias OnScrubBackground = (value: Double) -> Unit
typealias OnScrubEnd = () -> Unit
typealias OnSelectSegment = (index: Int?) -> Unit
```
**주목**: `OnScrub`이 `Map<String, String>` — **이미 포맷된 문자열**을 앱에 전달합니다. 앱이 실값을 못 받으므로 렌더러의 포매터가 값 표현의 최종 결정권을 갖습니다(`OnScrubBackground`만 `Double` 실값).

---

# 서브시스템 분류표 (기능 단위 전수)

## A — 데이터 입력·검증·정규화·다운샘플링

| 기능 | 위치 | 코어 대응 |
|---|---|---|
| area x 오름차순 정렬 | `RDLineChart.kt:105` | 없음 (계약만 `AreaInterpolation.kt:7`) |
| 실루엣 높이 0~1 정규화 위임 | `AreaSilhouette.kt:24-25` | **있음** `query/HeightFractions.kt:13` |
| 시리즈 id → 축/역할 "첫 우선" 매핑 | `LineChartDrawing.kt:223-232`, `LineChartInteraction.kt:53-54` | 없음(코어가 유일성 미강제) |
| 2점 미만 시리즈 제외 | `LineChartDrawing.kt:242,248`; `AreaSilhouette.kt:40` | 없음 |
| 막대 색 앵커 축약(fastest/slowest/average, 부분 배제·2개 미만 폴백) | `RDBarChart.kt:243-248` | **없음** — `stats/Stats.kt`가 유사 기능이나 미연결 |
| 도넛 선택 가능 인덱스 집합 산출 | `DonutSelectionState.kt:42` | 없음(`DonutEngine.layout` 결과 후처리) |
| 다운샘플링 | **렌더러에 없음** | `PaceSeriesEngine.kt`(렌더러 미호출) |

## B — 스케일·틱·축라벨

| 기능 | 위치 | 코어 대응 |
|---|---|---|
| tick 선형관계 재구성(기울기) | `AxisScale.kt:35-44` | 없음 |
| 정규화 위치 → 도메인 값 | `AxisScale.kt:27-28` | 없음(`AxisDomain.normalize` 역함수 부재) |
| 도메인 값 → 정규화 위치 | `AxisScale.kt:30-31` | `AxisDomain.normalize`와 중복 개념 |
| 축별 tick 조회 | `AxisScale.kt:12-13` | 없음 |
| 전체 X 도메인 역산(줌 초기화, 외삽 포함) | `LineChartInteraction.kt:222-227` | **없음** — 코어가 `AxisDomain` 미출력 |
| 그리드 Y축 폴백(primary→secondary) | `LineChartDrawing.kt:271-280` | 없음 |
| Y 역산으로 도트 y 산출 | `TouchMarker.kt:100-104` | 없음(코어 정규화값 재활용 불가) |
| 오버레이 정규화 공간 재-최근접 | `TouchMarker.kt:154` | 없음 |
| x축 라벨 솎아내기 stride | `RDBarChart.kt:205` | **있음** `query/LabelThinning.kt:22` |
| 라벨 표시 여부(첫·마지막 강제) | `RDBarChart.kt:275` | **있음** `query/LabelThinning.kt:41` |
| 라벨 폭 측정(stride 입력) | `RDBarChart.kt:162-176`, `LineChartDrawing.kt:699-714` | 없음(플랫폼 텍스트 측정) |
| 도넛 fraction → 각도(deg) | `RDHeartRateZoneChart.kt:222-223` | 없음 |
| 줌 창 산술(핀치·팬·클램프·maxScale) | `ZoomState.kt:34-77` | **없음** |

## C — 좌표변환·레이아웃

| 기능 | 위치 | 코어 대응 |
|---|---|---|
| 플롯 사각형 파생(insets) | `PlotArea.kt:25-29` | 없음(타당) |
| x: 정규화 → 픽셀 | `PlotArea.kt:37` | 없음(타당) |
| **y: 정규화 → 픽셀 + 축 반전** | `PlotArea.kt:39-42` | 없음(렌더러 책임 명문) |
| y 반전 무시(오버레이) | `PlotArea.kt:51-55` | 없음 |
| 픽셀 x → 정규화 x + 클램프 | `PlotArea.kt:58-61` | 없음 |
| renderable 판정 | `PlotArea.kt:35`; `RDBarChart.kt:196,330`; `RDHeartRateZoneChart.kt:140` | 없음 |
| **막대 y 매핑(`maxY - pos*height`) — PlotArea.y 우회** | `RDBarChart.kt:209,265,297` | 없음 — **두 번째 좌표 경로** |
| 막대 슬롯·폭·x 위치 | `RDBarChart.kt:251-256` | 없음 |
| 막대 최소높이 clamp | `RDBarChart.kt:254` | 없음(코어 heightFraction 덮어씀) |
| 밴드 사각형 min/abs 정렬 | `LineChartDrawing.kt:155-163` | 없음 |
| 실루엣 픽셀 매핑(바닥 기준, 0~1 클램프) | `AreaSilhouette.kt:42-48` | 없음 |
| 실루엣/그라데이션 폴리곤 닫기 | `AreaSilhouette.kt:50-55`; `LineChartDrawing.kt:341-345` | 없음 |
| 텍스트 앵커+정렬 → 원점 | `LineChartDrawing.kt:672-681` | 없음 |
| 말풍선 크기·좌우 클램프·상단 고정 | `RDBarChart.kt:358-366` | 없음 |
| 도넛 반경·중심 | `RDHeartRateZoneChart.kt:203-207,264` | 없음 |
| 도넛 센터 내접원 폭 제한(0.9) | `RDHeartRateZoneChart.kt:146-151` | 없음 |
| 도넛 센터 2줄 수직 중앙 | `RDHeartRateZoneChart.kt:172-178` | 없음 |
| 확대 클립 사각형(top=0) | `LineChartDrawing.kt:491-498` | 없음 |
| Path 조립(polyline/polygon) | `LineChartDrawing.kt:630-647` | 없음 |
| 정적 레이어·Path 캐시 | `LineChartDrawing.kt:513-565` | 없음 (**Android 전용**) |

## D — 스타일 상수

| 기능 | 위치 |
|---|---|
| 스타일 기본값 전량(위 표 33행) | `ChartStyle.kt:37-131` |
| 라이트/다크 색 팔레트 2세트(24색 쌍) | `ChartStyle.kt:141-203` |
| dp→px 밀도 환산(11개 필드) | `ChartStyle.kt:223-242` (**Android 전용**) |
| 헤어라인 하한 1px | `ChartStyle.kt:135,228` (**Android 전용**) |
| 라벨 여백 상수 4개 + 글꼴 상한 | `LineChartDrawing.kt:717-722` |
| 막대 여백·말풍선 상수 6개 | `RDBarChart.kt:392-397` |
| 도넛 각도·히트타겟 상수 3개 | `RDHeartRateZoneChart.kt:293-296` |
| 터치선 폭·epsilon | `TouchMarker.kt:188-189` |
| 컬러맵 앵커 비율(0.70/0.25/0.4) | `PaceColormap.kt:27,34` |
| 시리즈 색 리졸버(맵→역할→축) | `LineChartDrawing.kt:235-239` |
| 그라데이션 √n 알파 감쇠 | `LineChartDrawing.kt:180` |
| 비선택 디밍 배율 적용 | `RDBarChart.kt:337`; `RDHeartRateZoneChart.kt:219` |
| 부분 스플릿 알파 적용 | `RDBarChart.kt:271` |
| 3구간 페이스 색 보간 | `PaceColormap.kt:26-44` (**분류 애매 — 값→색 매핑**) |
| StrokeCap/Join 선택 | `LineChartDrawing.kt:195,205-206`; `RDHeartRateZoneChart.kt:289` |

## E — 상호작용

| 기능 | 위치 | 코어 대응 |
|---|---|---|
| 제스처 임계값 조회(slop·롱프레스·더블탭) | `LineGestures.kt:49-51` | 없음 |
| 가로 우세 판정 | `LineGestures.kt:31` | 없음 |
| 픽셀 → 도메인 x | `LineGestures.kt:63-68` | 없음 |
| 제스처 의미 해석 상태머신(핀치/롱프레스/드래그/탭 4분기) | `LineGestures.kt:106-157` | 없음 |
| 스크럽 루프 + finally endScrub | `LineGestures.kt:169-195` | 없음 |
| 팬 비율(분모=플롯폭) | `LineGestures.kt:220-221` | 없음 |
| 핀치 누적 배율·centroid 앵커 | `LineGestures.kt:245-248` | 없음 |
| 더블탭 근사(타임아웃 창) | `LineGestures.kt:258-266` | 없음 |
| consume 정책 3곳 | `LineGestures.kt:187,216,250` | 없음 |
| 근접 판정 위임 + 창 epsilon 확장 | `TouchMarker.kt:64-69` | **있음** `query/Nearest.kt:14` |
| 스냅 소스 선택(main 우선) | `TouchMarker.kt:73-75` | **없음** |
| 창 밖 마커/도트 생략 + 클램프 | `TouchMarker.kt:77-79,87-88` | 없음 |
| 배경 area 보간 위임 | `LineChartInteraction.kt:134` | **있음** `query/AreaInterpolation.kt:10` |
| 스크럽 창 경계 epsilon 클램프 | `LineChartInteraction.kt:104-109` | 없음 |
| 배경 단독 폴백 판정 | `LineChartInteraction.kt:114-118` | 없음 |
| 콜백 짝맞춤 불변식 | `LineChartInteraction.kt:120-127,143-148` | 없음 |
| "비줌=null" 정규화 3곳 | `LineChartInteraction.kt:161,187,210` | 없음 |
| 줌 창 → layout 분기 | `LineChartInteraction.kt:73-81` | 없음 |
| 막대 히트테스트 위임 | `RDBarChart.kt:99` | **있음** `query/BarHitTest.kt:13` |
| 막대 선택 no-op 가드·범위 가드 | `RDBarChart.kt:84,114,138` | 없음 |
| **도넛 히트테스트(반경+각도 역산)** | `RDHeartRateZoneChart.kt:261-278` | **없음** (BarHitTest만 존재) |
| 히트 대역 48dp 확장 | `RDHeartRateZoneChart.kt:92` | 없음 (**Android 전용**) |
| 도넛 토글 전이 위임 | `RDHeartRateZoneChart.kt:112`; `DonutSelectionState.kt:34` | **있음** `DonutEngine.kt:36` |
| 도넛 자동 해제 타이머 | `RDHeartRateZoneChart.kt:127-134` | 없음 |
| 햅틱 3종 매핑 | `LineGestures.kt:113`; `RDBarChart.kt:86`; `RDHeartRateZoneChart.kt:116` | 없음 |
| 명령형 요청 봉투(줌·마커) | `LineChartZoomController.kt:23`; `LineChartMarkerController.kt:31` | 없음 (**Android 전용**) |

## F — 포맷팅

| 기능 | 위치 | 로케일 API | iOS 대응 |
|---|---|---|---|
| **`defaultLineChartFormatter`** (`%g` 재구현: 유효숫자 6, 트레일링 0 제거, 지수 표기, nan/inf) | `RDLineChart.kt:45-61` | `Locale.ROOT` **고정(회피)** | `RDChartView.swift:530` 1줄 |
| `trimTrailingZeros` | `RDLineChart.kt:60-61` | — | 없음(Swift `%g`가 처리) |
| 막대 y틱 폴백 = `roundToInt().toString()` | `RDBarChart.kt:222` | 미사용 | `RDBarChartView.swift:88` 부근 |
| 도넛 센터 퍼센트 `"${roundToInt()}%"` | `RDHeartRateZoneChart.kt:239` | 미사용 | `RDHeartRateZoneView.swift:205` 동일 |
| 도넛 TalkBack(`colorRole.name` + %) | `RDHeartRateZoneChart.kt:185-188` | 하드코딩 한국어 | `:181,214` **다름** |
| 라인 TalkBack | `RDLineChart.kt:257-261` | 하드코딩 한국어 | **없음** |
| 막대 TalkBack(구간별 값) | `RDBarChart.kt:155-158` | 하드코딩 한국어 | **없음** |
| 주입 포매터 호출 3곳 | `LineChartDrawing.kt:365`; `TouchMarker.kt:94,120` | — | 동일 |
| **페이스 `mm:ss`** | **SDK에 없음** — 앱이 `labelFormatter`/`barLabels` 주입 | — | 동일(없음) |
| 레이어 이름 문자열(테스트 계약) | `LineChartDrawing.kt`·`RDBarChart.kt` 전역 | — | CALayer name 동일 |

## G — 애니메이션

| 기능 | 위치 | 값 | iOS |
|---|---|---|---|
| 공용 등장 진행도 헬퍼 | `EntranceAnimation.kt:17-28` | `snapTo(0f)` → `animateTo(1f, tween(d, EmphasizedDecelerate))` | 없음 |
| `EmphasizedDecelerate` 이징 | `RDBarChart.kt:402` | `CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)` | `.easeOut`(라인만) **불일치** |
| 라인 등장(자체 관리, 키=`Unit`) | `RDLineChart.kt:37,154-157` | **600ms**, 기본 on | 0.6s ✅ / easeOut ✗ |
| Path 트림(진행률 → 길이비율) | `LineChartDrawing.kt:584,649-655` | `measure.length * progress` | `strokeEnd` 0→1(프레임워크) |
| 막대 성장 | `RDBarChart.kt:63,400`; 적용 `:255` | **300ms**, 기본 off | **없음(정적)** |
| 도넛 sweep | `RDHeartRateZoneChart.kt:85,299`; 적용 `:223` | **550ms**, 기본 off | **없음(정적)** |
| 선택 변경 즉시 반영(애니 없음) | `RDBarChart.kt:140-148` | — | `CATransaction.setDisableActions(true)` (`RDBarChartView.swift:203`) |

---

# Y축 방향·좌표계 요약 (요청 6번 별도 정리)

**정식 반전 지점 1곳**: `PlotArea.kt:39-42` — `fractionFromTop = if (invertedAxes.contains(axis)) ny else 1.0 - ny`. 라인차트 전 경로(`LineChartDrawing.kt:155-156,282,383,395`, `TouchMarker.kt:101`)가 이것만 사용.

**반전 무시 경로 1곳**: `PlotArea.kt:51` `yIgnoringInversion` — 오버레이 전용(코어가 자체 정규화, 축 없음). 사용: `LineChartDrawing.kt:250`, `TouchMarker.kt:155`.

**우회 경로 2곳** (아키텍처상 문제):
1. `RDBarChart.kt:209,265,297` — `plot.maxY - position * plot.height`. `PlotArea`를 `invertedAxes` 없이 생성(`:195`, `:329`)하고 코어가 이미 `1.0 - normalize`로 반전해 보낸 값(`BarChartEngine.kt:66,68,69`)을 씁니다. **반전이 코어와 렌더러에 나뉘어 있습니다**.
2. `AreaSilhouette.kt:47` — `baseY - fractions[index] * usableHeight`. 주석(`:31`)에 "축 반전 무관(자체 매핑) — `PlotArea.y`는 쓰지 않는다" 명시.

**Density 환산 지점 전수**:
- 스타일 일괄: `ChartStyle.kt:223-242` ← 호출 `RDLineChart.kt:123`, `RDBarChart.kt:67`, `RDHeartRateZoneChart.kt:89`
- 스타일 밖 상수 개별 곱셈: `LineChartDrawing.kt:309,318,371,382,394`(마커 폭·라벨 여백), `TouchMarker.kt:175`(터치선), `RDBarChart.kt:204,227,282,303,351,358-359,372`, `RDHeartRateZoneChart.kt:92`
- `density` 획득: `RDLineChart.kt:122`, `RDBarChart.kt:65-66`(fontScale 포함), `RDHeartRateZoneChart.kt:88`, `DrawScope.density`(`LineChartDrawing.kt:451,473`)
- **iOS엔 이 계층이 전혀 없습니다** — 안드로이드 전용 어댑터 층이며, 단위테스트는 density=1 전제로 iOS 수치와 일치시킵니다(`ChartStyle.kt:215-218` 주석).
