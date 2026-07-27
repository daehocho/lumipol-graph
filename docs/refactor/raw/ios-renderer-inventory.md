# iOS 렌더러 경계 인벤토리 (`ios-renderer/Sources/LumipolGraphUI/`, 12파일 1,903줄)

기준 커밋: `0ad1ddf` (0.29.0 계열). 모든 경로는 `/Users/daeho/lumipol-graph/ios-renderer/Sources/LumipolGraphUI/` 기준으로 파일명만 표기.
코어 대조 대상: `/Users/daeho/lumipol-graph/core/src/commonMain/kotlin/com/lumipol/graph/`.

## 총평 (먼저)

렌더러는 이미 상당 부분 코어에 위임하고 있습니다(`nearest`, `interpolatedY`, `heightFractions`, `labelStride`/`isLabelVisible`, `barIndexAtX`, `toggleSelection`). 그럼에도 **"값을 고르거나 만들어내는" 로직이 6군데 남아 있고**, 그중 4개는 코어에 대응 구현이 아예 없습니다:

1. `AxisScale` — tick 2개에서 축 도메인을 역산하는 **역스케일 재구성**. 코어는 `AxisDomain.normalize`만 노출하고 역함수가 없어서 렌더러가 자체 재구성한다. 렌더러 전체 상호작용(스크럽·줌·실루엣 x배치)이 이 추정에 얹혀 있다.
2. `ZoomState` 전체 — 줌/팬 도메인 창 계산(63줄). 코어에 대응 없음.
3. `PaceColormap.defaultPaceColor` — 3구간 연속 색 보간 공식 전체 + `RDBarChartView`의 색 앵커 통계(fastest/slowest/average) 산출. 코어에 대응 없음.
4. `RDHeartRateZoneView.segmentIndex(at:)` — 도넛 각도/반경 히트테스트. 코어에 `toggleSelection`은 있지만 히트테스트는 없음.
5. **Y축 반전이 3곳에 독립 구현**되어 있음 (`PlotArea.y`, `RDBarChartView.redraw`, `AreaSilhouette.layer`). 셋이 같은 규칙을 각자 쓴다.
6. `RDBarChartView`는 `PlotArea`를 아예 쓰지 않고 `bounds.inset(by:)`로 자체 플롯 사각형을 만든다 — 라인차트와 좌표 경로가 분리돼 있다.

또한 `ChartStyle.barColors`(ChartStyle.swift:48-52)는 **어느 렌더러 파일에서도 읽히지 않는 죽은 기본값**입니다.

---

## 1. RDChartView.swift (558줄) — 라인차트 호스트 뷰

### 1-A. 자체 계산 로직

| 위치 | 코드 | 코어 대응 |
|---|---|---|
| `RDChartView.swift:244` | `let epsilon = (state.window.upperBound - state.window.lowerBound) * 1e-9` | **없음.** 줌 창 경계 관용 오차를 렌더러가 정의. `TouchMarker`의 고정 `1e-9`와 다른 스케일 상대 epsilon이라 두 곳의 관용도가 실제로 다르다 |
| `RDChartView.swift:247` | `rawX = min(max(rawX, state.window.lowerBound), state.window.upperBound)` | 없음. 도메인 클램프 |
| `RDChartView.swift:322-325` | `let lower = scale.value(atPosition: 0)` / `let upper = scale.value(atPosition: 1)` / `zoomState = ZoomState(fullDomain: lower...upper)` | **없음.** 전체 X 도메인을 tick 역산으로 복원. 코어 `LineChartEngine.layout`은 `xDom`을 알고 있지만 `LineChartLayout`에 내보내지 않아 렌더러가 추정해야 한다 (구조적 누락) |
| `RDChartView.swift:423-427` | `let span = start.upperBound - start.lowerBound` / `let fraction = Double(recognizer.translation(in: self).x / plotArea.rect.width)` / `let targetLower = start.lowerBound - fraction * span` | **없음.** 팬 픽셀→도메인 환산. `ZoomState.pan(byFraction:)`이 있는데도 여기서 직접 `setWindow`를 호출한다 (`ZoomState.swift:41` 미사용 경로) |
| `RDChartView.swift:376` | `let anchor = plotArea.normalizedX(at: recognizer.location(in: self).x)` | 없음. 핀치 앵커 픽셀→정규화 |
| `RDChartView.swift:513-520` | `AxisScale(ticks: xTicks)` 캐시 구성. tick 부족 시 nil → 스크럽 무음 비활성 | **부분.** `AxisDomain`이 정방향만 있음 |
| `RDChartView.swift:525` | `let rawX = xScale.value(atPosition: plotArea.normalizedX(at: location.x))` | 없음. 터치 픽셀 → 원본 도메인 x |
| `RDChartView.swift:133` | `self.backgroundArea = backgroundArea?.sorted { $0.x < $1.x }` | **없음.** 입력 정규화(정렬)를 렌더러가 수행 — 코어 `interpolatedY`의 오름차순 전제를 렌더러가 보증 |
| `RDChartView.swift:358-361` | `CGRect(x: plotArea.rect.minX, y: 0, width: plotArea.rect.width, height: plotArea.rect.maxY)` | 없음. 클립 마스크 기하 (위쪽만 뷰 상단까지 개방) |
| `RDChartView.swift:439-441` | `abs(translation.x) > abs(translation.y)` | 없음. 제스처 방향 우세 판정 |
| `RDChartView.swift:104-108, 162-165` | `RebuildKey(bounds:layout:)` — `ObjectIdentifier(chartLayout)` 동일성으로 재구축 스킵 | 없음. 순수 렌더러 캐시 정책 |

### 1-B. 포맷팅

```swift
// RDChartView.swift:529-531
static func defaultFormatter(_ axis: ChartAxis, _ value: Double) -> String {
    String(format: "%g", value)
}
```

`%g` = C 표준. **유효숫자 6자리, 지수/고정 자동 선택, 후행 0 제거, 로케일 미적용**(`String(format:)`은 `Locale` 인자 없이 POSIX 소수점 `.` 고정). 즉 `1234567` → `1.23457e+06`, `0.5` → `0.5`. `NumberFormatter` 미사용. 축 종류(`ChartAxis`)를 인자로 받지만 무시한다. mm:ss·단위 포맷은 전부 앱 주입(`labelFormatter`)이고 렌더러엔 mm:ss 구현이 없다.

### 1-C. 상수/기본값

| 위치 | 값 | 의미 |
|---|---|---|
| `RDChartView.swift:26` | `isAnimationEnabled = true` | 등장 애니 기본 on |
| `RDChartView.swift:38` | `isZoomEnabled = false` | 줌 기본 off |
| `RDChartView.swift:51` | `maxZoomScale: CGFloat = 10` | 최대 확대 10x |
| `RDChartView.swift:223` | `animation.duration = 0.6` | strokeEnd 등장 0.6s |
| `RDChartView.swift:224` | `CAMediaTimingFunction(name: .easeOut)` | 커브 |
| `RDChartView.swift:244` | `1e-9` | 창 경계 상대 epsilon |
| `RDChartView.swift:446` | `pan.maximumNumberOfTouches = 1` | |
| `RDChartView.swift:460` | `doubleTapRecognizer.numberOfTapsRequired = 2` | |
| `RDChartView.swift:465` | `longPressRecognizer.minimumPressDuration = 0.5` | 스크럽 진입 0.5s |
| `RDChartView.swift:498` | `UIImpactFeedbackGenerator(style: .medium)` | 확대 스크럽 진입 햅틱 |

### 1-D. 코어 API 호출 지점

- `RDChartView.swift:147` — `LineChartEngine.shared.layout(data:backgroundArea:)` (전체 구간)
- `RDChartView.swift:332-336` — `LineChartEngine.shared.layout(data:xMin:xMax:)` (줌 커밋)
- `RDChartView.swift:279-280` — `LineChartEngine.shared.interpolatedY(points:x:)` (배경 area 스크럽 실값)

그 외 코어 심볼 호출 없음. **`niceScale`·`SeriesSelection`·`PaceSeriesEngine`은 렌더러에서 전혀 호출되지 않는다**(앱 책임).

### 1-E. 제스처/애니메이션

인식 파라미터: `installGestures()` `RDChartView.swift:443-469`. 의미 해석은 세 갈래로 갈린다.

- **분기 결정**: `handleGesture` `RDChartView.swift:471-488` — `isScrubbing`이면 팬 무시(473), `zoomState?.isZoomed == true || panStartWindow != nil`이면 팬 핸들러(475-479), 팬 종료면 마커 제거(482-486), 그 외 `scrub(at:)`.
- **"어느 포인트인가" 판정**: `scrub(at:)` `RDChartView.swift:523-527`이 픽셀→rawX만 만들고, 실제 포인트 선택은 `TouchMarker.make` → 코어 `nearest`.
- **동시 인식 정책**: `RDChartView.swift:539-547` (확대 팬은 차트 독점), `549-557` (확대 시 가로 우세만 시작).
- 애니메이션: `animateMainLines()` `RDChartView.swift:217-227` — `series.main.` 프리픽스 레이어만 `strokeEnd` 0→1. 최초 render 1회(`139-140`).

### 1-F. Y축 방향

이 파일엔 Y 변환이 없다. 전부 `PlotArea`(178에서 `invertedAxes` 주입)로 위임.

---

## 2. RDBarChartView.swift (299줄) — 스플릿 막대

### 2-A. 자체 계산 로직

| 위치 | 코드 | 코어 대응 |
|---|---|---|
| `RDBarChartView.swift:71, 177, 216` | `let plot = bounds.inset(by: insets)` | **`PlotArea`를 안 쓴다.** 라인차트와 별도 좌표 경로 — 동일 기능 2중 구현 |
| `RDBarChartView.swift:76` | `let y = plot.maxY - CGFloat(tick.position) * plot.height` | **Y 반전 독립 구현 #2.** `PlotArea.y`와 같은 규칙을 다시 씀. 코어 `BarChartEngine`이 이미 `1.0 - dom.normalize`로 반전해 놨으므로(`BarChartEngine.kt:66-69`) 여기 `maxY - pos*h`는 "bottom-up" 매핑 |
| `RDBarChartView.swift:97-98` | `let slot = plot.width / CGFloat(n)` / `let barWidth = slot * 0.6` | **없음.** 막대 슬롯 폭·막대 폭 비율(0.6)이 렌더러 결정. 코어 `barIndexAtX`는 같은 슬롯 수학을 갖고 있지만 barWidth는 모른다 → 히트 슬롯과 시각 막대의 폭이 코어/렌더러로 나뉘어 있다 |
| `RDBarChartView.swift:104-109` | `let fullValues = layout.bars.filter { !$0.isPartial }.map { $0.value }` / `fullHasRange = fullValues.count >= 2 && fullValues.max()! > fullValues.min()!` / `let anchorValues = fullHasRange ? fullValues : layout.bars.map { $0.value }` / `fastest = min`, `slowest = max`, `average = reduce(0,+)/count` | **없음. 최대 위반 후보.** 색 앵커 통계(부분 스플릿 제외 규칙 + 축퇴 폴백)를 렌더러가 계산. 코어엔 `Stats`/`seriesStat`(라인용)만 있고 막대용 집계 통계 출력이 없다. Android와 규칙이 갈릴 수밖에 없는 지점 |
| `RDBarChartView.swift:114-123` | `maxW = max(maxW, ceil((s as NSString).size(withAttributes: labelAttrs).width))` | 폭 **측정**은 플랫폼 고유(정당). stride 계산은 코어 위임(121) |
| `RDBarChartView.swift:127` | `let h = min(max(style.barMinHeight, CGFloat(bar.heightFraction) * plot.height), plot.height)` | **없음.** 최소 가시 높이 클램프 — 코어 `heightFraction`을 렌더러가 보정한다(값 보정) |
| `RDBarChartView.swift:128-129` | `let x = plot.minX + slot * CGFloat(i) + (slot - barWidth) / 2` | 없음. 슬롯 내 센터링 |
| `RDBarChartView.swift:150-151` | `let refPos = CGFloat(truncating: refBox)` / `let y = plot.maxY - refPos * plot.height` | Y 반전 #2 재사용 |
| `RDBarChartView.swift:207-209` | `let base: Float = layout.bars[i].isPartial ? style.barPartialOpacity : 1.0` / `layer.opacity = dim ? base : base * style.barDimOpacity` | 없음. 알파 합성 규칙(부분 × 디밍 곱) |
| `RDBarChartView.swift:236-244` | `padH: CGFloat = 8, padV: CGFloat = 4` / `bx = max(plot.minX, min(bx, plot.maxX - bw))` / `let by = plot.minY` | 없음. 말풍선 기하 + 좌우 클램프(좌측 우선) |
| `RDBarChartView.swift:279-285` | `LabelAlign` 4종 origin 보정 | 없음. 텍스트 정렬(플랫폼 고유로 봐도 무리 없음) |

### 2-B. 포맷팅

```swift
// RDBarChartView.swift:271-273
private func yTickLabel(_ value: Double) -> String {
    String(Int(value.rounded()))  // ...y틱은 원값(초)만
}
```

`Double.rounded()` = **schoolbook rounding (half away from zero)**, `Int(_:)`로 절단 후 `String(Int)` — 로케일 미적용(천 단위 구분 없음), 0패딩 없음. 음수/거대값 시 `Int` 이니셜라이저가 트랩할 수 있다(NaN·범위 초과 → 크래시). `yLabelFormatter` 주입 시 대체(`89`).

나머지 문자열은 전부 앱 주입: `barLabels`(`RDBarChartView.swift:44`, 말풍선 텍스트 `232`), `xAxisLabels`(`45`, x축 라벨 `144`). **mm:ss 페이스 포맷은 렌더러에 없다.**

### 2-C. 상수/기본값

| 위치 | 값 |
|---|---|
| `RDBarChartView.swift:9` | `labelMinGap = 6.0` (Android `BAR_LABEL_MIN_GAP`과 동일 명시) |
| `RDBarChartView.swift:36` | `longPress.minimumPressDuration = 0.5` |
| `RDBarChartView.swift:90` | `x: insets.left - 4` (y라벨 오른쪽 여백 4) |
| `RDBarChartView.swift:98` | `slot * 0.6` (막대 폭 비율) |
| `RDBarChartView.swift:143` | `let baseline = plot.maxY + 4` |
| `RDBarChartView.swift:158` | 참조선 `line.lineWidth = 1` |
| `RDBarChartView.swift:227` | 선택 가이드선 `lineWidth = 1` |
| `RDBarChartView.swift:236` | `padH = 8`, `padV = 4` |
| `RDBarChartView.swift:250` | 말풍선 `cornerRadius: 6` |
| `RDBarChartView.swift:15, 170, 188` | `UISelectionFeedbackGenerator` + `prepare()`/`selectionChanged()` |

### 2-D. 코어 API 호출 지점

- `RDBarChartView.swift:121-122` — `LabelThinningKt.labelStride(count:plotWidthPx:labelWidthPx:gapPx:)`
- `RDBarChartView.swift:142` — `LabelThinningKt.isLabelVisible(index:count:stride:)`
- `RDBarChartView.swift:266-268` — `BarHitTestKt.barIndexAtX(x:plotMinX:plotWidth:count:)`

**`BarChartEngine.layout`은 호출하지 않는다** — `render(_ layout: BarChartLayout, ...)`(`41`)로 앱이 만든 레이아웃을 받는다. 라인차트(`RDChartView`가 직접 `LineChartEngine.layout` 호출)와 계약이 비대칭이다.

### 2-E. 제스처

- 인식: `installGestures()` `RDBarChartView.swift:34-39` — 롱프레스 0.5s만. 팬/핀치/탭 없음.
- 의미 해석: `handleLongPress` `185-197`(began/changed → `scrub`), `scrub(at:)` `175-183`(플롯 폭 확인 → 코어 `barIndexAtX`), `selectBar(at:)` `167-172`(동일 인덱스면 무시 + 햅틱).
- `barLabels?.isEmpty == false` 가드(`176`) — **라벨 없으면 선택 자체가 안 된다**. 표시 데이터 유무가 상호작용 가능성을 좌우하는 숨은 결합.
- 동시 인식 무조건 허용: `295-298`.

### 2-F. Y축 방향

`RDBarChartView.swift:76`, `127`, `129`, `151` — 전부 `plot.maxY - (position|heightFraction) * plot.height`. 즉 **"0=바닥, 1=천장" bottom-up 고정**이고 `invertedAxes` 개념이 없다. 페이스 반전은 코어 `BarChartEngine`이 `1.0 - normalize`로 이미 처리(`BarChartEngine.kt:66-69`). 라인차트(`PlotArea.y`)는 `1.0 - ny`가 기본이고 반전축만 `ny` — **두 차트의 반전 책임 위치가 정반대**다.

---

## 3. ChartLayerBuilder.swift (275줄) — 레이어 조립

### 3-A. 자체 계산 로직

| 위치 | 코드 | 코어 대응 |
|---|---|---|
| `ChartLayerBuilder.swift:17` | `Dictionary(data.series.map { ($0.id, $0.axis) }, uniquingKeysWith: { first, _ in first })` | **없음.** 시리즈 id 중복 시 "첫 시리즈 우선" 규칙을 렌더러가 정함. 코어가 유일성을 강제하지 않아 생긴 틈 (`TouchMarker.swift:56-61`에 같은 규칙 재구현 — 2곳 중복) |
| `ChartLayerBuilder.swift:70` | `guard series.points.count >= 2 else { return nil }` | 없음. 1점 시리즈 미표시 임계값 |
| `ChartLayerBuilder.swift:41` | `let alpha = style.gradientMaxAlpha / CGFloat(n).squareRoot()` | **없음.** 그라데이션 알파 √n 감쇠 — 값 생성. `n`은 "그릴 수 있는 시리즈 수"(`39`)로 실제 겹침이 아니다(`ChartStyle.swift:13-16`에 한계 자체가 문서화됨) |
| `ChartLayerBuilder.swift:87-91` | `if let c = style.seriesColors[id] { return c }` / `if role == .overlay { return style.overlayLineColor }` / `return axis == .secondary ? style.secondaryLineColor : style.primaryLineColor` | 없음. 색 폴백 체인(맵 → 역할 → 축) |
| `ChartLayerBuilder.swift:135-138` | `areaPath.addLine(to: CGPoint(x: linePoints[count-1].x, y: plotArea.rect.maxY))` … `.close()` | 없음. area 닫는 변 |
| `ChartLayerBuilder.swift:139-141` | `CGAffineTransform(translationX: -plotArea.rect.minX, y: -plotArea.rect.minY)` | 없음. 마스크 좌표계 이동 |
| `ChartLayerBuilder.swift:161-163` | `let (yTicks, yAxis) = primaryTicks.isEmpty ? (ticks(.ySecondary), .secondary) : (primaryTicks, .primary)` | **없음.** "가로 그리드를 어느 축 tick으로 그릴까"를 렌더러가 고른다 — 값 선택 결정 |
| `ChartLayerBuilder.swift:190-192` | `y: min(y1, y2)`, `height: abs(y1 - y2)` | 없음. 반전축에서 lower/upper가 뒤집히는 것을 흡수 |
| `ChartLayerBuilder.swift:271-272` | `(text as NSString).size(withAttributes:)` + `ceil` | 텍스트 측정 — 플랫폼 고유(정당) |
| `ChartLayerBuilder.swift:5, 20-60` | z-순서 결정 | 순수 렌더러(정당) |

### 3-B. 포맷팅

자체 문자열 생성 없음. 전부 주입 포매터 통과:

- `ChartLayerBuilder.swift:232` — `formatter(ticksLayout.axis, tick.value)` (축 라벨)
- `ChartLayerBuilder.swift:212` — `marker.label`을 그대로 사용(코어 제공 문자열)
- 레이어 `name` 문자열(`97`, `112`, `127`, `171`, `188`, `201`, `229`)은 식별자 — 스냅샷 테스트 계약이므로 사실상 공개 API.

### 3-C. 상수/기본값

| 위치 | 값 |
|---|---|
| `ChartLayerBuilder.swift:102-103` | `lineJoin = .round`, `lineCap = .round` (main) |
| `ChartLayerBuilder.swift:117` | `lineJoin = .round` (overlay, cap 미설정 → `.butt` 기본) |
| `ChartLayerBuilder.swift:130-131` | 그라데이션 stops `alpha → 0` (2색, 세로 기본 방향) |
| `ChartLayerBuilder.swift:209` | `line.lineWidth = marker.emphasis ? 1.5 : 1` |
| `ChartLayerBuilder.swift:216` | 마커 라벨 `y: plotArea.rect.minY - text.frame.height - 2` (여백 2) |
| `ChartLayerBuilder.swift:239` | x라벨 `y: plotArea.rect.maxY + 4` |
| `ChartLayerBuilder.swift:245` | yPrimary `x: minX - size.width - 4` |
| `ChartLayerBuilder.swift:249` | ySecondary `x: maxX + 4` |
| `ChartLayerBuilder.swift:270` | `layer.contentsScale = UIScreen.main.scale` |

### 3-D. 코어 API 호출

없음. `LineChartLayout`/`AxisTick`/`SeriesLayout` **타입만** 소비한다 (설계상 올바른 어댑터).

### 3-F. Y축 방향

전부 `plotArea.y(_:axis:)` / `pointIgnoringInversion` 위임. 반전 분기 지점은 `mappedPoints` `ChartLayerBuilder.swift:71-73`:

```swift
return series.role == .overlay
    ? series.points.map { plotArea.pointIgnoringInversion($0) }
    : series.points.map { plotArea.point($0, axis: axis) }
```

**역할(overlay)에 따라 반전 규칙을 렌더러가 갈라 적용**한다. 코어는 overlay를 자체 min~max 정규화만 하고(`LineChartEngine.kt:78-82`) "반전 무시"라는 표시를 출력에 실어주지 않으므로, 이 규칙은 렌더러가 암묵 재현하는 계약이다. `TouchMarker.swift:137`, `ChartLayerBuilder.swift:71`, `AreaSilhouette.swift:40` 세 곳이 각자 재현.

---

## 4. RDHeartRateZoneView.swift (257줄) — 심박존 도넛

### 4-A. 자체 계산 로직

| 위치 | 코드 | 코어 대응 |
|---|---|---|
| `RDHeartRateZoneView.swift:64-68` | `let ring = style.donutRingWidth` / `let radius = (min(bounds.width, bounds.height) - ring) / 2` / `let start = -CGFloat.pi / 2` | 없음. 링 기하 (플랫폼 고유로 봐도 무리 없음) |
| `RDHeartRateZoneView.swift:79-80` | `let a0 = start + 2 * .pi * CGFloat(seg.startFraction)` / `let a1 = start + 2 * .pi * CGFloat(seg.startFraction + seg.sweepFraction)` | fraction→각도 변환. 코어가 fraction 제공(`DonutEngine.kt:18-21`) — 정당한 어댑터 |
| `RDHeartRateZoneView.swift:237-256` | **히트테스트 전체.** `242-246` 반경 대역 검사 `distance >= radius - ring/2, distance <= radius + ring/2`; `247-248` `atan2(p.y - center.y, p.x - center.x) + .pi / 2` + 음수 보정; `249` `let frac = Double(angle / (2 * .pi))`; `250-252` `frac >= $0.startFraction && frac < $0.startFraction + $0.sweepFraction` | **없음. 위반.** `barIndexAtX`가 코어로 간 것과 같은 성격(순수 기하 히트테스트)인데 도넛만 남아 있다. `DonutHitTest.segmentAt(fraction:)` 후보 |
| `RDHeartRateZoneView.swift:124-126` | `currentLayout?.segments.contains { Int($0.sourceIndex) == index }` | 없음. 외부 구동 인덱스 유효성 검사 |
| `RDHeartRateZoneView.swift:192-197` | `let dimmed = selectedIndex != nil && Int(seg.sourceIndex) != selectedIndex` / `base.withAlphaComponent(style.donutDimmedAlpha)` | 없음. 디밍 규칙 |
| `RDHeartRateZoneView.swift:219-230` | `let maxWidth = max(0, (radius - ring / 2) * 2 * 0.9)` / `var y = bounds.midY - totalH / 2` | 없음. 내접원 90% 폭 센터 라벨 레이아웃 |
| `RDHeartRateZoneView.swift:70` | `guard let layout = currentLayout, layout.total > 0, !layout.segments.isEmpty` → 빈 링 폴백 | 없음. 무데이터 표현 결정 |

### 4-B. 포맷팅

```swift
// RDHeartRateZoneView.swift:205
let percentText = "\(Int((seg.sweepFraction * 100).rounded()))%"
```

`Double.rounded()` = **half away from zero**, 소수점 없음, `%` 하드코딩, **`NumberFormatter`/로케일 미사용**. 세그먼트별로 독립 반올림하므로 **합이 100%가 안 될 수 있다**(예: 33.3/33.3/33.3 → 33+33+33=99). 코어에 퍼센트 문자열/정수 배분 로직 없음.

```swift
// RDHeartRateZoneView.swift:181, 188, 203  (3곳 반복)
accessibilityLabel = "심박존 도넛"
// RDHeartRateZoneView.swift:214
accessibilityLabel = [seg.label, percentText].compactMap { $0 }.joined(separator: " ")
```

**한국어 문자열 리터럴이 SDK 안에 하드코딩**되어 있다. `NSLocalizedString` 미사용, 앱 주입 지점 없음. 존 이름(`seg.label`)은 코어 경유 앱 제공(`DonutSegmentLayout.label`).

### 4-C. 상수/기본값

| 위치 | 값 |
|---|---|
| `RDHeartRateZoneView.swift:28` | `UIImpactFeedbackGenerator(style: .light)` |
| `RDHeartRateZoneView.swift:68` | `start = -CGFloat.pi / 2` (12시 시작, 시계방향) |
| `RDHeartRateZoneView.swift:81, 194` | 색 폴백 `?? .systemGray` |
| `RDHeartRateZoneView.swift:96` | `shape.lineCap = .butt` |
| `RDHeartRateZoneView.swift:176-177` | `textAlignment = .center`, `lineBreakMode = .byTruncatingTail` |
| `RDHeartRateZoneView.swift:222` | 센터 라벨 폭 계수 `0.9` |

### 4-D. 코어 API 호출 지점

- `RDHeartRateZoneView.swift:45` — `DonutEngine.shared.layout(data:)`
- `RDHeartRateZoneView.swift:143-146` — `DonutEngine.shared.toggleSelection(current:tapped:)` (`KotlinInt` 브리지)

**`HeartRateZoneEngine`(`calculate`/`zoneBpmRanges`)은 호출하지 않는다.** 존 집계·bpm 경계는 앱이 계산해 `DonutChartData`로 넘긴다 — 클래스 이름이 `RDHeartRateZoneView`인데 심박존 코어를 안 쓰는 비대칭.

### 4-E. 제스처

**제스처 recognizer를 쓰지 않고 `touchesEnded` 오버라이드**(`RDHeartRateZoneView.swift:102-105`). 임계값·슬롭 없음(즉 드래그 후 떼도 선택된다). 의미 해석 = `handleTap`(`110-112`) → `segmentIndex(at:)`(히트테스트) → `applySelection`(`129-139`) → 코어 `toggleSelection`. 자동 해제 타이머: `scheduleAutoDeselect` `149-158`, `Timer.scheduledTimer(withTimeInterval: style.donutAutoDeselectDelay, repeats: false)`.

### 4-F. Y축

해당 없음(극좌표). 각도 기준점만 `-π/2`.

---

## 5. AxisScale.swift (28줄) — **역스케일 재구성 (핵심 경계 위반)**

### 5-A. 자체 계산 로직

```swift
// AxisScale.swift:12-27
init?(ticks: [AxisTick]) {
    guard let first = ticks.first, let last = ticks.last,
          last.position != first.position, last.value != first.value
    else { return nil }
    baseValue = first.value
    basePosition = first.position
    valuePerPosition = (last.value - first.value) / (last.position - first.position)
}
func value(atPosition position: Double) -> Double {
    baseValue + (position - basePosition) * valuePerPosition
}
func position(ofValue value: Double) -> Double {
    basePosition + (value - baseValue) / valuePerPosition
}
```

**코어 대응: 부분적으로만 존재.** `AxisDomain.normalize`(`AxisDomain.kt:9-10`)가 정방향이고 **역함수(denormalize)가 없다**. 그래서 렌더러가 `LineChartLayout.axisTicks`에서 도메인을 역추정한다. 함의:

- 축이 선형이라는 가정이 렌더러에 하드코딩된다(로그축 등 도입 시 조용히 깨진다).
- **tick 2개 미만이면 `nil`** → `RDChartView.xAxisScale()`이 nil → 스크럽·줌·실루엣이 전부 무음 비활성(`RDChartView.swift:513-520`, `524`, `321`, `190`).
- `last.value != first.value` 가드는 **X 도메인이 축퇴(단일 x값)인 기록에서 상호작용을 끈다**.
- 코어가 `xDom`/`yDom`을 layout에 실어주면 이 파일 전체가 사라진다.

### 5-B/C

포맷팅·상수 없음(`AxisScale.swift`는 순수 수학).

### 5-D

코어 호출 없음. `AxisTick` 타입만 소비.

---

## 6. PlotArea.swift (46줄) — 픽셀 변환

### 6-A. 자체 계산 로직 (전부 좌표변환 — 설계상 렌더러 책임)

```swift
// PlotArea.swift:17-19
func x(_ nx: Double) -> CGFloat { rect.minX + CGFloat(nx) * rect.width }
// PlotArea.swift:21-24
func y(_ ny: Double, axis: Axis) -> CGFloat {
    let fractionFromTop = invertedAxes.contains(axis) ? ny : 1.0 - ny
    return rect.minY + CGFloat(fractionFromTop) * rect.height
}
// PlotArea.swift:32-34
func yIgnoringInversion(_ ny: Double) -> CGFloat {
    rect.minY + CGFloat(1.0 - ny) * rect.height
}
// PlotArea.swift:42-45
func normalizedX(at px: CGFloat) -> Double {
    guard rect.width > 0 else { return 0 }
    return Double(min(max((px - rect.minX) / rect.width, 0), 1))
}
```

`normalizedX`의 `0~1` 클램프(`44`)는 좌표변환이 아니라 **입력 정책**이다(플롯 밖 터치를 가장자리로 흡수). 코어 대응 없음.

### 6-C. 상수

`PlotArea.swift:15` — `isRenderable`: `width > 0 && height > 0` (렌더 가능 임계값).

### 6-F. Y축 방향 — **정본 위치**

`PlotArea.swift:22`가 유일한 반전 스위치: `invertedAxes.contains(axis) ? ny : 1.0 - ny`. 기본은 "값 큰 쪽이 위"(`1.0 - ny`), 반전축은 "값 큰 쪽이 아래"(`ny`). 단, 앞서 지적한 대로 `RDBarChartView.swift:76`와 `AreaSilhouette.swift:40`이 이 파일을 우회한다.

---

## 7. PaceColormap.swift (44줄) — **컬러맵 (경계 위반)**

### 7-A. 자체 계산 로직

```swift
// PaceColormap.swift:26-42
static func defaultPaceColor(_ input: BarPaceColorInput) -> UIColor {
    let f = input.fastest, s = input.slowest, a = input.average, p = input.value
    guard s > f else { return UIColor(red: 0, green: 1, blue: 0, alpha: 1) }
    let pace1 = a - (a - f) * 0.70
    let pace2 = a + (s - a) * 0.25
    let length1 = pace1 - f, length2 = pace2 - pace1, length3 = s - pace2
    func clamp(_ x: Double) -> CGFloat { CGFloat(min(max(x, 0), 1)) }
    if p < pace1 {                                  // 파랑↔청록
        let cv = length1 > 0 ? clamp((pace1 - max(f, p)) / length1) : 0
        return UIColor(red: 0, green: 1 - 0.4 * cv, blue: 1, alpha: 1)
    } else if p < pace2 {                           // 초록↔노랑
        let cv = length2 > 0 ? clamp((pace2 - p) / length2) : 0
        return UIColor(red: 1 - cv, green: 1, blue: 0, alpha: 1)
    } else {                                        // 노랑↔빨강
        let cv = length3 > 0 ? clamp((s - min(s, p)) / length3) : 0
        return UIColor(red: 1, green: cv, blue: 0, alpha: 1)
    }
}
```

**코어 대응: 없음.** 3구간 앵커 산정(`0.70`/`0.25`)과 구간별 정규화 계수 `cv`는 순수 수학이고 플랫폼 중립이다. 색 자체(RGB)는 스타일이지만 **`cv` 계산과 구간 판정은 코어로 갈 수 있는 값 생성**이다. Android에 동일 공식이 별도로 존재한다면 두 벌이 표류한다.

- 축퇴 폴백 `s > f` 실패 시 순수 초록 `(0,1,0)`(`28`).
- `2-A`의 `fastest/slowest/average` 산출(`RDBarChartView.swift:104-109`)과 짝이므로 **막대 색 결정 로직 전체가 렌더러 안에 있다**.
- `BarColorRole`(코어가 준 FASTER/ON_TARGET/SLOWER)는 `colorRole`로 입력에 실려 있지만(`PaceColormap.swift:12`) **기본 팔레트는 이를 무시한다**. 즉 코어가 계산한 역할 분류가 기본 경로에서 버려진다. 대응 `style.barColors`(`ChartStyle.swift:48-52`)도 미사용.

### 7-C. 상수

`0.70`(pace1 앵커 계수), `0.25`(pace2), `0.4`(파랑↔청록 green 감쇠 폭). RGB: `(0, 1-0.4cv, 1)`, `(1-cv, 1, 0)`, `(1, cv, 0)`, 폴백 `(0,1,0)`. 전부 `alpha: 1`.

### 7-D

코어 호출 없음. `BarColorRole` 타입만 소비.

---

## 8. AreaSilhouette.swift (56줄) — 배경 고도 실루엣

### 8-A. 자체 계산 로직

| 위치 | 코드 | 코어 대응 |
|---|---|---|
| `AreaSilhouette.swift:20-22` | `HeightFractionsKt.heightFractions(values:minSpan:)` | **코어 위임 완료** (`HeightFractions.kt:13-19`) |
| `AreaSilhouette.swift:31` | `guard points.count >= 2, plotArea.isRenderable` | 없음. 2점 임계값 |
| `AreaSilhouette.swift:34` | `let usableHeight = style.areaHeightFraction * plotArea.rect.height` | 없음. 실루엣이 차지할 높이 비율(0.35) 적용 |
| `AreaSilhouette.swift:38` | `let nx = min(max(xScale.position(ofValue: points[index].x), 0), 1)` | **없음.** 시리즈 도메인보다 넓은 area의 x를 0~1로 **클램프** — 1x에 클립 마스크가 없어서 하는 값 보정. 클램프된 점들이 플롯 가장자리에 뭉쳐 실루엣 모양을 왜곡한다(주석은 "번짐 방지"만 언급) |
| `AreaSilhouette.swift:40` | `let py = baseY - CGFloat(fractions[index]) * usableHeight` | **Y 반전 독립 구현 #3.** `plotArea.y`를 명시적으로 쓰지 않음(`27`에 문서화) |
| `AreaSilhouette.swift:43-48` | 폴리라인 + 바닥 닫기 | 없음. 경로 조립(정당) |

### 8-B. 포맷팅

없음.

### 8-C. 상수

`AreaSilhouette.swift:19` — `minSpan: Double = 0` (기본 인자, 실제 호출은 `style.areaMinValueSpan`=0.5로 `32`). `AreaSilhouette.swift:50` — `layer.name = "area.altitude"`. `53` — `strokeColor = nil`(외곽선 없음).

### 8-D. 코어 API 호출 지점

- `AreaSilhouette.swift:20-22` — `HeightFractionsKt.heightFractions(values:minSpan:)` (`KotlinDouble` 배열 브리지)

### 8-F. Y축 방향

`AreaSilhouette.swift:39-40`: `baseY = plotArea.rect.maxY`(`33`) 기준 위로 `fraction * usableHeight`. **축 반전과 무관하게 항상 bottom-up**. 페이스축이 반전된 차트에서도 고도는 위로 솟는다 — 의도된 계약이지만 `PlotArea`를 우회한 세 번째 구현이다.

---

## 9. ZoomState.swift (63줄) — **줌 도메인 계산 (경계 위반, 통째로)**

### 9-A. 자체 계산 로직 — 전부

```swift
// ZoomState.swift:17
var scale: Double { fullSpan / span }
// ZoomState.swift:23-28  (레거시 경로 — RDChartView 미사용)
mutating func pinch(by gestureScale: Double, anchor: Double, maxScale: Double) {
    guard gestureScale > 0 else { return }
    let targetSpan = min(max(span / gestureScale, fullSpan / maxScale), fullSpan)
    let anchorValue = window.lowerBound + anchor * span
    place(lower: anchorValue - anchor * targetSpan, span: targetSpan)
}
// ZoomState.swift:32-38  (실사용 경로)
mutating func pinch(from startWindow: ClosedRange<Double>, cumulativeScale: Double, anchor: Double, maxScale: Double) {
    let startSpan = startWindow.upperBound - startWindow.lowerBound
    let targetSpan = min(max(startSpan / cumulativeScale, fullSpan / maxScale), fullSpan)
    let anchorValue = startWindow.lowerBound + anchor * startSpan
    place(lower: anchorValue - anchor * targetSpan, span: targetSpan)
}
// ZoomState.swift:41-43
mutating func pan(byFraction fraction: Double) {
    place(lower: window.lowerBound - fraction * span, span: span)
}
// ZoomState.swift:46-49
mutating func setWindow(_ target: ClosedRange<Double>) {
    let span = min(target.upperBound - target.lowerBound, fullSpan)
    place(lower: target.lowerBound, span: span)
}
// ZoomState.swift:53-62
private mutating func place(lower: Double, span: Double) {
    if span >= fullSpan { window = fullDomain; return }
    let clamped = min(max(lower, fullDomain.lowerBound), fullDomain.upperBound - span)
    window = clamped...(clamped + span)
}
```

**코어 대응: 없음.** 파일 헤더가 "UIKit 비의존, 단위 테스트 대상"이라 명시(`ZoomState.swift:3`) — 이미 플랫폼 중립임을 자각한 코드다. `maxScale` 클램프, 앵커 고정, `fullDomain` 경계 클램프, `span >= fullSpan` 시 `fullDomain` 인스턴스 재사용(1 ulp 드리프트로 `isZoomed`가 영구 true가 되는 것을 막는 `54-58`의 미묘한 규칙) — 전부 Android가 독립 재현해야 하는 규칙이다. `core/query/ZoomWindow.kt` 후보.

미사용 API: `pinch(by:anchor:maxScale:)`(`23`), `pan(byFraction:)`(`41`), `reset()`(`51`), `scale`(`17`)은 `RDChartView`에서 호출되지 않는다(테스트 전용). `RDChartView.swift:423-427`이 `pan(byFraction:)`과 같은 계산을 다시 한다.

### 9-B/C

포맷팅 없음. 상수: `ZoomState.swift:14` `isZoomed`는 **엄격 동등성** `window != fullDomain` — `54-58`의 인스턴스 재사용과 짝을 이룬다.

### 9-D

코어 호출 없음. `import Foundation`만.

---

## 10. TouchMarker.swift (174줄) — 스크럽 마커

### 10-A. 자체 계산 로직

| 위치 | 코드 | 코어 대응 |
|---|---|---|
| `TouchMarker.swift:50-54` | `LineChartEngine.shared.nearest(data:x:xMin:xMax:)` with `xMin: xScale.value(atPosition: -1e-9)`, `xMax: xScale.value(atPosition: 1 + 1e-9)` | **코어 위임 + 렌더러가 창 경계를 epsilon으로 넓힘**. 관용치가 렌더러 소유 |
| `TouchMarker.swift:56-61` | id→axis, id→role 딕셔너리 (중복 시 첫 항목) | 없음. `ChartLayerBuilder.swift:17`과 중복 |
| `TouchMarker.swift:64` | `let snapSource = results.first { roleBySeriesId[$0.seriesId] == .main } ?? results.first` | **없음. 위반.** "수직선을 어느 시리즈의 근접점에 스냅할지"를 렌더러가 고른다. 코어 `nearest`는 시리즈별 결과만 주고 대표를 정하지 않는다. Android가 다른 규칙을 쓰면 같은 터치에 다른 스냅 |
| `TouchMarker.swift:70-72`, `31-32`, `83-84` | `guard rawNx >= -1e-9, rawNx <= 1 + 1e-9 else { return nil }` / `let nx = min(max(rawNx, 0), 1)` | **없음.** 창 밖 판정 + 클램프, 3곳 반복 |
| `TouchMarker.swift:102` | `yScale.position(ofValue: result.y)` | **없음(`AxisScale` 경유).** 실값 y → 정규화 위치 역산. 코어는 `nearest`에서 **실값 y만** 주고 정규화 y를 주지 않아 렌더러가 역산해야 한다 — 구조적 누락 |
| `TouchMarker.swift:132-136` | `layoutSeries.points.min(by: { abs($0.x - seriesNx) < abs($1.x - seriesNx) })` | **없음. 위반.** 정규화 공간에서의 **두 번째 최근접 탐색**을 렌더러가 직접 구현. 코어 `nearest`(`Nearest.kt:7-19`)와 같은 알고리즘의 중복이고, overlay는 축 스케일이 없어 역산이 불가하다는 이유로 생겨난 우회 경로. 코어가 overlay 정규화 y를 `NearestResult`에 실어주면 사라진다 |
| `TouchMarker.swift:122` | `guard !valuesBySeriesId.isEmpty else { return nil }` | 없음. 마커 표시 임계값 |
| `TouchMarker.swift:169-173` | `chartAxis(of:)` `Axis` → `ChartAxis` 매핑 | 없음. 두 코어 enum 간 매핑을 렌더러가 유지(코어에 브리지 없음) |

### 10-B. 포맷팅

자체 생성 없음. 전부 주입 포매터:

- `TouchMarker.swift:93` — `context.formatter(.yOverlay, result.y)` (overlay는 실값)
- `TouchMarker.swift:120` — `context.formatter(chartAxis, result.y)`

`.yOverlay` 축으로 포매터가 호출되는 것이 `RDChartView.swift:117-118`에 계약으로 문서화되어 있다(축 keyed 딕셔너리 포매터의 크래시 함정).

### 10-C. 상수

| 위치 | 값 |
|---|---|
| `TouchMarker.swift:31, 52, 53, 71, 84` | `1e-9` (경계 관용, 5회 반복) |
| `TouchMarker.swift:108-110, 143-145` | 도트 `radius: style.touchDotRadius`, `startAngle: 0, endAngle: .pi * 2, clockwise: true` |
| `TouchMarker.swift:161` | `line.lineWidth = 1` (수직선 폭 — 스타일 미노출 하드코딩) |
| `TouchMarker.swift:35, 76, 106, 141, 158` | 레이어 이름 `"touch.marker"`, `"touch.dot.<id>"`, `"touch.line"` |

### 10-D. 코어 API 호출 지점

- `TouchMarker.swift:50-54` — `LineChartEngine.shared.nearest(data:x:xMin:xMax:)`

### 10-E. 제스처 의미 해석

이 파일이 **"어느 포인트인가"의 실제 결정자**다: 코어 `nearest`가 시리즈별 후보를 주고, `TouchMarker.swift:64`가 대표(스냅 소스)를 고르고, `70-72`가 창 밖이면 전체 취소, `83-84`가 시리즈별로 개별 탈락시킨다. 즉 **3단 필터가 렌더러에 있다**.

### 10-F. Y축 방향

`TouchMarker.swift:101-104` — `plotArea.point(NormalizedPoint(x:nx, y:yScale.position(ofValue: result.y)), axis: axis)` (반전 적용). `TouchMarker.swift:137-139` — `plotArea.pointIgnoringInversion(...)` (overlay, 반전 무시). 라인 경로(`ChartLayerBuilder.swift:71-73`)와 동일 규칙을 재현.

---

## 11. ChartStyle.swift (101줄) — 기본값 전체 표

**2단계 상수 대조표의 iOS 열.** 총 45개 프로퍼티. 괄호 안은 실제 사용처.

| # | 프로퍼티 | 기본값 | 줄 | 소비처 |
|---|---|---|---|---|
| 1 | `lineWidth` | `2` | 7 | `ChartLayerBuilder.swift:101` |
| 2 | `primaryLineColor` | `.systemBlue` | 8 | `ChartLayerBuilder.swift:90` |
| 3 | `secondaryLineColor` | `.systemRed` | 9 | `ChartLayerBuilder.swift:90` |
| 4 | `gradientMaxAlpha` | `0.25` | 17 | `ChartLayerBuilder.swift:40-41` (÷√n) |
| 5 | `seriesColors` | `[:]` | 20 | `ChartLayerBuilder.swift:88` |
| 6 | `gridLineColor` | `UIColor.systemGray4.withAlphaComponent(0.7)` | 23 | `ChartLayerBuilder.swift:20`, `RDBarChartView.swift:77` (nil→그리드 없음) |
| 7 | `gridLineDashPattern` | `[3, 3]` | 24 | `ChartLayerBuilder.swift:176`, `RDBarChartView.swift:85` |
| 8 | `gridLineWidth` | `0.5` | 25 | `ChartLayerBuilder.swift:175`, `RDBarChartView.swift:84` |
| 9 | `overlayLineColor` | `UIColor.systemPurple.withAlphaComponent(0.8)` | 29 | `ChartLayerBuilder.swift:89` |
| 10 | `overlayLineWidth` | `1.5` | 30 | `ChartLayerBuilder.swift:116` |
| 11 | `refLineDashPattern` | `[6, 3]` | 33 | `RDBarChartView.swift:159` (라인차트 기준선은 현재 미사용) |
| 12 | `refBandColor` | `UIColor.systemOrange.withAlphaComponent(0.12)` | 34 | `ChartLayerBuilder.swift:193` |
| 13 | `areaFillColor` | `UIColor.systemGray3.withAlphaComponent(0.35)` | 37 | `AreaSilhouette.swift:52` |
| 14 | `areaHeightFraction` | `0.35` | 38 | `AreaSilhouette.swift:34` |
| 15 | `areaMinValueSpan` | `0.5` (도메인 단위, m) | 41 | `AreaSilhouette.swift:32` → 코어 `minSpan` |
| 16 | `markerLineColor` | `.systemGray4` | 44 | `ChartLayerBuilder.swift:208` |
| 17 | `markerEmphasisLineColor` | `.systemGray` | 45 | `ChartLayerBuilder.swift:208` |
| 18 | `barColors` | `[.faster: .systemGreen, .onTarget: .systemGray, .slower: .systemOrange]` | 48-52 | **미사용 (죽은 기본값)** |
| 19 | `barCornerRadius` | `3` | 53 | `RDBarChartView.swift:132` |
| 20 | `barShowYAxisLabels` | `true` | 54 | `RDBarChartView.swift:88` |
| 21 | `barShowXAxisLabels` | `true` | 55 | `RDBarChartView.swift:141` |
| 22 | `barReferenceLineColor` | `UIColor.label.withAlphaComponent(0.6)` | 56 | `RDBarChartView.swift:157` |
| 23 | `barMinHeight` | `2` | 57 | `RDBarChartView.swift:127` (코어 heightFraction 보정) |
| 24 | `barDimOpacity` | `0.35` (`Float`) | 58 | `RDBarChartView.swift:209` |
| 25 | `barPartialOpacity` | `0.6` (`Float`) | 59 | `RDBarChartView.swift:207` |
| 26 | `barColorProvider` | `nil` | 61 | `RDBarChartView.swift:136` (nil→`defaultPaceColor`) |
| 27 | `barSelectionLineColor` | `UIColor.label.withAlphaComponent(0.55)` | 62 | `RDBarChartView.swift:226` |
| 28 | `barCalloutBackgroundColor` | `.label` | 63 | `RDBarChartView.swift:252` |
| 29 | `barCalloutTextColor` | `.systemBackground` | 64 | `RDBarChartView.swift:235` |
| 30 | `barCalloutFont` | `.systemFont(ofSize: 12, weight: .semibold)` | 65 | `RDBarChartView.swift:234` |
| 31 | `donutColors` | `[.zone1: .systemBlue, .zone2: .systemGreen α0.7, .zone3: .systemYellow, .zone4: .systemOrange, .zone5: .systemRed]` | 68-74 | `RDHeartRateZoneView.swift:81, 194` |
| 32 | `donutRingWidth` | `28` | 75 | `RDHeartRateZoneView.swift:64, 220, 242` |
| 33 | `donutEmptyColor` | `UIColor.systemGray4.withAlphaComponent(0.5)` | 76 | `RDHeartRateZoneView.swift:72` |
| 34 | `donutDimmedAlpha` | `0.3` | 79 | `RDHeartRateZoneView.swift:196` |
| 35 | `donutCenterLabelFont` | `.systemFont(ofSize: 13)` | 80 | `RDHeartRateZoneView.swift:206` |
| 36 | `donutCenterLabelColor` | `.secondaryLabel` | 81 | `RDHeartRateZoneView.swift:207` |
| 37 | `donutCenterPercentFont` | `.systemFont(ofSize: 28, weight: .bold)` | 82 | `RDHeartRateZoneView.swift:210` |
| 38 | `donutCenterPercentColor` | `.label` | 83 | `RDHeartRateZoneView.swift:211` |
| 39 | `donutAutoDeselectDelay` | `3.0` s | 84 | `RDHeartRateZoneView.swift:152-154` |
| 40 | `donutSelectionHapticsEnabled` | `true` | 85 | `RDHeartRateZoneView.swift:132` |
| 41 | `axisLabelFont` | `.systemFont(ofSize: 10)` | 88 | `ChartLayerBuilder.swift:212, 233`, `RDBarChartView.swift:114, 277` |
| 42 | `axisLabelColor` | `.secondaryLabel` | 89 | `ChartLayerBuilder.swift:212, 233`, `RDBarChartView.swift:277` |
| 43 | `plotInsets` | `UIEdgeInsets(top: 16, left: 44, bottom: 20, right: 44)` | 92 | `RDChartView.swift:178`, `RDBarChartView.swift:70, 177, 216` |
| 44 | `touchLineColor` | `.label` | 95 | `TouchMarker.swift:160` |
| 45 | `touchDotRadius` | `4` | 96 | `TouchMarker.swift:108, 143` |

**스타일에 노출되지 않은 하드코딩 상수** (2단계 대조 시 누락 위험): `TouchMarker.swift:161` 수직선 폭 `1`, `ChartLayerBuilder.swift:209` 마커선 폭 `1.5`/`1`, `RDBarChartView.swift:158/227` 폭 `1`, `RDChartView.swift:223` 애니 `0.6`, `RDChartView.swift:51` `maxZoomScale = 10`, `RDBarChartView.swift:98` 막대 폭 비율 `0.6`, `RDBarChartView.swift:9` `labelMinGap = 6.0`, `RDBarChartView.swift:250` 말풍선 반경 `6`, 라벨 여백 `4`/`2`, `PaceColormap`의 `0.70`/`0.25`/`0.4`/RGB.

---

## 12. Exports.swift (2줄)

```swift
// Exports.swift:2
@_exported import LumipolGraph
```

로직 없음. **코어 타입 전체를 SDK 공개 표면으로 재노출**한다 — 앱이 `LineChartData`·`Point`·`Axis` 등을 직접 구성한다는 뜻이고, 이는 "A(데이터 입력·검증)" 경계가 SDK 밖(앱)에 있음을 확정한다.

---

# 서브시스템 분류표

## A — 데이터 입력·검증·정규화·다운샘플링

| 기능 | 위치 | 판정 |
|---|---|---|
| `backgroundArea` x 오름차순 정렬 | `RDChartView.swift:133` | 렌더러가 코어 전제조건 보증. 코어가 `interpolatedY` 안에서 정렬/검증해야 |
| `AreaPoint` → 코어 `Point` 변환 캐시 | `RDChartView.swift:134`, `100` | 브리지(정당) |
| 시리즈 id 중복 해소 "첫 항목 우선" | `ChartLayerBuilder.swift:17`, `TouchMarker.swift:56-61` | **코어에 유일성 계약 부재로 렌더러가 정책 결정. 2곳 중복** |
| 1점 이하 시리즈 미표시 | `ChartLayerBuilder.swift:70` | 임계값 렌더러 소유 |
| 2점 미만 area 미표시 | `AreaSilhouette.swift:31` | 임계값 렌더러 소유 |
| 색 앵커용 부분 스플릿 제외 + 축퇴 폴백 | `RDBarChartView.swift:104-109` | **위반. 집계 통계** |
| 실루엣 값 0~1 정규화 | `AreaSilhouette.swift:20-22` | **코어 위임 완료** (`heightFractions`) |
| `touchesEnded`가 유일 입력(슬롭 없음) | `RDHeartRateZoneView.swift:102-105` | 입력 정책 |
| 다운샘플·평활·아웃라이어 컷 | — | **렌더러에 없음. 코어 `PaceSeriesEngine`이 전담 (앱이 호출)** |

## B — 스케일·틱·축라벨

| 기능 | 위치 | 판정 |
|---|---|---|
| **tick 2점으로 축 도메인 역산** | `AxisScale.swift:12-27` | **위반. 코어 `AxisDomain`에 역함수 없음** |
| 실값 y → 정규화 위치 역산 (스크럽 도트) | `TouchMarker.swift:102` | **위반. `NearestResult`가 정규화 y를 안 준다** |
| 전체 X 도메인 복원 (`position 0`/`1` 역산) | `RDChartView.swift:322-323` | **위반. `LineChartLayout`이 `xDom`을 안 내보낸다** |
| `AxisScale` 캐시 (스크럽 핫패스) | `RDChartView.swift:513-520`, `71` | 성능(정당) |
| 가로 그리드 축 선택 (primary 없으면 secondary) | `ChartLayerBuilder.swift:161-163` | **위반. 값 선택 결정** |
| x축 라벨 stride 계산 | `RDBarChartView.swift:121-122` | **코어 위임 완료** (`labelStride`) |
| 라벨 표시 여부 판정 | `RDBarChartView.swift:142` | **코어 위임 완료** (`isLabelVisible`) |
| 라벨 최대 폭 측정 | `RDBarChartView.swift:117-120` | 플랫폼 고유(정당) |
| 라벨 최소 여백 `6.0` | `RDBarChartView.swift:9` | 상수 — Android와 수동 동기 중 |
| `niceScale`/헤드룸 | — | **렌더러에 없음. 코어 전담** (`NiceScale.kt`) |

## C — 좌표변환·레이아웃

| 기능 | 위치 | 판정 |
|---|---|---|
| 정규화 x → 픽셀 | `PlotArea.swift:17-19` | 정당 |
| 정규화 y → 픽셀 (반전 스위치) | `PlotArea.swift:21-24` | 정당 (정본) |
| 반전 무시 y (overlay) | `PlotArea.swift:32-34` | 정당하나 **"overlay는 반전 무시"라는 규칙 자체는 코어 출력에 표시 없음** |
| 픽셀 → 정규화 x + 0~1 클램프 | `PlotArea.swift:42-45` | 클램프는 입력 정책 |
| **막대 자체 플롯 사각형 + bottom-up 매핑** | `RDBarChartView.swift:71, 76, 127-129, 151` | **`PlotArea` 우회. 2중 구현** |
| **실루엣 자체 bottom-up 매핑** | `AreaSilhouette.swift:33-40` | **`PlotArea` 우회. 3중 구현** |
| 실루엣 x 0~1 클램프 | `AreaSilhouette.swift:38` | **값 보정 — 모양 왜곡 부작용** |
| `barMinHeight` 클램프 | `RDBarChartView.swift:127` | **코어 `heightFraction` 보정** |
| 막대 슬롯/폭/센터링 | `RDBarChartView.swift:97-98, 128-129` | **`barIndexAtX`와 슬롯 수학 분리** |
| 그라데이션 area path 닫기 + 마스크 이동 | `ChartLayerBuilder.swift:133-142` | 정당 |
| 밴드 min/abs 흡수 | `ChartLayerBuilder.swift:190-192` | 정당 |
| 축 라벨/마커 라벨 배치 | `ChartLayerBuilder.swift:213-217, 236-250` | 정당 |
| 말풍선 기하 + 좌우 클램프 | `RDBarChartView.swift:236-244` | 정당 |
| 도넛 반경/각도 기하 | `RDHeartRateZoneView.swift:64-68, 79-80` | 정당 |
| 센터 라벨 프레임(내접원 90%) | `RDHeartRateZoneView.swift:219-230` | 정당 |
| 줌 클립 마스크 | `RDChartView.swift:354-366` | 정당 |
| 레이어 z-순서 | `ChartLayerBuilder.swift:5, 20-60`, `RDChartView.swift:190-204` | 정당 |
| 재구축 스킵 키 | `RDChartView.swift:104-108, 162-165` | 정당(성능) |

## D — 스타일 상수

| 기능 | 위치 |
|---|---|
| 기본값 45종 | `ChartStyle.swift:7-96` (위 표) |
| **미사용 기본값** `barColors` | `ChartStyle.swift:48-52` |
| 색 폴백 체인 (맵→역할→축) | `ChartLayerBuilder.swift:87-91` |
| 도넛 색 폴백 `.systemGray` | `RDHeartRateZoneView.swift:81, 194` |
| **그라데이션 α ÷ √n** | `ChartLayerBuilder.swift:41` — 값 생성 |
| **알파 합성 (partial × dim)** | `RDBarChartView.swift:207-209` |
| **디밍 알파 대체** | `RDHeartRateZoneView.swift:196` |
| **연속 페이스 컬러맵 (3구간 보간)** | `PaceColormap.swift:26-42` — **위반** |
| 스타일 미노출 하드코딩 상수 | `TouchMarker.swift:161`, `ChartLayerBuilder.swift:209`, `RDBarChartView.swift:98/158/227/250`, `RDChartView.swift:51/223` |

## E — 상호작용

| 기능 | 위치 | 판정 |
|---|---|---|
| 제스처 인식 파라미터 (라인) | `RDChartView.swift:443-469` — 롱프레스 0.5s, 더블탭 2회, 팬 1터치 | 플랫폼 고유(정당) |
| 제스처 인식 파라미터 (막대) | `RDBarChartView.swift:34-39` — 롱프레스 0.5s | 정당 |
| 도넛 입력 (`touchesEnded`, 임계 없음) | `RDHeartRateZoneView.swift:102-105` | 정당 |
| 제스처 라우팅 (스크럽/팬/핀치 배타) | `RDChartView.swift:471-488`, `415`, `480` | 정당 |
| 동시 인식 정책 | `RDChartView.swift:539-547, 549-557`, `RDBarChartView.swift:295-298` | 정당 |
| 가로 우세 판정 | `RDChartView.swift:439-441` | 정당 |
| 픽셀 → rawX 환산 | `RDChartView.swift:525` | `AxisScale` 의존 |
| **근접점 질의** | `TouchMarker.swift:50-54` | **코어 위임 완료** (`nearest`) |
| **스냅 대표 시리즈 선택 (main 우선)** | `TouchMarker.swift:64` | **위반** |
| **overlay 정규화 공간 최근접 탐색** | `TouchMarker.swift:132-136` | **위반. `nearest` 알고리즘 중복** |
| 창 밖 3단 필터 + 클램프 (`1e-9`) | `TouchMarker.swift:31-32, 70-72, 83-84`, `RDChartView.swift:244-247` | **위반. 관용치가 렌더러 소유, 두 종류(고정/상대) 혼재** |
| 마커 표시 임계 (`valuesBySeriesId` 빈 값) | `TouchMarker.swift:122` | 정책 |
| 델리게이트 콜백 짝 보존 (`hadMarker`) | `RDChartView.swift:250, 266-271, 294-298` | 정당 |
| **줌 창 계산 전체** | `ZoomState.swift:17-62` | **위반. 코어 대응 없음** |
| 핀치 앵커 산출 | `RDChartView.swift:376` | 정당 |
| **팬 픽셀→도메인 환산** | `RDChartView.swift:423-427` | **위반 + `ZoomState.pan` 중복** |
| 제스처 중 상태 유지/지연 정리 | `RDChartView.swift:341-343, 401-405, 429-433` | 정당 |
| **막대 히트테스트** | `RDBarChartView.swift:265-269` | **코어 위임 완료** (`barIndexAtX`) |
| 막대 선택 중복 억제 | `RDBarChartView.swift:167-172` | 정당 |
| `barLabels` 없으면 선택 불가 | `RDBarChartView.swift:176` | 숨은 결합 |
| **도넛 히트테스트 (반경+각도)** | `RDHeartRateZoneView.swift:237-256` | **위반. `barIndexAtX`와 동급인데 코어 미이관** |
| **도넛 토글 전이** | `RDHeartRateZoneView.swift:143-146` | **코어 위임 완료** (`toggleSelection`) |
| 외부 구동 인덱스 검증 | `RDHeartRateZoneView.swift:120, 124-126` | 정책 |
| 햅틱 트리거 조건 | `RDChartView.swift:496-499`, `RDBarChartView.swift:170, 188`, `RDHeartRateZoneView.swift:132` | 정당 |
| 지표 선택/슬롯 배정 | — | **렌더러에 없음. 코어 `SeriesSelection` (앱이 호출)** |

## F — 포맷팅

| 기능 | 위치 | 규칙 |
|---|---|---|
| 라인 축 기본 포매터 | `RDChartView.swift:529-531` | `String(format: "%g", value)` — 유효숫자 6, 후행 0 제거, **로케일 미적용** |
| 막대 y틱 기본 포매터 | `RDBarChartView.swift:271-273` | `String(Int(value.rounded()))` — half-away-from-zero, **로케일 미적용**, NaN/범위초과 시 트랩 위험 |
| 도넛 퍼센트 | `RDHeartRateZoneView.swift:205` | `"\(Int((sweepFraction*100).rounded()))%"` — **독립 반올림으로 합≠100 가능, 로케일 미적용** |
| 도넛 접근성 라벨 (기본) | `RDHeartRateZoneView.swift:181, 188, 203` | **`"심박존 도넛"` 한국어 하드코딩, 미로컬라이즈** |
| 도넛 접근성 라벨 (선택) | `RDHeartRateZoneView.swift:214` | `[seg.label, percentText].compactMap{}.joined(separator: " ")` |
| 라인 차트 접근성 | — | **없음** (`RDChartView`에 `accessibility*` 설정 전무) |
| 주입 포매터 통과 지점 | `ChartLayerBuilder.swift:232`, `TouchMarker.swift:93, 120` | `.yOverlay` 케이스 계약 (`RDChartView.swift:117-118`) |
| 앱 제공 문자열 통과 | `RDBarChartView.swift:89, 144, 234`, `ChartLayerBuilder.swift:212` | mm:ss 등 **렌더러 구현 없음** |
| 레이어 이름 문자열 | `ChartLayerBuilder.swift:97/112/127/171/188/201/229`, `TouchMarker.swift:35/76/106/141/158`, `RDBarChartView.swift:225/247`, `AreaSilhouette.swift:50`, `RDChartView.swift:184/186` | 스냅샷 테스트 계약 = 사실상 공개 API |

## G — 애니메이션

| 기능 | 위치 | 값 |
|---|---|---|
| main 라인 등장 (`strokeEnd` 0→1) | `RDChartView.swift:217-227` | `duration = 0.6`, `.easeOut`, `series.main.` 프리픽스 대상만 |
| 등장 1회 무장 규칙 | `RDChartView.swift:26, 86-89, 139-140, 206-209, 346` | 최초 render만, 줌 커밋 시 해제 |
| 암시 애니 차단 (재구축) | `RDChartView.swift:168-170` | `CATransaction.setDisableActions(true)` |
| 암시 애니 차단 (막대 선택) | `RDBarChartView.swift:202-204` | 동일 |
| 도넛 자동 해제 타이머 | `RDHeartRateZoneView.swift:149-158` | `Timer`, `style.donutAutoDeselectDelay = 3.0` |
| 라이브 핀치/팬 = 매 프레임 코어 재계산 | `RDChartView.swift:392-399, 414-436`, `329-350` | 애니 대신 재렌더 전략 (`layoutIfNeeded()` 동기 호출 `348`) |
| 막대/도넛 렌더 직후 동기 레이아웃 | `RDBarChartView.swift:56`, `RDHeartRateZoneView.swift:49` | `layoutIfNeeded()` — 라인차트만 비동기(`RDChartView.swift:141`)로 계약이 갈린다 |

---

# 코어 API 호출 지점 전체 (8종)

| 코어 심볼 | 호출 위치 |
|---|---|
| `LineChartEngine.layout(data:backgroundArea:)` | `RDChartView.swift:147` |
| `LineChartEngine.layout(data:xMin:xMax:)` | `RDChartView.swift:332-336` |
| `LineChartEngine.interpolatedY(points:x:)` | `RDChartView.swift:279-280` |
| `LineChartEngine.nearest(data:x:xMin:xMax:)` | `TouchMarker.swift:50-54` |
| `LabelThinningKt.labelStride` / `isLabelVisible` | `RDBarChartView.swift:121-122` / `142` |
| `BarHitTestKt.barIndexAtX` | `RDBarChartView.swift:266-268` |
| `HeightFractionsKt.heightFractions` | `AreaSilhouette.swift:20-22` |
| `DonutEngine.layout` / `toggleSelection` | `RDHeartRateZoneView.swift:45` / `143-146` |

**렌더러가 전혀 호출하지 않는 코어 API**: `BarChartEngine.layout`·`chooseTimeBucketSeconds`, `HeartRateZoneEngine.calculate`·`zoneBpmRanges`, `PaceSeriesEngine.preprocess`, `SeriesSelection.*`, `PaceSeriesId.*`, `niceScale`, `AxisDomain`, `yValues`, `Stats`/`segmentStats`, `LineChartEngine.nearest(data:x:)`(창 없는 오버로드), `LineChartEngine.layout(data:)`(단일 인자). 앱이 직접 호출하거나 미사용입니다. 특히 `LineChartLayout.stats`(`Output.kt:24`)는 렌더러가 만들어 받아 두고 **한 번도 읽지 않습니다**.
