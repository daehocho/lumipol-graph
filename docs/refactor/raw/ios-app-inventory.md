# Runday_IOS — lumipol-graph(차트) SDK 연동 인벤토리

조사 대상: `/Users/daeho/Runday_IOS` (읽기 전용). 범위는 `import LumipolGraph`/`LumipolGraphUI`를 하는 파일 전부(프로덕션 6개 + 테스트 2개)와 거기서 역추적한 가공 경로.
모든 인용의 줄번호는 조사 시점(2026-07-27) 기준.

## 0. 연동 지점 전체 목록 (전수)

`grep -rn "import LumipolGraph"` 결과가 곧 SDK 접촉면 전부입니다. 프로덕션 파일은 딱 6개이며 모두 `RunDay/Sources/Feature/Record/ChartComponents/` 아래에 있습니다.

| 파일 | 역할 |
|---|---|
| `RDPaceChartDataBuilder.swift` (354줄) | 통합 라인차트 데이터 빌더 — 유일한 페이스 계산 지점 |
| `RDUnifiedChartView.swift` (418줄) | 통합 라인차트 카드(칩·범례·요약·스크럽) |
| `RDSplitChartDataBuilder.swift` (108줄) | 스플릿 막대 데이터 빌더 |
| `RDSplitChartView.swift` (309줄) | 스플릿 막대 카드(색범례) |
| `RDHeartRateZoneCardView.swift` (325줄) | 심박존 도넛 카드(범례·적응형 레이아웃) |
| `RDHeartRateZoneCalculator.swift` (54줄) | 심박존 계산 위임 + 나이기반 최대심박 |
| `RunDayTests/RDPaceChartDataBuilderTests.swift` | 29 테스트 (`import LumipolGraphUI`) |
| `RunDayTests/RDSplitChartDataBuilderTests.swift` | 9 테스트 (`import LumipolGraph`) |

세 카드는 모두 `RDAnalyticsReportViewController.m`(ObjC) 한 곳에서만 생성·주입됩니다 (`RDAnalyticsReportViewController.m:306-324`):

```objc
[unifiedChartView showWithTrack:self.track dataType:self->graphDataType];
[splitChartView showWithTrack:self.track distanceUnit:userData.distanceUnit targetPaceSecPerUnit:0 dataType:dataType];
[heartRateZoneCardView showWithTrack:self.track birthday:udAccount.birthday gender:udAccount.gender];
```

**중요**: `RDPaceChartDataBuilder.swift`와 같은 디렉터리의 `BalloonMarker.swift`(321줄)는 SDK와 무관한 **DGCharts(`import Charts`) 레거시**입니다(`BalloonMarker.swift:13`, `open class BalloonMarker: MarkerImage`). 구 화면(`RDGraphView`, `CommonFiles/ChartViews/RDBarChartView`, VirtualRace, SmartTraining)이 여전히 이걸 씁니다. 즉 앱에는 DGCharts 기반 차트와 lumipol-graph 기반 차트가 **공존**하며, 이름 충돌 때문에 SDK 타입을 모듈 한정으로 참조하는 곳이 세 군데 있습니다:

- `RDSplitChartView.swift:21` — `private let barView = LumipolGraphUI.RDBarChartView(frame: .zero)` (앱에 동명 `Sources/CommonFiles/ChartViews/RDBarChartView.swift` 존재)
- `RDSplitChartDataBuilder.swift:62-63` — `let data: LumipolGraph.BarChartData` (DGCharts `BarChartData`와 충돌)
- `RDPaceChartDataBuilder.swift:196` — `let data: LumipolGraph.LineChartData`

---

## 1. SDK 공개 API 사용 빈도

### 1-A. 렌더러(LumipolGraphUI) — 뷰·스타일·델리게이트

| 심볼 | 사용 파일 | 참조 수 | 핵심 호출 |
|---|---|---|---|
| `RDChartView` | RDUnifiedChartView | 4 | `chartView.render(_:style:invertedAxes:labelFormatter:backgroundArea:)` — `RDUnifiedChartView.swift:143-151` |
| `RDChartView.isZoomEnabled` | RDUnifiedChartView | 1 | `chartView.isZoomEnabled = true` — `:294` |
| `RDChartScrubDelegate` | RDUnifiedChartView | 2 | 채택 `:405`, 3개 콜백 전부 구현 `:406/410/414` |
| `LumipolGraphUI.RDBarChartView` | RDSplitChartView | 4 | `render(_:style:barLabels:xAxisLabels:yLabelFormatter:)` — `RDSplitChartView.swift:100-106` |
| `LumipolGraphUI.RDHeartRateZoneView` | RDHeartRateZoneCardView | 3 | `render(_:style:)` `:309`, `selectSegment(at:)` `:211`, `selectedIndex` `:204`, `zoneDelegate` `:251` |
| `RDHeartRateZoneSelectionDelegate` | RDHeartRateZoneCardView | 1 | `:320-324` |
| `ChartStyle` | 3개 뷰 | 5 | 아래 §5 |
| `ChartAxis` | RDUnifiedChartView | 3 | `formatAxisLabel(axis:…)` — `.yPrimary`/`.ySecondary` 분기 `:192-196` |

`RDChartView`의 ObjC 진입점(`render(data:)`)은 쓰지 않고 Swift 전체 오버로드만 씁니다.

### 1-B. 코어(LumipolGraph) — 엔진·데이터 타입

| 심볼 | 사용 파일 | 참조 수 |
|---|---|---|
| `PaceSeriesId` (LINE_PRIORITY/DISPLAY_PRIORITY/PACE/HEART/CADENCE/ALTITUDE) | RDPaceChartDataBuilder | 12 |
| `PaceSamplePoint` | RDPaceChartDataBuilder | 8 |
| `SeriesSelection` (toggled/normalized/assignSlots/slotAxis) | RDPaceChartDataBuilder(7), RDUnifiedChartView(2, 주석) | 9 |
| `PaceSeriesEngine.shared.preprocess` | RDPaceChartDataBuilder | 3 |
| `PaceSeriesInput` | RDPaceChartDataBuilder | 1 |
| `Series`, `Axis`, `SeriesRole(.main)`, `Point`, `AreaPoint`, `LineChartData`, `ChartConfig` | RDPaceChartDataBuilder | 각 1~7 |
| `LumipolGraph.BarChartData` | RDSplitChartDataBuilder(5), RDSplitChartView(2) | 7 |
| `SplitSample` | RDSplitChartDataBuilder | 5 |
| `BarChartEngine.shared` (`chooseTimeBucketSeconds`, `layout`) | RDSplitChartDataBuilder(2), RDSplitChartView(2) | 4 |
| `LumipolGraph.BarChartLayout` (`bars`, `.value`, `.isPartial`, `.endMinutes`) | RDSplitChartView | 2 |
| `DonutChartData` / `DonutSegment` / `DonutColorRole` | RDHeartRateZoneCardView | 4 |
| `HeartRateZoneEngine.shared` (`calculate`, `zoneBpmRanges`) / `HeartRateZoneSample` | RDHeartRateZoneCalculator | 5 |
| `KotlinDouble` / `KotlinInt` (KMP 브릿지 박싱) | RDPaceChartDataBuilder(10), RDSplitChartDataBuilder(4) | 14 |

`LineChartEngine`은 앱에서 **직접 호출하지 않습니다**(`RDChartView.render` 내부에서만). 반면 `BarChartEngine.layout`은 앱이 직접 호출합니다(`RDSplitChartView.swift:71`) — 렌더러가 layout을 만들어 주지 않는 비대칭 지점입니다.

### 1-C. 옵셔널/박싱 브릿지 패턴

Kotlin 옵셔널 왕복이 앱 코드에 그대로 노출돼 있습니다:

```swift
// RDPaceChartDataBuilder.swift:161-163
heartRate: point.heartRate.map { KotlinDouble(value: $0) },
cadence: point.cadence.map { KotlinDouble(value: $0) },
altitude: point.altitude.map { KotlinDouble(value: $0) }))
```

`KotlinDouble` 생성자 레이블이 두 파일에서 다릅니다 — `KotlinDouble(value:)`(`RDPaceChartDataBuilder.swift:161`) vs `KotlinDouble(double:)`(`RDSplitChartDataBuilder.swift:82`). Int 계열은 `KotlinInt(int: Int32(...))`, 역방향은 `Int(truncating:)`·`$0.int32Value`로 언박싱합니다(`RDPaceChartDataBuilder.swift:176, 211-212, 249`).

---

## 2. SDK 호출 전 데이터 가공 — 전부

### 2-A. 공통 원천: `Track.sortedTrackPointsA2()`

세 카드 전부 이 하나로 시작합니다(`RDPaceChartDataBuilder.swift:86`, `RDSplitChartDataBuilder.swift:43`, `RDHeartRateZoneCardView.swift:87`). 정렬 규칙은 `RunDay/Sources/CommonFiles/CoreDataEntities/Track.m:98-118`:

```objc
NSArray *zeroPoints = [... filteredArrayUsingPredicate:zeroPredicate];   // exerciseTime == 0
...
[result addObject:sortedZeroPoints.firstObject];                          // created 최신 1개만 남김
NSSortDescriptor *exerciseTimeAsc = [[NSSortDescriptor alloc] initWithKey:@"exerciseTime" ascending:YES];
```

즉 **`exerciseTime == 0`인 포인트는 `created` 최신 1개만 살리고 나머지는 버린 뒤, `exerciseTime` 오름차순 정렬**. 빈 트랙이면 `nil`을 반환하고, 앱은 `?? []`로 받습니다. 이 함수는 차트 전용이 아니라 앱 전역(`TrackInfoMaker.m`, `RDDataTransferManager.m` 등 11개 파일)이 공유합니다.

### 2-B. 결측 센티널 해석·이상치 클램프 (라인차트)

`RDPaceChartDataBuilder.swift:53-63` — CoreData 원본값 → nil 정규화를 **여기 한 곳**에 모았습니다:

```swift
heartRate: rawHeartRate.flatMap { $0 > 0 ? $0 : nil },
altitude: rawAltitude.flatMap { $0 > Self.invalidAltitude ? $0 : nil },
cadence: rawCadence.flatMap { $0 > 0 ? min($0, Self.maxCadence) : nil }
```

센티널 상수 (`:25`, `:29`):
- `invalidAltitude = -100.0` — `> -100.0`만 유효. 구 iOS 관행(`TrackInfoMaker.m`·`RDDataTransferManager.m`의 `> -100.f`)이자 Android `ChartSampleMapper.INVALID_ALTITUDE`와 동일 값이라고 주석에 명시.
- `maxCadence = 250.0` — 상한 **클램프**(버리지 않고 `min`). 주석: "COROS는 한쪽 발 걸음수를 2배해 저장하므로 이상치가 상한을 넘기 쉽다."
- 심박: `> 0`만 유효. 단 테스트에 `testZeroHeartRateIsRealMeasurementNotMissing`(`RDPaceChartDataBuilderTests.swift:107`)이 있어 0 취급 규칙이 명문화돼 있습니다.

### 2-C. `PaceSamplePoint.x` / `paceSeconds` 계산 — 워치/GPS 분기

`RDPaceChartDataBuilder.swift:136-164` 전문 인용:

```swift
static func series(input: RDPaceChartInput, xMode: RDPaceChartXMode) -> RDPaceSeries {
    guard input.sumDistance > 0, input.runningTime > 0 else { return .empty }      // :137 조기 컷

    let unitValue = (input.distanceUnit == .KM) ? 1.0 : KilometerToMile           // :140
    var distanceSum = 0.0
    var timeSum = 0.0
    for point in input.points {
        let pace: Double
        if input.usesWatchSpeed {
            pace = RunPaceUtils.mphToPace(fromSpeed: point.speed, unitType: input.distanceUnit)   // :148
        } else {
            pace = RunPaceUtils.pace(
                withDistance: point.distance, time: point.timeInterval,
                unitType: input.distanceUnit)                                     // :150-152
        }
        let paceSeconds = RunPaceUtils.seconds(withPace: pace)  // 무효 페이스면 0 이하  // :154
        timeSum += point.timeInterval                                             // :155
        distanceSum += abs(point.distance * unitValue)                            // :156
        let x = (xMode == .distance) ? distanceSum / 1000.0 : timeSum / 60.0      // :157
```

정리하면:

- **분기 조건**: `usesWatchSpeed = (track.useRundayWatch?.intValue == 1)` (`:99`). 워치 운동이면 포인트별 `speed`(m/s)에서, 아니면 포인트별 `distance`/`timeInterval`에서 페이스를 냅니다.
- **워치 경로** `RunPaceUtils.mphToPace` (`RunPaceUtils.swift:330-340`): `speed <= 0`·NaN·inf면 `0.0`. 유효하면 `unitValue = KM ? 1.0 : 1.0/MileToKilometer`, `pace = (1000.0 * unitValue) / speed / 60.0` → **분/단위거리**. 함수명은 "mph"지만 실제 입력은 m/s입니다(주석에도 m/s로 적혀 있음).
- **GPS 경로** `RunPaceUtils.pace(withDistance:time:unitType:)` (`RunPaceUtils.swift:28-38`): `distance == 0`·NaN·inf 또는 `time == 0`·NaN·inf면 `0.0`. 유효하면 `unitValue = KM ? 1.0 : MileToKilometer`, `(time / 60.0) / (distance / (1000.0 * unitValue))` → **분/단위거리**. 음수 distance는 걸러지지 않으므로 음수 페이스가 나올 수 있고, 그건 코어 필터가 받습니다.
- **`paceSeconds`** = `RunPaceUtils.seconds(withPace:)` (`RunPaceUtils.swift:234-244`): `pace * 60`, 그리고 `floorToDecimal(2)`로 소수 2자리 내림("0.000000000006 같은 것을 방지"). 0/NaN/inf는 `0.0`. 즉 **초 단위, 소수 2자리 내림**.
- **`x` (거리모드)**: `distanceSum += abs(point.distance * unitValue)` 후 `/1000.0`. `unitValue`는 거리모드에서 `KilometerToMile = 0.621371`(마일) — 미터→마일 환산이므로 방향이 맞습니다. `abs()`로 음수 거리 포인트가 x를 되감지 못하게 막습니다.
- **`x` (시간모드)**: `timeSum / 60.0` → **분**. `abs` 없음(음수 timeInterval은 그대로 누적).
- **비대칭 주의**: `paceSeconds`용 마일 환산은 `MileToKilometer = 1.609344000000865`, x용은 `KilometerToMile = 0.621371`입니다(`define.h:310-311`). 두 상수는 정확한 역수가 아니며(0.621371 × 1.609344 = 0.99999966…) 앱 전역이 예전부터 이 값을 씁니다.

**앱이 하지 않는 것**: 필터·결측 승계·다운샘플·가용성 판정은 전부 코어 `PaceSeriesEngine.preprocess`로 넘깁니다(`:166-169`). 앱 주석(`:129-131`)에 코어 규칙이 문서화돼 있습니다 — "필터: 2분/km 미만·평균+10분/km 초과 컷 + 슬로우 아웃라이어 퍼센타일 컷. 다운샘플: skip = max(1, count/3000) — 최대 ~3000점 렌더." 이 규칙들은 테스트로 앱 쪽에서도 잠겨 있습니다(`RDPaceChartDataBuilderTests.swift:38, 53, 70, 86, 116`).

### 2-D. 스플릿 막대 가공

`RDSplitChartDataBuilder.swift:72-75`:

```swift
let unit = (input.distanceUnit == .mile) ? metersPerMile : 1000.0
let samples = input.points
    .filter { $0.distance > 0 && $0.timeInterval > 0 }
    .map { SplitSample(distanceMeters: $0.distance, timeSeconds: $0.timeInterval) }
```

- 앱이 하는 유일한 필터: `distance > 0 && timeInterval > 0`. 페이스 계산은 하지 않고 **원시 미터·초를 그대로** 코어에 넘깁니다(집계·색기준·라벨은 코어 소유, `:65` 주석).
- 시간모드에서만 버킷 크기를 코어에 물어보고 되돌려 줍니다(`:93-94`, `BarChartEngine.shared.chooseTimeBucketSeconds(runningSeconds:)`).
- 시간모드는 `targetPaceSecPerUnit: nil`을 강제해 색 기준을 런 평균으로 넘깁니다(`:98`).
- 여기서 `metersPerMile = 1609.344`(`:67`)를 쓰는데, 이는 앱 전역 `MileToKilometer = 1.609344000000865`(`define.h:310`)와 **다른 값**입니다. 라인차트와 막대차트의 마일 환산 상수가 갈라져 있습니다.

### 2-E. 심박존 시간 재구성

`RDHeartRateZoneCardView.swift:94-115` — 저장된 per-point `timeInterval`을 쓰지 않고 **누적 운동시간(`exerciseTime`) 델타로 재구성**합니다:

```swift
let hasCumulativeTime = (points.last?.exerciseTime?.doubleValue ?? 0) > 0
...
if hasCumulativeTime {
    let elapsedSeconds = point.exerciseTime?.doubleValue ?? 0
    deltaSeconds = max(0, elapsedSeconds - prevSeconds)
    prevSeconds = max(prevSeconds, elapsedSeconds)
} else {
    deltaSeconds = max(0, point.timeInterval?.doubleValue ?? 0)
}
```

- 이유(주석 `:82-86`): 저장된 `timeInterval`은 출처별로 wall-clock/운동시계가 섞여 있어, AOS(`ChartSampleMapper.zoneSamples`)와 존별 누적시간을 맞추려면 `exerciseTime` 델타를 써야 함. iOS `exerciseTime`은 초, AOS `realExerciseTime`은 ms라서 `/1000` 대응(추가 변환 불필요).
- 폴백(주석 `:88-93`): 2021.01 데이터 정합 작업 이전 기록은 `exerciseTime` 누적값이 비어 있고, 그 마이그레이션은 기록 상세를 거칠 때만 도는데 이 카드는 플랜·버추얼레이스 결과 경로로도 열림 → 전부 0이면 `timeInterval`로 되돌림.
- `max(0, …)`으로 역전 포인트의 음수 델타를 막습니다(`:99-101` 주석).
- 심박 결측 표현이 라인차트와 **다릅니다**: 여기서는 nil 대신 `heartRate: point.heartRate?.doubleValue ?? 0`(`:111`)로 0을 넘기고, 0 이하는 미측정이라는 계약(`RDHeartRateZoneCalculator.swift:13`)에 의존합니다.

---

## 3. 앱이 만드는 문자열 (라벨·툴팁·범례·포매터)

### 3-A. 라인차트 축/툴팁 포매터 (SDK에 콜백으로 주입)

`RDUnifiedChartView.swift:147-149`에서 클로저를 주입하고, 본체는 `:187-207`:

```swift
if let slot = slot, let metric = axisMetric[slot] {
    switch metric {
    case .pace: return RunPaceUtils.stringPace(withSeconds: value * 60.0)  // 페이스 시리즈 y=분
    case .heartRate, .cadence, .altitude: return "\(Int(value))"
    }
}
// x축
if xMode == .time { return value <= 0.1 ? "" : "\(Int(value)):00" }
return String(format: "%g", value)
```

규칙:
- **페이스 y축**: 시리즈 y가 **분**이므로 `× 60`으로 초 환산 후 `stringPace(withSeconds:)`. 반올림 없이 `Int` 절삭.
- **심박/케이던스/고도 y축**: `Int(value)` — **절삭**(0쪽으로), 반올림 아님.
- **x축 시간모드**: `"\(Int(value)):00"` → `"3:00"` 형태. `value <= 0.1`이면 빈 문자열(0 틱 숨김).
- **x축 거리모드**: `String(format: "%g", value)` — 유효숫자 기반 자동 표기(예: `1`, `2.5`, `1e+06`). 단위 문자(`km`/`mi`)는 **붙지 않습니다**.
- 이 포매터는 축 tick뿐 아니라 **스크럽 값에도 그대로 쓰입니다** — SDK가 `didScrubTo valuesBySeriesId`에 담아주는 문자열이 `labelFormatter` 결과입니다(`ios-renderer/Sources/LumipolGraphUI/RDChartView.swift:6-7` 주석 및 `:276`).
- 로케일: 전부 `String(format:)`/문자열 보간이므로 **로케일 비의존**(소수점은 항상 `.`). `NumberFormatter`는 쓰지 않습니다.

`.yOverlay` 케이스는 처리하지 않고 `default: slot = nil`로 x축 포맷으로 떨어집니다(`:195-197`) — 0.29.0에서 오버레이 슬롯이 폐지돼 발생하지 않는다는 전제입니다.

### 3-B. 페이스 문자열 포맷 규칙 (`RunPaceUtils`)

- `stringPace(withSeconds:)` (`RunPaceUtils.swift:633-635`) → `stringPace1Minute(withSeconds:)` (`:648-659`):
  ```swift
  if seconds == 0.0 || seconds.isNaN || seconds.isInfinite { return "-'--\"" }
  let min: Int = Int(seconds) / 60
  let sec: Int = Int(seconds) % 60
  return String(format: "%d'%02d\"", min, sec)
  ```
  즉 표기는 `mm:ss`가 아니라 **`4'30"`** (아포스트로피+더블쿼트). 분은 0패딩 없음, 초는 2자리 0패딩, **절삭**(반올림 아님). 무효값은 `-'--"`. **음수 초는 가드가 없어** `-1'-30"` 같은 결과가 나올 수 있습니다.
- `stringPace(withPace:)` (`:546-561`): 추가로 `pace < PACE_MIN(0.1) || pace > PACE_MAX(99.0)`이면 `-'--"`(`define.h:350-351`). 평균 페이스에 이 경로가 쓰입니다(`RDPaceChartDataBuilder.swift:350`).
- `stringHHMMSS(withSeconds:)` (`:405-424`): 1시간 이상 `"%d:%02d:%02d"`, 미만 `"%02d:%02d"`, 0/NaN/inf는 `"00:00"`. 심박존 범례 시간이 이걸 씁니다.
- 거리 문자열 유틸(`stringKilometer`/`stringMile`/`stringKilometerOrMile`, `:523-758`)은 **차트 경로에서 쓰이지 않습니다** — 차트 x축은 `%g`로 직접 만듭니다. 참고로 이들은 소수 2자리 **내림**(`floorToDecimal(2)`)이고, `stringKilometer`만 유일하게 `NumberFormatter`(로케일 의존)를 씁니다.

### 3-C. 막대차트 문자열

`RDSplitChartView.swift:76, 78, 105`:

```swift
let barLabels = layout.bars.map { RunPaceUtils.stringPace(withSeconds: $0.value) }
let xLabels = xAxisLabels(layout: layout, xMode: xMode)
...
yLabelFormatter: { RunPaceUtils.stringPace(withSeconds: $0) }
```

- `barLabels`: 구간 페이스 문자열. 주석(`:73-74`)에 따르면 렌더러 0.15.x부터 막대 위 상시 표시가 아니라 **롱프레스 말풍선 전용**.
- x축 라벨(`:132-141`): 시간모드는 코어가 낸 `endMinutes`를 `"\($0.endMinutes?.intValue ?? 0)"`로 감싸기만(반올림·최소1은 코어 소유), 거리모드는 `(1...layout.bars.count).map { "\($0)" }` — **단위 인덱스 1,2,3…**(누적 거리값 아님).
- y축: 페이스 문자열 동일 포매터.
- 색 범례 캡션(`:193-199`): 앵커가 있으면 실제 최저/최고 구간 페이스 문자열, 없으면 `StringTable[1196]`(느림)/`StringTable[1195]`(빠름).

### 3-D. StringTable(로컬라이즈) id 목록

차트 화면이 쓰는 문자열 id 전부입니다.

| id | 용도 | 위치 |
|---|---|---|
| 142 | "페이스" — 통합차트 범례 + 스플릿카드 제목 | `RDUnifiedChartView.swift:258`, `RDSplitChartView.swift:218` |
| 5093 / 5092 / 5094 | 심박수 / 고도 / 케이던스 (칩·범례) | `RDUnifiedChartView.swift:259-261` |
| 41 / 5207 | "km" / "min" 토글 | `:298-299` |
| 34 / 5099 | 평균 페이스 / 최고 페이스 | `:317-319` |
| 5091 | "분석 데이터가 없습니다." | `:334`, `RDHeartRateZoneCardView.swift:257` |
| 1196 / 1195 | 느림 / 빠름 (막대 색범례 폴백) | `RDSplitChartView.swift:197-198` |
| 6611 | "심박수 구간" (도넛 제목) | `RDHeartRateZoneCardView.swift:253` |
| 6631, 6630, 6629, 6628, 6627 | Z1 워밍업 → Z5 최대 존 이름 | `:48` (`zoneNameIds` 배열, index 0=Z1) |
| 6632 / 6633 | `"({0} bpm~)"` / `"({0}~{1} bpm)"` | `:242-246` |

`{0}`/`{1}` 치환은 `replacingOccurrences(of:with:)` 수동 방식입니다(`:242-246`).

`RDSplitChartView.swift:218`에 알려진 결함이 주석으로 남아 있습니다: "페이스(전용 '구간별 페이스' 문자열 미발견 — 142로 대체)".

### 3-E. 요약값 정규화

`RDUnifiedChartView.swift:210-212` — 세 가지 무효 표현을 `"--"`로 통일:

```swift
private func normalized(_ value: String) -> String {
    (value.isEmpty || value == "0" || value == "-'--\"") ? emptyData : value
}
```

`emptyData = "--"` (`:32`). 문자열 비교로 무효 판정을 하는 구조라 `stringPace`의 무효 표현이 바뀌면 조용히 깨집니다.

고도 스크럽 값만 앱이 직접 포맷합니다 — 유일하게 **반올림**하고 단위를 붙이는 곳입니다(`:411`):

```swift
altitudeLegendValueLabel?.text = "\(Int(value.rounded()))m"
```

---

## 4. SDK 위에 덧그리는 뷰

세 카드 모두 SDK 뷰를 **자식 하나로 품고**, 나머지 UI는 전부 앱이 UIKit으로 직접 그립니다. SDK 뷰 위에 겹치는 오버레이는 없고(z축 중첩 없음), 모두 위/옆 형제 뷰입니다.

### 4-A. 통합 라인차트 (`RDUnifiedChartView`, 위→아래)

`:20-30` 선언, `:340-364` 제약.

1. `toggleStack` — `RDChartButton` 2개(km/min), 우측 상단, `spacing 8`, top 12 / trailing -16
2. `chipStack` — 지표 칩 4개(페이스·심박·케이던스·고도), `spacing 12`, 데이터 없는 지표는 `isHidden`으로 자리까지 회수(`:271` + 주석)
3. `statsStack` — 평균/최고 페이스 2열 `fillEqually` (`:320-323`)
4. `legendStack` — 색점+이름+값 범례. `fillEqually` 균등 칼럼 + **뒤쪽 빈 `UIView()` 패딩**으로 칼럼 피치 고정(`:242-244`)
5. `chartView` — `RDChartView` (SDK)
6. `noDataLabel` — 차트 중앙에 센터 정렬로 겹침(`:362-363`), 차트는 `isHidden`으로 숨김

범례 값 라벨은 스크럽 콜백으로 앱이 채웁니다(`:406-417`). 고도는 라인 시리즈가 아니라 `valuesBySeriesId`에 안 실려서 `didScrubToBackgroundValue`로 별도 채웁니다(`:47-48`, `:410-412`).

범례에 **무엇을 넣을지**는 `axisMetric`이 아니라 `data.series`에서 유도합니다(`:226-228`) — 0.29.0에서 심박·케이던스가 보조축을 공유하면서 `axisMetric`이 축당 대표 지표 하나만 담게 되어 케이던스가 빠지던 문제 때문(`:224-225` 주석):

```swift
let drawnIds = Set(result.data.series.map { $0.id })
var shown = Set(allMetrics.filter { drawnIds.contains(seriesId(for: $0)) })
if result.altitudeArea != nil { shown.insert(.altitude) }
```

### 4-B. 스플릿 막대 (`RDSplitChartView`)

`:232-245`: `titleLabel`(좌상단) + `legendStack`(우상단, 제목과 centerY 정렬) + `barView`(SDK, 그 아래 전체).

범례는 앱이 직접 만든 `GradientBarView`(`:252-295`, `private final class`, `CAGradientLayer` 래퍼)입니다. **SDK의 막대 색 공식을 앱이 24회 샘플링해 그라데이션 바를 재구성**합니다(`:159-181`) — 색약보정 모드에서는 `isEqualRGBA`(허용오차 0.001, `:297-308`)로 중복 색을 접고 하드 스톱으로 끊습니다.

### 4-C. 심박존 도넛 (`RDHeartRateZoneCardView`)

`:268-289` (SnapKit): `titleLabel` 상단 / `donutView`(SDK) 좌측 정사각 / `legendStack` 우측(도넛과 centerY) / `noDataLabel` 중앙.

범례가 SDK 도넛과 **양방향 결합**돼 있습니다:
- 범례 행 탭 → `donutView.selectSegment(at: zoneIndex)`로 SDK에 위임(`:209-212`)
- SDK 선택 변경 → `RDHeartRateZoneSelectionDelegate`로 되돌아와 범례 강조 갱신(`:320-324`, `applyLegendSelection`)
- 순서 반전 주의: 범례는 Z5→Z1로 쌓지만 배열 인덱스는 0=Z1이라, 행/라벨을 존 인덱스로 색인한 별도 배열에 보관합니다(`:58-61`)

`layoutSubviews`(`:295-316`)에서 폭 기반 적응 레이아웃을 직접 굴립니다 — 도넛을 160→120까지 줄여 범례 폭을 확보하고, 그래도 부족하면 범례에서 bpm 범위 텍스트를 제거. 필요 폭은 `RDChartLegend.trailingValueRowWidth`로 **NSString 실측**(`:226-235`).

---

## 5. 하드코딩 스타일/설정 값 (리터럴 전부)

### 5-A. 코어에 넘기는 설정값

| 값 | 위치 | 비고 |
|---|---|---|
| `ChartConfig(segmentCount: 5, maxTicks: 5)` | `RDPaceChartDataBuilder.swift:339` | 라인차트, 리터럴 인라인 |
| `toleranceSecPerUnit = 10.0` | `RDSplitChartDataBuilder.swift:68` | 막대 목표 페이스 허용오차(초/단위) |
| `maxTicks: Int32 = 5` | `:69` | 막대 |
| `metersPerMile = 1609.344` | `:67` | §2-D의 상수 불일치 참조 |
| `targetPaceSecPerUnit: 0` | `RDAnalyticsReportViewController.m:317` | ObjC 호출부에서 항상 0 → `> 0` 검사에 걸려 nil(`RDSplitChartView.swift:65`). **목표 페이스 기능이 사실상 비활성** |

### 5-B. 라인차트 `ChartStyle` (`RDUnifiedChartView.swift:170-183`)

```swift
style.seriesColors = [pace: …, heartRate: …, cadence: …]
style.areaFillColor = color(for: .altitude).withAlphaComponent(0.22)
style.gradientMaxAlpha = 0.25
```

`gradientMaxAlpha = 0.25`는 SDK 기본값과 동일한 값을 **의도적으로 명시 고정**한 것입니다(`:178-181` 주석: 코어 문서가 "선택 상한이 없으니 앱이 조절하라"고 안내하므로 기본값에 묵시적으로 기대지 않음). 나머지 `ChartStyle` 필드(~45개)는 전부 SDK 기본값입니다 — `plotInsets = (16, 44, 20, 44)`, `lineWidth = 2`, `axisLabelFont = 10pt`, `gridLineDashPattern = [3,3]` 등.

`invertedAxes`: `result.paceOnPrimary ? [.primary] : []`(`:142`) — 페이스가 주축일 때만 위=빠름으로 반전.

### 5-C. 지표 고정색 (`RDUnifiedChartView.swift:158-165`)

| 지표 | 색 | 실제 RGB |
|---|---|---|
| pace | `.graphOutlinePurple()` | `123,123,255` (`UIColor+Additions.m:415-417`) |
| heartRate | `.graphOutlineRed()` | `255,120,155` (`:431-433`) |
| cadence | `UIColor(named: "Color_GraphGreen") ?? .systemGreen` | 에셋 카탈로그 |
| altitude | `UIColor(named: "Color_GraphOrange") ?? .systemOrange` | 에셋, 실루엣은 alpha 0.22 |

슬롯색이 아니라 **지표 기준 고정색**이라 선택 조합이 바뀌어도 색이 유지되고(`:157` 주석), `seriesColors`(슬롯색보다 우선)로 SDK에 직접 매핑합니다.

### 5-D. 막대차트 `ChartStyle` (`RDSplitChartView.swift:87-99`)

```swift
var style = ChartStyle.default
style.barShowYAxisLabels = true
style.barShowXAxisLabels = true
style.barPartialOpacity = 1.0        // SDK 기본 0.6 → 1.0으로 덮어씀
style.barColorProvider = { colorInput in RDRouteColorizer.color(forPace: …) }
```

`barPartialOpacity = 1.0`은 SDK 기본 0.6에서 **의도적으로 변경**(주석 `:90`: "기본 0.6은 색을 흐려 범례와 대응이 깨짐"). `barColors`(BarColorRole 팔레트)는 `barColorProvider`가 있으면 무력화됩니다.

색 앵커 계산 리터럴(`:113-130`): 온전한 막대가 `count >= 2`이고 `max > min`이면 온전한 것만, 아니면 전체로 폴백. `average`는 `duration / (distance / splitDistanceMeters)`(런 전체 평균)를 우선, 없으면 막대값 산술평균.

`RDRouteColorizer.color(forPace:paceAvg:fastestPace:slowestPace:colorBlindnessCorrection:)`의 구간 계수(`RDRouteColorizer.swift:127-128`): `pace1 = paceAvg - (paceAvg - fastest) * 0.70`, `pace2 = paceAvg + (slowest - paceAvg) * 0.25`. 색약보정 임계값 `0.2 * length1`, `0.5 * length2`(`:136, 139`). 공식이 앵커에 대해 선형이라 막대차트가 초 단위 앵커를 넘겨도(지도는 분 단위) 색 결과는 일치합니다.

### 5-E. 도넛 `ChartStyle` (`RDHeartRateZoneCardView.swift:163-168`)

```swift
style.donutColors = Dictionary(uniqueKeysWithValues: zip(zoneColorRoles, zoneColors))
style.donutRingWidth = style.donutRingWidth * (donutSize / desiredDonutSize)
```

`donutRingWidth`는 SDK 기본 28에 도넛 크기 비율을 곱합니다 — 도넛이 120으로 줄면 `28 × 0.75 = 21`. 존 색은 `Color_WarmGreyTwo`(Z1), `Color_GraphBlue`(Z2), `Color_GraphGreen`(Z3), `Color_GraphOrange`(Z4), `Color_GraphRed`(Z5) (`:50-56`, 각각 `?? .systemGray/.systemBlue/.systemGreen/.systemOrange/.systemRed` 폴백). `donutAutoDeselectDelay = 3.0`, `donutDimmedAlpha = 0.3`은 SDK 기본값 그대로 사용.

### 5-F. 앱 자체 레이아웃/폰트 리터럴

**통합차트** (`RDUnifiedChartView.swift`): 배경 `.white`(`:294`) / 토글 폰트 15 (`:371`) / 칩 폰트 14, 선택시 `.boldSystemFont(14)` (`:276-277`, `:310`) / 통계 제목 12·값 16 (`:376`, `:380`) / noData 16 (`:335`) / `chipStack.spacing 12`(`:305`), `legendStack.spacing 8`(`:330`), `toggleStack.spacing 8`(`:302`), 통계열 `spacing 4`(`:388`) / 제약 상수: top 12, 좌우 16, 차트만 좌우 8, bottom -12, 수직 간격 10·12·8·8 (`:344-364`)

**스플릿** (`RDSplitChartView.swift`): 제목 `19pt .medium`(`:219`), `.black` / `legendStack.spacing 6`(`:225`) / 그라데이션 바 `96×10`, `cornerRadius 3`(`:184-186`, `:260`) / 샘플링 `steps = 24`(`:159`) / 색 비교 tolerance `0.001`(`:305`) / 인셋 좌우 16(제목·범례), 8(barView), 수직 12 (`:236-245`)

**심박존** (`RDHeartRateZoneCardView.swift`): `desiredDonutSize 160`, `minDonutSize 120`, `horizontalInset 16` (`:28-32`) / `legendDimAlpha 0.45`(`:62`), `legendRowMinHeight 26`(`:66`, 주석에 "HIG 권장 44pt에는 못 미치는 알려진 제약"으로 명시) / 제목 `19pt .medium`(`:254`), noData 16(`:258`) / `legendStack.spacing 6`(`:264`) / 폭 계산 `bounds.width - horizontalInset * 3`(`:299`)

**범례 팩토리** (`RDChartLegend.swift:15-23`): `dotSize 8`(cornerRadius 4) / 통합차트 행 이름 12pt·값 12pt semibold / HR존 행 이름 13pt·값 13pt semibold, 선택시 13pt semibold(`:95-97`) / `trailingValueRowSpacing 6` / 색상 `Color_GreyishBrown`(이름), `Color_BlackTwo`(값) / 이름 라벨 `minimumScaleFactor 0.8`(`:75`)

**섹션 높이** (`RDAnalyticsReportViewController.m:32-34`): `UNIFIED_SECTION_HEIGHT 460.0`, `SPLIT_SECTION_HEIGHT 300.0`, `HEART_ZONE_SECTION_HEIGHT 220.0`

**기타**: `defaultSelection = [.pace, .heartRate, .altitude]`(`RDUnifiedChartView.swift:34`) — 케이던스는 기본 미선택. `RDChartButton` 색은 `darkPeriwinkle()`(116,96,217) 선택 / `wisteria()`(154,147,191) 미선택, `circleView.cornerRadius = 2` (`RDChartButton.swift:48-51`).

---

## 6. 심박존 도넛 · 스플릿 막대 — 최대심박 계산 경로

2~5 항목은 §2-D/2-E, §3-C, §4-B/4-C, §5-D/5-E에 이미 녹였으므로, 여기서는 **최대심박 계산 위치와 공식**만 별도로 정리합니다.

계산은 **앱**에 있습니다 — SDK는 최대심박을 모르고, 완성된 정수만 받습니다.

1. **입력 조달**: `RDAnalyticsReportViewController.m:324` — `udAccount.birthday`, `udAccount.gender`를 `RDUserDefaultAccount`에서 읽어 넘김.

2. **만나이 산출**: `RDHeartRateZoneCardView.swift:117-120`

   ```swift
   let maxHeartRate: Int = birthday.map {
       RDHeartRateZoneCalculator.ageBasedMaxHeartRate(
           age: RDDateUtils.ageCalculation(birthDate: $0), isMale: gender == Int(MALE))
   } ?? 0
   ```
   `birthday`가 nil이면 `maxHeartRate = 0` → 코어가 무데이터로 판정 → 도넛 숨김.

   `RDDateUtils.ageCalculation` (`RunDay/Sources/CommonFiles/RDDate+Utils.swift:656-660`): `Calendar.current.dateComponents([.year], from: birthDate, to: Date().toLocalTime()).year ?? 0`, `max(age, 0)`. 현재 캘린더/로컬시간 기준 만나이(연 단위 정수를 Double로).

3. **공식**: `RDHeartRateZoneCalculator.swift:30-33`

   ```swift
   static func ageBasedMaxHeartRate(age: Double, isMale: Bool) -> Int {
       let maxHR = isMale ? (220.0 - age) : (206.0 - 0.88 * age)
       return maxHR > 0 ? Int(maxHR) : 0
   }
   ```
   남성 `220 − 나이`, 여성 `206 − 0.88 × 나이`. **성별 판정은 `gender == Int(MALE)`, 즉 MALE이 아닌 모든 값(미설정 포함)이 여성 공식으로 떨어집니다.** 결과는 `Int(...)` **절삭**. 구 `RDHeartPhaseGraphView`의 `getBpm` 공식과 동일하다고 주석에 명시(`:29`).

4. **존 경계**: 코어가 소유합니다. `HeartRateZoneEngine.shared.zoneBpmRanges(maxHeartRate:)`(`:39-43`)와 `calculate(samples:maxHeartRate:)`(`:45-53`)로 전량 위임. 앱 주석(`:24-26`, `:36-38`)에 코어 계약이 문서화돼 있습니다 — 최대심박의 **50/60/70/80/90% 경계**, 판정은 `frac >= 비율`(하한 포함)이므로 표시용 하한 bpm은 `ceil(비율 × maxHR)`, **50% 미만(휴식)은 어느 존에도 넣지 않음**.

5. **방어 가드**: `RDHeartRateZoneCardView.swift:127` — `result.zoneSeconds.count == zoneColorRoles.count`(=5)를 확인해 어긋나면 무데이터 처리(하위 인덱싱 크래시 방지).

---

## 7. 눈에 걸린 것 (조사 중 발견, 수정하지 않음)

1. **마일 환산 상수 3종 병존** — `MileToKilometer = 1.609344000000865`(`define.h:310`, 페이스), `KilometerToMile = 0.621371`(`define.h:311`, 라인차트 x), `metersPerMile = 1609.344`(`RDSplitChartDataBuilder.swift:67`, 막대). 정확한 역수가 아니라 마일 사용자에서 라인차트 x와 막대 스플릿 경계가 미세하게 갈립니다.

2. **`KotlinDouble` 생성자 레이블 불일치** — `KotlinDouble(value:)`(`RDPaceChartDataBuilder.swift:161`) vs `KotlinDouble(double:)`(`RDSplitChartDataBuilder.swift:82`). 같은 브릿지인데 호출 형태가 두 가지입니다.

3. **`targetPaceSecPerUnit`이 항상 0** — `RDAnalyticsReportViewController.m:317`이 리터럴 `0`을 넘기고 `RDSplitChartView.swift:65`가 `> 0`으로 걸러 nil이 됩니다. 즉 코어의 목표 페이스 색 기준 경로는 현재 프로덕션에서 절대 실행되지 않습니다(`toleranceSecPerUnit = 10.0`도 함께 사문화).

4. **무효값 판정이 문자열 비교** — `RDUnifiedChartView.swift:211`의 `value == "-'--\""`. `RunPaceUtils`의 무효 표현이 바뀌면 조용히 `"--"` 정규화가 깨집니다.

5. **`stringPace1Minute`에 음수 가드 없음** (`RunPaceUtils.swift:648-659`) — 코어 필터가 음수 페이스를 걸러 준다는 전제에 의존합니다.

6. **심박 결측 표현이 두 카드에서 다름** — 라인차트는 nil(`RDPaceChartDataBuilder.swift:59`), 도넛은 0(`RDHeartRateZoneCardView.swift:111`). 각 코어 엔진 계약이 달라서 의도된 것으로 보이지만, 브릿지 감사 시 짚어볼 지점입니다.

7. **`BarChartEngine.layout` 호출 비대칭** — 라인차트는 `RDChartView.render`가 내부에서 엔진을 돌리는데, 막대차트는 앱이 `BarChartEngine.shared.layout(data:)`을 직접 호출한 뒤 layout을 렌더러에 넘깁니다(`RDSplitChartView.swift:71`, `:100`). 앱이 layout에서 색 앵커를 직접 계산해야 하기 때문(`:83-85` 주석)이지만, 경계 설계상 라인/막대의 책임 분할이 다릅니다.
