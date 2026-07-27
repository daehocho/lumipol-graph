# 20 — 브릿지 레이어 감사 (2단계)

- 작성일: 2026-07-27
- 근거: 체크인된 생성 헤더 `ios-renderer/Frameworks/LumipolGraph.xcframework/ios-arm64/LumipolGraph.framework/Headers/LumipolGraph.h` (829줄, 0.29.0 빌드) 전수 판독
- 브릿지 방식: 순정 Kotlin/Native ObjC interop (SKIE 등 Swift export 도구 미사용 — 00-baseline §2)

## 요약 — 위험 항목 3건, 안전 확인 6건

| 항목 | 판정 | 상세 |
|---|---|---|
| 기본 인자 | ⚠️ **전면 소실** | §3 |
| 예외 | ⚠️ **@Throws 없음 — require() 크래시 경로** | §6 |
| enum 완전 매칭 | ⚠️ **Swift enum 아님 — exhaustive switch 불가** | §4 |
| 숫자 타입 | ✅ 폭 보존, 박싱은 옵셔널·컬렉션 원소에 한정 | §1 |
| 널러빌리티 | ✅ 전 API 보존 | §2 |
| 컬렉션 | ✅ 순서 보존, 빈 컬렉션 ≠ nil 구분 유지 | §5 |
| 상수/companion | ✅ 접근 가능 (복사본 여부는 21 문서) | §7 |
| 색/좌표 표현 | ✅ 코어는 역할(enum)·정규화 0~1만 운반 — 색 실값은 코어를 안 지남 | §8 |
| 이름 매핑 | ✅ 충돌 변형은 `copy`→`doCopy`뿐, 오버로드 안전 | §9 |

## §1 숫자 타입

- `Double`(non-null) → C `double` 원시 노출. 예: `LumipolGraph.h:271` `@property (readonly) double position;`
- `Double?` → `LumipolGraphDouble *`(NSNumber 서브클래스) 박싱. 예: `LumipolGraph.h:298`
  `@property (readonly) LumipolGraphDouble * _Nullable splitTimeSeconds;`
- `Int` → `int32_t`. 예: `LumipolGraph.h:295` `@property (readonly) int32_t maxTicks;`
- `List<Double>` → `NSArray<LumipolGraphDouble *>` — **원소 단위 박싱**. 예: `LumipolGraph.h:744`
  `@property (readonly) NSArray<LumipolGraphDouble *> *ticks;`
  값 손실은 없으나(원시 double 보존) 대량 시리즈에서 언박싱 비용 존재. 성능 항목이지 동등성 항목 아님.
- 코어 공개 API에 `Float` 없음 — `Float↔CGFloat` 이슈는 브릿지가 아니라 렌더러 내부(iOS CGFloat=64bit vs Compose Float=32bit, 05 문서 T2 렌더러 한계 1e-6의 근거).

## §2 널러빌리티

- 코어 옵셔널 전부 `_Nullable`로 보존 확인: `PaceSamplePoint.heartRate/cadence/altitude`
  (`LumipolGraph.h:541-543` `LumipolGraphDouble * _Nullable heartRate`),
  `BarLayout.endMinutes`(`:340`), `Marker.label`(`:491`), `DonutSegment.label`(`:421`),
  `ZoneBpmRange.upper`(`:717`), `PaceSeriesResult.altitudeArea`(`:569`),
  `Stats.segmentSeriesId`(`:704`), `toggleSelection`의 인자/반환(`:166`).
- 제네릭 내부 널러빌리티 소실 우려(`List<String?>` 류): 코어 API에 **해당 형태 없음** — 컬렉션 원소는 전부 non-null.

## §3 기본 인자 — 전면 소실 (ObjC export 한계)

Kotlin 기본 인자는 ObjC 헤더에서 사라진다. 전 생성자가 전 인자 필수:

- `BarChartData` — Kotlin은 8개 중 6개 기본값(`BarInput.kt:15-25`), ObjC는 **8개 전부 필수**
  (`LumipolGraph.h:290` `initWithSamples:splitDistanceMeters:targetPaceSecPerUnit:toleranceSecPerUnit:maxTicks:splitTimeSeconds:totalDurationSeconds:totalDistanceMeters:`)
- `ChartConfig(segmentCount, maxTicks)` — Kotlin 기본 `(0, 5)`(`Input.kt:24`), ObjC 둘 다 필수(`:364`)
- `Series(id, points, axis, role)` — Kotlin 기본 `PRIMARY/MAIN`, ObjC 4개 필수
- `Marker`, `RefBand`, `niceScale(..., maxTicks: 5, headroomFraction: 0.0)`(`:788` — 4개 전부 필수) 동일
- **완화 장치 확인**: `DonutSegment`만 2-인자 보조 생성자를 코어가 명시 제공
  (`DonutInput.kt:13-14` `// ObjC export는 기본 인자를 내보내지 않는다 — 기존 Swift 호출부(value:colorRole:) 보존용.`
  → `LumipolGraph.h:414-415`에 init 2종 노출). 나머지 타입엔 이 완화가 없다.

**위험 시나리오**: 코어가 기본값을 바꿔도(예: `toleranceSecPerUnit 10.0`) iOS 호출부는 자기가
하드코딩한 옛 값을 계속 넘긴다 — 기본값 변경이 Android에만 적용되는 반쪽 배포가 조용히 성립한다.
실제로 iOS 렌더러/앱이 어떤 리터럴을 넘기는지는 21-constants-diff에서 대조.

## §4 enum/sealed

- 코어 enum(`Axis`, `SeriesRole`, `ChartAxis`, `BarColorRole`, `DonutColorRole`)은
  `LumipolGraphKotlinEnum` 서브클래스 + 클래스 프로퍼티로 노출 (`LumipolGraph.h:253-261`).
  **Swift enum이 아니므로 switch exhaustive 검사 불가** — 코어에 케이스를 추가해도
  (예: 존6 추가, 새 SeriesRole) iOS 쪽 누락 분기가 컴파일 타임에 안 잡힌다.
- sealed class는 코어 공개 API에 없음(현재는 위험 미현실화, 정책상 주의만).
- `entries`/`values()` 노출은 정상(`:260`).

## §5 컬렉션

- `List` → `NSArray` 순서 보존. 빈 리스트는 빈 NSArray로 노출되고, null 가능 리스트
  (`altitudeArea`)만 `_Nullable` — **빈 컬렉션 vs nil 의미 구분이 브릿지에서 유지된다**
  (`LumipolGraph.h:569` `NSArray<LumipolGraphPoint *> * _Nullable altitudeArea` vs `:573` non-null `pace`).
  코어 규약(Output.kt:73 "미가용이면 emptyList", 고도만 null)이 그대로 전달됨.
- `Set<Int>` → `NSSet<LumipolGraphInt *>` (`:570` `availableSeries`). Swift에서 `contains(KotlinInt)`
  동작은 NSNumber 동등성 기반이라 안전. 단 Swift `Int`와 직접 비교하려면 변환 필요 — 호출부 인벤토리에서 확인.

## §6 예외 — @Throws 부재

헤더 전체에 `error:` 패턴(NSError 브릿징) **0건**. 코어의 `require()`는 iOS 경계에서
잡을 수 없는 크래시가 된다:

- `LineChartEngine.kt:46` `require(xMax > xMin) { "xMax must be > xMin" }` — 줌/뷰포트 경로.
  iOS 렌더러가 `RDChartView.swift:334`에서 `xMin: state.window.lowerBound`로 호출 —
  창 폭 0이 들어오면 앱 크래시
- `NiceScale.kt:26-27` `require(maxTicks >= 2)`, `require(headroomFraction >= 0.0)`
- `BarChartEngine.kt:38` `require(unit > 0)`
- `SeriesSelection.kt:54` `require(index >= 0)` (slotAxis)

Kotlin/Native에서 처리되지 않은 Kotlin 예외가 ObjC 경계를 넘으면 프로세스 종료다.
Android는 같은 예외가 잡을 수 있는 `IllegalArgumentException`으로 전파된다 — **같은 잘못된
입력에 대해 플랫폼 간 실패 모드가 다르다**(iOS 즉사 vs AOS 크래시 다이얼로그/캐치 가능).
→ 3단계 정책에서 "경계 계약: require 대신 정규화(클램프)로 흡수할지, @Throws로 명시할지" 결정 필요.

## §7 상수/companion

- `Y_AXIS_HEADROOM_FRACTION`은 `LumipolGraphNiceScaleKt.Y_AXIS_HEADROOM_FRACTION`으로 접근 가능
  (`LumipolGraph.h:789`). 렌더러 grep 결과 **양쪽 렌더러 모두 niceScale/헤드룸 상수를 직접
  참조하지 않는다**(스케일 계산이 코어 layout 안에서만 일어남) — 이 상수의 복사본 위험은 현재 없음.
- `PaceSeriesId.PACE/HEART/CADENCE/ALTITUDE` + `LINE_PRIORITY`/`DISPLAY_PRIORITY`는
  `shared` 싱글턴 프로퍼티로 노출(`LumipolGraph.h:212-217`) — 접근 경로 동일성 확보.
  앱이 이 값을 하드코딩(0,1,2,3)하고 있는지는 21 문서에서 대조.

## §8 색/좌표 표현

- 코어는 색을 **역할 enum**(BarColorRole/DonutColorRole)로만 운반, RGB 실값은 렌더러/앱 소유 —
  색 단위(0~1 vs 0~255) 해석 문제는 브릿지가 아니라 렌더러 중복 구현 이슈(1단계 D).
- 좌표는 전부 정규화 0~1 double(`NormalizedPoint`, `AxisTick.position`) — 단위 해석 여지 없음.
  px/dp/pt 환산은 전적으로 렌더러 책임(정책상 올바른 배치).

## §9 이름 매핑

- `copy()` → `doCopy(...)` 변형(`LumipolGraph.h:267` 등) — data class 복사를 iOS가 쓰면 이름이 다름.
  호출부에서 사용 시 혼선 여지만 있고 의미 차이는 없음.
- 오버로드 3종 `layout(data:)`/`layout(data:backgroundArea:)`/`layout(data:xMin:xMax:)`는
  ObjC 셀렉터가 서로 달라(`:188-190`) `doSomethingX_` 류 변형 **없음**.
- `Series.id` → `id` 프로퍼티(`:110`): ObjC 키워드와 충돌하지 않게 노출됨, Swift에서 정상.

## 3단계로 넘길 결정 사항

1. **require() 경계 계약** — 코어가 잘못된 입력을 예외 대신 정규화로 흡수할지(권장: 렌더러가
   임의 가드를 각자 넣으면 그것도 발산 지점), @Throws를 붙일지.
2. **기본 인자 소실 완화** — DonutSegment처럼 보조 생성자를 늘릴지, iOS 렌더러가 기본값을
   코어 상수에서 읽어 채우는 팩토리를 둘지. "iOS 호출부가 코어 기본값을 하드코딩하지 않는다"를
   가드레일(5단계 금지 패턴)에 포함.
3. **enum 비완전 매칭** — 코어 enum에 케이스 추가 시 iOS 렌더러의 default 분기 정책
   (무시? assert?)을 명문화.
