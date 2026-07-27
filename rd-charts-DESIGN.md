# lumipol-graph — 크로스플랫폼 차트 라이브러리 설계

> 최초 작성일: 2026-06-24 · 갱신: 2026-07-24
> 출처: Runday iOS 그래프 개선 논의에서 분리된 독립 프로젝트
> 이 문서는 새 리포 `lumipol-graph`의 `docs/DESIGN.md`로 옮겨 사용한다.
> **북극성: iOS·Android 양쪽에서 쓰는 크로스플랫폼 그래프 SDK.**

## 배경 / 문제

Runday iOS는 현재 DGCharts(danielgindi/Charts)를 로컬 벤더링(140개 .swift 파일)해
라인/바/파이 차트를 그린다. 주요 불만 두 가지:

1. **디자인/커스터마이징 한계** — 범용 라이브러리라 Runday 디자인에 딱 맞추기 어려움
2. **유지보수/레거시 부담** — 140개 벤더링 파일, ObjC 연동, 업데이트 어려움

이미 신규 차트(`RDStatisticsBarChartView`, `RDWorkoutPatternDonutChartView`,
`RDProfilePedometerBarChartView`)는 DGCharts 없이 CoreGraphics로 직접 구현 중 →
탈(脫)DGCharts 방향은 이미 시작됨.

분석 탭(기록 상세 > 분석)의 라인차트(`RDGraphView`, DGCharts 기반)를 고도화하고 싶다는
요구가 이 SDK의 직접적 동기다. 고도화의 목적은 단순 데이터 표시를 넘어 **"이번 운동을
회고하고, 다음 목표가 느껴지게"** 하는 것. (자세한 UX 결정은 §3.)

## 결정된 방향 (확정)

| 항목 | 결정 | 이유 |
|---|---|---|
| 라이브러리 교체 vs 직접 구현 | **직접 구현** | 다른 범용 라이브러리도 같은 디자인 한계·새 레거시를 낳음 |
| 범위 | **크로스플랫폼 + 진짜 코드 공유** (iOS/Android) | 사용자 요구 |
| 아키텍처 | **A안: 공유 코어 + 네이티브 렌더러** | 공유 가치 높은 "계산"만 공유, 렌더링은 네이티브로 느낌·성능 유지 |
| 공유 코어 언어 | **KMP (Kotlin Multiplatform)** | 안드로이드 팀이 이미 Kotlin → Android는 FFI 0, iOS만 신규 비용 |
| 코어 출력 좌표 | **정규화 0~1 좌표만** | 코어가 화면 해상도/스케일을 몰라도 됨(more pure), 픽셀 변환은 렌더러 |
| 리포 구조 | **독립 리포 `lumipol-graph`** (양 앱이 의존) | iOS/Android 앱이 별도 리포라 공유 코어는 제3의 리포여야 함 |
| 첫 파일럿 | **라인 차트(페이스/심박수) — A+C 능력 포함** | 러닝 결과 분석의 핵심 + DGCharts에서 가장 아픈 부분 → 아키텍처 검증값 최고 |
| OSS | **"추출 가능한 구조"로 시작, 나중에 공개** | 처음부터 OSS 운영 부담(문서·이슈·버저닝) 지지 않음. 구조는 OSS 친화적 |

### 명시적 비목표 (YAGNI)
- 처음부터 모든 차트 종류 지원 ❌ (라인 1종부터)
- 웹 타겟 ❌
- "아무 데이터나" 범용 API ❌
- 기존 DGCharts 사용처 일괄 교체 ❌ (병존 후 점진 교체)
- **앱 도메인 로직 ❌** — 어떤 러닝을 비교 대상으로 고를지, 목표 페이스를 뭘로 계산할지,
  인사이트 문장을 어떻게 쓸지, 단위 포맷 — 전부 SDK 밖(=Runday 앱). §4 경계 참고.

### 의도적으로 배제한 대안
- **라이브러리 교체** (다른 OSS 차트 도입): 디자인 한계·새 레거시 그대로 → 기각
- **B안 Compose Multiplatform**: 코드 공유 최대지만 iOS에 Kotlin/Skia UI 런타임 통째 탑재, 네이티브 느낌 포기 → 기각
  - **재검토(2026-07-01)**: "Android가 이미 Compose 기반 + 차트 종류 다수 확장 + 차트는 커스텀 캔버스라 느낌 페널티 작음"이라는 새 사실로 B안을 다시 평가함. 그럼에도 **iOS 네이티브 느낌·기존 UIKit/ObjC(Runday) 통합·Compose-iOS에 런타임 탑재 회피**를 우선해 **현행 A안(공유 코어 + 네이티브 렌더러) 유지로 재확정**. (완전 네이티브·SDK 없음 안은 계산 로직 중복 → iOS/Android 숫자 불일치 위험으로 기각.)
- **C안 WebView + JS 차트**: 코드 공유 가장 쉽지만 스크롤/제스처 지연, 60fps·세밀 터치 약함 → 기각
- **Rust 코어**: iOS 측 더 깨끗하고 웹/WASM 보너스 있으나, 팀이 Kotlin 보유 → KMP가 총 신규 비용 최소

## 3. 고도화 UX 방향 (2026-07-01 확정)

> 이 절은 2026-07-01 시점의 결정 기록이다. **0.17.0에서 LineChart 수평 기준선(`RefLine`)이
> SDK에서 제거**되어, 아래 "목표선"은 더 이상 SDK 기능이 아니다 — 앱이 직접 그리거나
> `RefBand`로 표현한다(7장 0.17.0 항목). 목표 대비 UX 방향 자체는 유효.
> 또한 **0.20.0에서 고스트 시리즈(`SeriesRole.GHOST`)도 제거**됐다 — 소비 앱이 쓰지 않기로
> 해 전용 역할·스타일을 걷어냈다(7장 0.20.0 항목). 아래 C안의 "고스트 선" 서술은 당시 기록이다.

분석 그래프가 "회고 + 다음 목표"를 담는 방식으로 **A안 + C안** 채택:

- **A. 겹쳐보기 + 자동 인사이트** — 페이스·심박을 한 그래프에 겹쳐(이중 Y축) 관계를 봄.
  그래프에 한 줄 해석을 붙임(문장은 앱이 생성).
- **C. 지난 기록 / 목표 대비** — 이번 기록 위에 지난 러닝("고스트" 선)과 목표 페이스
  기준선을 겹침. "얼마나 나아졌나 / 다음 목표선을 넘겼나".

### C안 앱 측 결정 (SDK 밖, Runday 책임)
- **고스트(지난 러닝) 선택**: 자동(비교가능성 우선) + 수동 변경.
  같은 코스(`workoutCourseId`)/같은 플랜 회차 우선 → 없으면 유사 거리(±10~15%)의 직전
  `Track` → 후보 없으면 고스트 생략(목표선만). 사용자가 기록 목록에서 다른 기록으로 교체 가능.
  - 근거: 거리 x축에 두 러닝을 겹치므로 거리·코스가 비슷해야 비교가 의미 있음.
- **목표선 값**: 해당 거리 개인최고(PB) 페이스 · 플랜 목표 페이스(`goalPace`, 있을 때만) ·
  최근 N회 평균 페이스. 셋 다 앱이 계산해서 "기준선 값"으로 SDK에 넣음. SDK엔 다 똑같은 수평선.

## 4. 3계층 분리 + 경계 (핵심 아키텍처)

| 계층 | 위치 | 책임 | 공유 |
|---|---|---|---|
| **① 앱 (호출측)** | Runday iOS / Android | 데이터 선택·목표 계산·인사이트 문장·단위 포맷, Track→숫자배열 변환 | ❌ (앱 전용) |
| **② Core** | `core/` Kotlin (KMP) | 데이터 모델, 도메인→**정규화 0~1** 스케일, 축 tick, 통계(스플릿 포함), 근접점 질의 | ✅ |
| **③ Renderer** | iOS Swift / Android Kotlin | 0~1 좌표 → 픽셀 변환, CoreGraphics/Canvas로 그림, 애니메이션, 터치 | ❌ |

**경계 원칙**: 앱이 SDK를 오염시키지 않도록, "어떤 러닝/목표/문장"은 전부 앱에 두고
SDK는 숫자·기준선·마커만 받는다. 이 선을 지켜야 SDK가 범용으로 남고 Android 재사용이 성립.

```
① 앱  ──(숫자 배열 + 기준선 값 + 라벨 포맷터)──▶  ② Core  ──(정규화 레이아웃)──▶  ③ Renderer
```

## 5. 인터페이스 계약 (SDK의 실체)

이 계약이 SDK의 핵심이자 안정 경계. 코어 내부가 바뀌어도 이 계약이 안 깨지면 렌더러는 안 깨진다.

### Core 입력 — 순수 숫자만, 단위·의미 모름
```
LineChartData(
  series: [Series],            // 1개 이상
  referenceBands: [RefBand],   // 0개 이상 (목표 구간)
  segmentMarkers: [Marker],    // 0개 이상 (km 구분선 등)
  config: Config
)
Series(id, points: [(x, y)], axis: .primary | .secondary, role: .main | .overlay)
RefBand(lower, upper, axis)
Marker(x, label?, emphasis: Bool)
```

> **0.17.0 breaking**: 수평 기준선(`referenceLines`/`RefLine`)은 LineChart에서 제거됐다.
> 단일 목표선이 필요하면 폭이 좁은 `RefBand`로 대체한다. 상세·마이그레이션은 7장 0.17.0 항목 참조.
> (BarChart의 목표/평균 기준선은 별개 경로 — `BarChartData.targetPaceSecPerUnit` →
> `BarChartLayout.referenceLinePosition`이며 이 제거의 영향을 받지 않는다.)

### Core 출력 `LineChartLayout` — 렌더러가 받는 전부, 픽셀 모름
```
LineChartLayout(
  series:    [ (id, normalizedPoints: [(0~1, 0~1)]) ],
  axisTicks: [ (axis, [(label, 0~1)]) ],        // 축별 tick 위치
  refBands:  [ (0~1, 0~1) ],
  markers:   [ (0~1, label?, emphasis) ],
  stats:     Stats(                              // 시리즈별 통계
               perSeries:       [(min, max, avg)],
               segments:        [(min, max, avg, count)], // 구간(km 등) 스플릿. count=0 → 빈 구간(렌더러가 skip)
               segmentSeriesId: String?          // 스플릿이 어느 시리즈 것인지 (없으면 null)
             )
)
query(x) -> [ (seriesId, nearest (x, y)) ]        // 터치 마커용. 주의: 입력 x는 원본 도메인 값(정규화 0~1 아님) — 렌더러가 터치→원본 x 변환 후 호출
query(x, xMin, xMax) -> [ … ]                     // 창 인식판(0.10.0) — 줌 상태 스크럽은 표시 창 안 점만 후보로(창 밖 전역 최근접이 창 안 이웃을 가리는 것 방지)
```

> **불변식(축-출력 일관성)**: 출력 요소(시리즈·밴드)가 걸린 모든 축은 반드시 `axisTicks`에 대응 항목을 가진다 — 렌더러가 밴드를 그릴 축을 항상 찾을 수 있게. (밴드 경계값이 `yValues`에 포함되어 도메인·tick이 생성되므로 구조적으로 보장됨.)

### A·C가 이 계약에 요구하는 능력
- **A →** 한 차트에 **2개 이상 시리즈 + 이중 Y축**(`.primary`/`.secondary` — 페이스·심박 스케일 다름)
- **C →** **수평 목표 밴드**(`referenceBands`). 겹쳐 그릴 비교선은 0.20.0에서 전용 역할
  (`role: .ghost`)이 사라져, 같은 축 `.main` 시리즈를 하나 더 넣거나 자체 정규화 `.overlay`를 쓴다
- **공통 →** **구간(km) 스플릿 통계**(`stats.segments`) + 터치 근접점 질의(`query`)

인사이트 "탐지" 로직(예: 심박↑와 페이스↓가 겹치는 구간 찾기)은 처음엔 SDK에 안 넣음(YAGNI).
SDK는 스플릿·통계 같은 순수 값만 제공하고, 문장은 앱이 만든다. 필요해지면 나중에 코어의 순수 함수로 승격.

## 6. 리포 구조

```
lumipol-graph/                  ← 신규 독립 GitHub 리포 (KMP)
├── core/                   ← Kotlin Multiplatform 공유 코어 (UI 0)
│   ├── 데이터 모델 (LineChartData / Series / RefBand / Marker)
│   ├── 스케일링 (도메인 → 정규화 0~1, 축별 min/max)
│   ├── 축 눈금 "nice tick" 계산
│   ├── 통계 (평균/최소/최대, 구간 스플릿) + 근접점 질의
│   └── commonTest 단위 테스트
├── ios-renderer/           ← Swift Package
│   ├── core의 xcframework 소비 (SPM binary target)
│   └── RDChartView (UIView) — CoreGraphics/SwiftUI 렌더 + 터치
├── android-renderer/       ← Kotlin: Canvas/Compose 렌더러
└── samples/                ← 양 플랫폼 데모 앱
```

- iOS Runday는 `lumipol-graph`를 **SPM 의존성**으로 소비 (코어=xcframework, 렌더러=Swift Package).
- Runday는 라이브러리의 한 명의 "사용자"가 된다.

## 7. Runday 통합 / 마이그레이션 경로 (점진)

1. iOS 렌더러가 `RDChartView`(UIView 서브클래스) 제공 → DGCharts와 **병존**
2. **분석 리포트 페이스 그래프**에 먼저 적용해 실사용 검증 (§10 확정된 결정)
3. 검증 후 나머지 라인차트 사용처 점진 교체, DGCharts 라인 의존 제거
4. 바/파이/도넛은 범위 밖 → DGCharts 또는 기존 CoreGraphics 커스텀 유지
- ObjC 화면(`RDAnalyticsReportViewController.m` 등)에서 쓰려면 `RDChartView`를 `@objc` 노출하거나 Swift 래퍼로 감쌈.

### Runday 프로젝트 규칙 연동 (중요)
- **단위 변환은 코어가 하지 않는다.** 거리·페이스·시간 포맷은 Runday의 `RunPaceUtils`로 처리한
  라벨 문자열을 코어/렌더러에 전달. (코어는 숫자만, 표시 포맷은 호출측)
- 데이터 소스: 분석 그래프는 `Track`(러닝 1회) + `TrackPoint`(거리·시간·심박·페이스…)에서 나옴.
  고스트/목표선 계산도 다른 `Track` 조회로 앱이 수행.
- 신규 파일명 `RD` 접두사 규칙은 Runday 쪽 래퍼에만 적용 (라이브러리 내부는 자체 네이밍).

### 시간모드 스플릿 + 경계 확장 (0.6.0)
- 막대 차트에 시간 버킷 모드(`BarChartData.splitTimeSeconds`) 추가 — N분 구간 페이스 막대.
- 버킷 크기 선택(`chooseTimeBucketSeconds`)·색 기준 평균 페이스·x라벨 정수(`endMinutes`)를
  코어로 이관. 두 앱이 색·라벨까지 동일. 초→분·나눗셈은 로케일 비의존이라 코어 순수성 유지.
- 문자열 조립·단위(km/mile) 선택은 여전히 앱 책임.

### 페이스 전처리 + 선택 규칙 이관 (0.7.0)
- 페이스 시계열 전처리(`PaceSeriesEngine.preprocess`) 코어화 — 절대필터·심박/케이던스 결측승계·
  p95×1.25 아웃라이어 컷·다운샘플·best/valid 집계·고도 평지판정. 튜닝 상수 전부 코어.
- 멀티지표 선택/슬롯 규칙(`SeriesSelection`) 코어화 — 도메인 프리 정수 id. 앱이 지표↔id 매핑.
- 앱은 포인트별 페이스 계산(RunPaceUtils·워치/GPS)·x 누적(단위)·LineChartData 조립만 유지.

### 심박존 집계 이관 (0.8.0)
- 심박존 집계(`HeartRateZoneEngine.calculate`)와 존 bpm 경계(`zoneBpmRanges`) 코어화 —
  공유 상수 `ZONE_LOWER_FRACTIONS`(50/60/70/80/90%)로 도넛·범례 경계 일치를 코어가 보장.
- 최대심박 공식(220-나이/206-0.88×나이)은 러닝 도메인이라 앱 유지. 도넛 각도는 기존 DonutEngine.

### 배경 area 스크럽 보간 이관 (0.9.0)
- 배경 area(고도 실루엣) 스크럽 실값 보간(`query.interpolatedY`) 코어화 — iOS 렌더러
  `RDChartView.backgroundValue`(이진 탐색 + 선형 보간·양끝 클램프)를 그대로 이관.
  근접점 질의(`nearest`)와 같은 `query` 패키지로, 양 플랫폼 렌더러가 동일 보간을 공유.
- 렌더러는 render 시 area 포인트를 코어 `Point`로 1회 변환·보관하고 스크럽(60~120Hz)마다
  `LineChartEngine.interpolatedY`를 호출. 값의 단위 포맷("123m" 등)은 여전히 앱 책임.

### 배경 area 단독 차트 지원 (0.10.0, 렌더러 전용)
- 시리즈 0개 + 배경 area만 있는 차트(선택 라인 지표에 데이터가 없는 기록)에서
  스크럽이 무반응이던 문제 수정 — 근접점 스냅 격자가 없으므로 rawX 그대로의
  수직선 마커(`TouchMarker.makeBackgroundOnly`)로 폴백해 `didScrubTo([:])` +
  배경 보간값 콜백을 발화. 시리즈가 있으면 기존 스냅 계약 유지(스냅 실패 시 무마커).
- 시리즈가 없으면 코어 layout의 X 도메인이 기본 0~1로 붕괴해 실루엣·축·스크럽
  좌표계가 어긋난다 — area x범위로 창(windowed) layout을 만들어 보정(1x·줌 해제 공통).

### 창 인식 근접점·도넛 원본 인덱스·area 인식 layout 코어화 (0.10.0, 코어)
고강도 코드리뷰(2026-07-11) 후속 — 렌더러가 복제하던 플랫폼 중립 규칙 4건을 코어로 이관,
iOS/Android 렌더러 모두 마이그레이션 완료.
- `query.nearest(data, x, xMin, xMax)`: 표시 창 안 점만 고려하는 근접 질의. 줌 가장자리에서
  창 밖 전역 최근접점이 스냅 소스가 되어 창 안 이웃이 있어도 마커가 침묵 드롭되던 버그의
  공통 수정(양 렌더러 TouchMarker가 이 오버로드 사용).
- `DonutSegmentLayout.sourceIndex`: 원본 `segments` 인덱스를 레이아웃이 직접 운반 —
  렌더러 히트테스트가 엔진의 value<=0 필터 규칙을 복제하지 않는다(규칙 변경 자동 추종).
- `LineChartEngine.layout(data, backgroundArea)`: 위 0.10.0 렌더러 보정(area x범위 X 도메인)의
  코어 이관 — 시리즈 없는 area 단독 기록의 좌표계 책임을 코어가 진다.
- `query.heightFractions(values)`: 실루엣 높이 min~max 정규화 코어화(interpolatedY와 같은 사유).
  축퇴(전부 동일) 시 **모두 0(평지)** — `AxisDomain.normalize`(0.5)와 다른 의도된 의미론.
- 동시 반영: 양 플랫폼 ZoomState의 완전 줌아웃 ulp 스냅(부동소수 재구성이 fullDomain과 1 ulp
  어긋나 isZoomed가 영영 true로 남던 버그) 수정.
  코어는 area를 모르므로(렌더러 장식) 렌더러 책임, 코어 변경 없음(xcframework 재빌드 불필요).

### iOS 렌더러 패리티 수정 2건 (2026-07-14, 렌더러 전용)
포팅 패리티 감사에서 Android 쪽이 옳다고 판단된 2건을 iOS에 역이식.
- **등장 애니메이션 뷰 수명당 1회**: `render()`마다 재무장하던 것을 최초 render에서만 —
  스트리밍 데이터 갱신 시 라인이 매번 0%부터 다시 그려지던 동작 제거(Android와 동일 계약).
  최초 render가 애니 비활성이면 이후 활성화해도 재생 없음(컴포지션 시점 고정 의미론).
- **스크럽 중 데이터 갱신 시 `chartViewDidEndScrub` 통지**: `render()`가 표시 중 마커를
  무통지 제거해 `didScrubTo` 짝이 깨지던 것을 종료 1회 통지로 수정(Android endScrub 동치).

### LineChart 수평 기준선(RefLine) 공개 API 제거 (0.17.0, 2026-07-24, **breaking**)
LineChart의 목표 페이스용 수평 기준선을 라이브러리에서 완전히 제거. 소비 앱이 목표선을
직접 그리거나 `RefBand`로 표현하는 쪽이 낫다는 판단(SDK는 밴드 하나로 충분).

**제거된 공개 API** — 아래를 쓰던 코드는 컴파일 에러가 난다.
- 입력: `LineChartData.referenceLines`, `RefLine` 타입
- 출력: `LineChartLayout.refLines`, `RefLineLayout` 타입
- 스타일: `ChartStyle.refLineColor` (양 플랫폼)
- iOS는 ObjC 심볼 `LumipolGraphRefLine`/`LumipolGraphRefLineLayout`도 사라진다.

**마이그레이션**
- 단일 목표선 → 폭이 좁은 `RefBand(lower: v - ε, upper: v + ε, axis:)`로 대체.
  단, 밴드는 라벨을 갖지 않으므로 `"목표 5'30\""` 같은 텍스트는 앱이 직접 그려야 한다.
- `ChartStyle`을 팩토리(`light()`/`dark()`, `.default`) 없이 **직접 생성**하던 코드는
  Android에서 `refLineColor`가 기본값 없는 필수 인자였으므로 해당 인자만 지우면 된다.
- **동작 변경(조용한 회귀 주의)**: 기준선 값이 더 이상 Y 도메인(`yValues`)에 기여하지 않는다.
  밴드 없이 기준선만으로 축 범위를 넓히던 차트는 **Y축 스케일이 좁아진다**. 기존 범위를
  유지하려면 그 값을 `RefBand`로 옮기거나 시리즈 데이터가 덮게 해야 한다.

**영향 없음(독립 경로)**: BarChart의 목표/평균 기준선(`targetPaceSecPerUnit` →
`BarChartLayout.referenceLinePosition` → `barRefLine` 레이어 / `barReferenceLineColor`)은
그대로다. 공유 스타일 `refLineDashPattern`도 **BarChart가 계속 쓰므로 유지**한다
(이름이 LineChart 유산이라 오해 소지가 있으나, 추가 breaking을 피해 이번엔 보존).

**xcframework 재빌드 필요** — 코어 공개 타입이 바뀌므로 `scripts/sync-xcframework.sh`로
재생성한 바이너리를 같은 커밋에 포함해야 iOS 테스트/샘플이 빌드된다.
iOS 스냅샷 3건(`testGhostAndBand`·`testTouchMarkerShown`·`testZoomedWindow`)도 재녹화 대상.

### 지표 가용성 계약 코어 이관 (0.18.0, 2026-07-24, **breaking**)
"이 기록에 어떤 지표가 있는가"를 소비 앱이 각자 판정하던 구조를 코어로 옮겼다. 같은 규칙을
Android/iOS가 따로 구현하고 있어 실제로 페이스 판정 기준이 갈렸고(`!pace.isEmpty` vs
`validPaceCount > 10`), 고도는 평지 컷(0.5m) 때문에 "미측정"과 "평탄한 코스"를 구분할 정보
자체가 앱에 전달되지 않았다.

**결측을 타입으로** — `PaceSamplePoint.heartRate`/`cadence`/`altitude`가 `Double?`가 됐다.
- 센티널(0, -100)이나 "값이 전부 같으면 미측정" 추론은 폐기. 필드마다 규약이 달랐던 문제도 해소.
- 결측 승계를 코어가 수행한다 — 앱 매퍼의 sanitize 단계는 제거하고 DB 센티널을 nil로 접는 일만
  남긴다. 규칙은 세 지표가 동일하다: 직전 유효값으로 채우고, **앞쪽 결측은 첫 유효값으로 소급**.
  한 지표만 소급하면 나머지는 결측 자리에 0이 남는데, 아래 이유로 그 0은 실측과 구분되지 않는다.
- 부수 효과: **0은 이제 실측값**이다. 심박 0을 미측정으로 쓰던 데이터는 매퍼에서 걸러야 한다.

**가용성을 출력으로** — `PaceSeriesResult.availableSeries: Set<Int>` 신설(id는 `PaceSeriesId`).
- 페이스 최소 표본 규칙(구 앱의 `validPaceCount > 10`)이 코어 상수로 들어왔다.
- 판정은 다운샘플 **이전** 원본 기준이라 표본 수에 따라 흔들리지 않는다. 동시에 실제 출력
  리스트가 비면 지표로 치지 않아, "count는 11인데 리스트는 빈" 틈이 사라진다.
- **가용 ⟺ 필드 비어있지 않음**이 네 지표 공통 불변식이다. 미가용이면 `pace`/`heart`/`cadence`는
  `emptyList`, `altitudeArea`는 nil. 한 지표만 필드를 채워두면 `availableSeries`를 안 보는 앱에서
  "코어는 없다는데 그려지는" 틈이 생긴다. 단 `bestPaceSeconds`·`validPaceCount`는 라인 표시와
  무관한 집계값이라 페이스가 미가용이어도 그대로 낸다.
- 고도 평지 컷 제거: 측정만 됐으면 고저차가 0이어도 `altitudeArea`를 반환한다.

**선택 규칙 완결** — `SeriesSelection.normalized(current:available:linePriority:maxCount:)` 신설
(0.21.0에서 `normalized(current:available:priority:)`로 시그니처 변경 — 아래 0.21.0 항목 참조).
- 앱이 각자 들고 있던 "숨긴 지표를 선택에서 빼고 라인이 비면 채우기"가 코어 단일 소스가 됐다.
- `toggled`의 최소1 보호를 `lineItems` 한정에서 **선택 전체**로 넓혔다. 고도만 측정된 기록에서
  고도 칩을 끄면 선택이 비어 "데이터 없음"이 뜨던 버그(양 플랫폼 공통)를 한 곳에서 잡는다.
- 같은 보호를 `maxCount` 초과 **축출**에도 적용했다. 축출이 무조건 index 0이면 비라인 추가 한
  번으로 유일한 라인이 밀려 라인 0개가 된다(비라인 id가 둘 이상인 앱에서 도달 가능). 새 항목이
  라인이면 라인 수가 보전되므로 기존 "가장 오래된 것부터" 순서 그대로다.

**노이즈 하한을 렌더로** — `heightFractions(values, minSpan)` / `ChartStyle.areaMinValueSpan`(기본 0.5).
- 실루엣은 자체 min~max로 0~1 정규화하므로 고저차 0.2m도 플롯 전체를 채운다. 0.5m 컷이
  사실상 이 하한을 겸하고 있었는데, 그 역할은 "그릴지"가 아니라 "얼마나 크게 그릴지"에 속한다.

**마이그레이션**: `PaceSamplePoint` 생성부(양 앱)를 nullable로 바꾸고, 앱의 availableMetrics·
선택 정규화 구현을 `availableSeries`/`SeriesSelection.normalized` 위임으로 교체한다.
지표↔id 매핑은 `PaceSeriesId`를 참조해 양 플랫폼이 같은 번호를 쓰게 한다.

**xcframework 재빌드 필요** — 코어 공개 타입이 바뀌므로 `scripts/sync-xcframework.sh`로
재생성한 바이너리를 같은 커밋에 포함해야 iOS 테스트/샘플이 빌드된다.

### 고스트 시리즈(`SeriesRole.GHOST`) 제거 (0.20.0, 2026-07-24, **breaking**)
"지난 러닝을 흐린 점선으로 겹쳐 그린다"는 전용 역할을 라이브러리에서 완전히 제거. 소비
앱(Runday)이 고스트 기능을 쓰지 않기로 해, 역할 하나를 위해 양 플랫폼이 스타일 3개와
전용 렌더 경로·터치 도트 분기를 유지할 이유가 없어졌다.

**제거된 공개 API** — 아래를 쓰던 코드는 컴파일 에러가 난다.
- 입력: `SeriesRole.GHOST` (`SeriesRole`은 이제 `{ MAIN, OVERLAY }`)
- 스타일: `ChartStyle.ghostLineColor` / `ghostLineWidth` / `ghostDashPattern` (양 플랫폼)
- iOS는 ObjC 심볼 `LumipolGraphSeriesRole.ghost`도 사라진다.
- 렌더 산출물: `series.ghost.{id}` 레이어가 더 이상 생성되지 않는다(레이어 이름으로 찾던 테스트 주의).

**마이그레이션** — 비교선을 계속 그리려면 둘 중 하나로 옮긴다. 성격이 다르니 골라야 한다.
- **같은 축 `.main` 추가**: y가 현재 기록과 같은 축·같은 도메인으로 정규화돼 비교가 수치적으로
  유효하다. 시리즈마다 area 그라데이션이 붙지만 0.21.0의 α/√n 감쇠로 두 장이면 장당
  0.25/√2 ≈ 0.177, 겹친 구간 합성 ~0.32다. 같은 축 두 `.main`의 라인·그라데이션·터치 도트
  색은 `ChartStyle.seriesColors`(0.21.0)로 시리즈별로 구분한다(예: 비교선 id에 회색 지정).
  그라데이션 중첩 자체가 싫으면 `gradientMaxAlpha = 0`으로 끈다.
- **`.overlay`**: 자체 min~max로 재정규화되고 Y 도메인에서 제외되며 축 반전도 무시한다
  (`pointIgnoringInversion`). 즉 **모양만 겹쳐 보이고 y 위치는 현재 기록 축과 무관**하다 —
  반전된 페이스 축에서는 지난 러닝의 가장 느린 구간이 아래에 그려진다. 스케일이 다른 보조
  지표용이지 "같은 단위의 지난 기록" 비교용이 아니다.
- `ChartStyle`을 팩토리(`defaults()`/`.default`) 없이 **직접 생성**하던 코드는 Android에서
  `ghostLineColor`가 기본값 없는 필수 인자였으므로 해당 인자만 지우면 된다.

**xcframework 재빌드 필요** — 코어 공개 타입이 바뀌므로 `scripts/sync-xcframework.sh`로
재생성한 바이너리를 같은 커밋에 포함해야 iOS 테스트/샘플이 빌드된다.
iOS 스냅샷 3건(`testGhostAndBand`→`testDualAxisAndBand`·`testTouchMarkerShown`·
`testZoomedWindow`)도 재녹화했다.

### SeriesSelection 단순화 + 시리즈별 색·그라데이션 (0.21.0, 2026-07-26, **breaking**)
선택 규칙에서 상한·라인최소1·축출을 제거하고(동시 선택 무제한, 고도 단독 허용), 같은 축에
여러 `.main`을 겹치는 사용(비교선)을 렌더러가 색·그라데이션으로 지원한다.

**breaking — `SeriesSelection` 시그니처 변경.** 아래를 쓰던 코드는 컴파일 에러가 난다.
- `toggled(current:toggling:lineItems:maxCount:)` → `toggled(current:toggling:)`.
  남은 규칙은 "마지막 하나 해제 무시"뿐 — 상한 축출·라인 최소1 보호가 사라졌다.
- `normalized(current:available:linePriority:maxCount:)` → `normalized(current:available:priority:)`.
  **`priority`는 라인만이 아니라 고도 포함 전체 표시 우선순위다** — 기존 호출에서 maxCount만
  지우고 `LINE_PRIORITY`를 그대로 넘기면 고도만 측정된 기록에서 폴백이 실패해 빈 선택
  ("데이터 없음")이 된다. 코어 상수 `PaceSeriesId.DISPLAY_PRIORITY`(0.22.0)를 쓴다.
- `assignSlots(priority:selected:withData:slotCount:)` → `assignSlots(priority:selected:withData:)`.
  상한 없음 — index 2+는 전부 overlay. 여기의 `priority`는 종전대로 `LINE_PRIORITY`(축 슬롯
  배정이므로 고도 제외)가 맞다.

**렌더러 — 시리즈별 색·배경 그라데이션 (양 플랫폼)**
- `ChartStyle.seriesColors`(id→색 Map) 신설. 라인·배경 그라데이션이 맵 색을 쓰고(축 슬롯 색보다
  우선), 비어 있으면 종전 축/역할 폴백. 같은 축 두 `.main`(비교선)을 색으로 구분하는 수단.
- 배경 그라데이션을 2패스로 재구성: 모든 시리즈(overlay 포함) 그라데이션 → 모든 시리즈 라인.
  겹침 탁해짐 방지로 시작 알파를 `gradientMaxAlpha / √n`(n=그라데이션 장수)로 감쇠한다.
  한계: n은 실제 겹침이 아니라 전체 그리기 가능 시리즈 수고, 합성 불투명도는 `1-(1-α/√n)^n`로
  n과 함께 서서히 는다 — 동시 선택이 많은 화면은 값을 낮추거나 0으로 끈다.

**xcframework 재빌드 필요** — 코어 공개 API가 바뀌므로 재생성 바이너리를 같은 커밋에 포함.
다중 배경 스냅샷도 재녹화했다.

### 코드리뷰 후속 — DISPLAY_PRIORITY·터치 도트 색 통일 (0.22.0, 2026-07-27)
0.21.0 코드리뷰에서 확인된 결함 수정. breaking 없음(공개 API는 추가만).

- **`PaceSeriesId.DISPLAY_PRIORITY` 신설** — `LINE_PRIORITY + ALTITUDE`. `normalized`의
  priority 계약(고도 포함)을 충족하는 코어 상수. 앱이 손으로 `+ altitude`를 조립할 필요가
  없어졌다. `LINE_PRIORITY`는 `assignSlots` 전용임을 KDoc에 명시.
- **터치 스크럽 도트 색 버그 수정(양 플랫폼)** — 도트가 축 슬롯 색만 쓰고 `seriesColors` 맵을
  무시하던 것을 라인·그라데이션과 같은 `seriesColor` 리졸버로 통일. 맵 미지정 스타일은 종전과
  동일한 색이다(스냅샷 영향 없음).
- **build 핫패스 재계산 제거(양 플랫폼, 동작 불변)** — 시리즈별 매핑 포인트/경로를 리빌드당
  최대 3회 계산하던 것을 1회로. 역할→좌표 매핑 규칙도 플랫폼당 한 곳(`mappedPoints`/
  `seriesPoints`)으로 단일화 — 그라데이션 닫는 변이 라인과 같은 매핑 결과를 공유한다.

**xcframework 재빌드 필요** — 코어 공개 API 추가이므로 재생성 바이너리를 같은 커밋에 포함.

### 오버레이 라인 점선 → 실선 (0.23.0, 2026-07-27, **breaking**)
페이스·심박·케이던스·고도를 모두 선택하면 케이던스가 축 슬롯 3번째라 `.overlay`가 되고
점선으로 그려지던 것을 실선으로 바꿨다. 오버레이도 다른 지표와 같은 실선으로 읽혀야 한다는
제품 판단이다. 폭(`overlayLineWidth` 1.5)은 그대로라 main(2.0)보다 가늘다.

**제거된 공개 API** — 아래를 쓰던 코드는 컴파일 에러가 난다.
- 스타일: `ChartStyle.overlayLineDashPattern` (양 플랫폼)
- Android에서 `scaledForDensity`의 dash 스케일 대상에서도 빠졌다.

**마이그레이션**: 이 속성을 설정하던 코드는 지우면 된다. 오버레이를 계속 점선으로 그릴
방법은 라이브러리에 없다 — 필요하면 이슈로 되살린다. 오버레이만 다르게 보이게 하려면
`ChartStyle.seriesColors`로 해당 id에 옅은 색을 준다.

**주의 — 시각 신호가 사라진 자리**: 오버레이는 자체 min~max로 재정규화되고 Y 도메인에서
빠지며 축 반전도 무시한다(0.20.0 항목 참조). 즉 **y 위치가 어느 축과도 무관**한데, 점선이
그 "축 밖" 신호였다. 실선이 되면 옆의 main 라인과 폭·색으로만 구분되므로, 반전된 페이스
축에서 오버레이를 축 눈금으로 읽으면 방향이 거꾸로다. 오버레이 색은 축 색과 확실히
구분되게 지정하는 편이 좋다.

**xcframework 재빌드 불필요** — 렌더러 전용 변경이고 코어 공개 타입은 그대로다.
iOS 스냅샷 1건(`testThreeSeriesStackedGradients`)을 재녹화했다.

### 오버레이 시리즈 터치 도트 추가 (0.24.0, 2026-07-27)
롱프레스 스크럽 시 오버레이 시리즈(4지표 선택 시 케이던스)에 터치 도트가 안 그려지던 것을
수정. 종전에는 "축이 없어 tick 스케일 역산 불가"로 값만 전달하고 도트를 생략했는데, 라인이
쓰는 것과 같은 소스 — 코어가 자체 정규화한 layout 포인트 — 에서 근접점을 찾아 라인과 동일한
반전 무시 매핑(`pointIgnoringInversion`)으로 도트를 놓는다. x는 다른 도트처럼 수직선 위치,
색은 `seriesColor` 리졸버(맵 우선) 공용. breaking 없음(공개 API 불변).

- 렌더 산출물: `touch.dot.{overlayId}` 레이어가 새로 생성된다(레이어 이름 기반 테스트 주의).
- 도트 y는 그 시리즈 자신의 근접 포인트 값 — 성긴 오버레이에서는 수직선 x와 어긋날 수 있으나
  케이던스는 다운샘플되지 않아 실사용 오차는 무시 가능.

**xcframework 재빌드 불필요** — 렌더러 전용 변경이고 코어 공개 타입은 그대로다.

### 페이스 막대 차트 Y축 반전 (0.25.0, 2026-07-27, **breaking(동작)**)
막대 차트의 Y축 방향을 뒤집었다 — 맨 위 틱이 가장 빠른 페이스, 빠른 스플릿일수록 막대가
길다. 종전에는 값 크기 그대로 정규화해(느릴수록 긴 막대) 방향을 색과 y틱으로만 전달했는데,
"위 = 빠름"이 페이스 라인차트(반전 축)와도 일관된다는 제품 판단이다.

- 코어 `BarChartEngine.layout`이 `heightFraction`·`yTicks[].position`·`referenceLinePosition`을
  전부 `1 - normalize(v)`로 뒤집는다. 렌더러(양 플랫폼)는 무변경 — 정규화 값 해석(0=바닥,
  1=천장)이 그대로라 코어 한 곳에서 끝난다.
- 공개 API 시그니처는 불변(컴파일 에러 없음). 단 **모든 소비 앱의 막대 방향이 자동으로
  뒤집히므로** 동작 breaking으로 기록한다. 마이그레이션 코드는 없다.
- 부수 효과: 종전에는 가장 빠른 막대가 `barMinHeight`로 클램프될 만큼 짧았는데, 이제는
  가장 느린 막대가 그렇다(도메인 상한 근처 값 → 높이 ~0).

**xcframework 재빌드 필요** — 코어 동작 변경이므로 재생성 바이너리를 같은 커밋에 포함해야
iOS가 새 동작을 받는다.

### 심박존 도넛 탭 토글 — 센터 라벨·디밍·자동해제·햅틱 (0.26.0, 2026-07-27, **breaking**)
도넛을 누르는 동안만 반응하던 스크럽 상호작용을 탭 토글 선택으로 바꿨다. 조각을 탭하면
선택이 확정되고 중앙에 존 이름·퍼센트가 뜨며 나머지 조각은 디밍된다. 같은 조각을 다시
탭하거나 링 밖(구멍 포함)을 탭하면 해제되고, 일정 시간 조작이 없으면 자동 해제된다.

- **코어** — `DonutSegment.label: String?`(신규 3-인자 생성자 + 기존 `value:colorRole:` 2-인자
  생성자를 별도 constructor로 보존, ObjC export가 기본 인자를 못 내보내는 제약 때문). 라벨을
  넘기지 않으면 센터에 퍼센트만 표시된다. `DonutSegmentLayout.label`이 레이아웃까지 그대로
  운반한다. `DonutEngine.toggleSelection(current:tapped:)` 순수 전이 함수 신설 — 링 밖 탭(`tapped
  == null`)과 같은 조각 재탭은 해제(`null`), 다른 조각 탭은 그 인덱스로 이동. 타이머·통지는
  플랫폼 책임으로 남겨, 코어는 "다음 선택 인덱스"만 결정한다.
- **AOS** — `RDHeartRateZoneChart`가 `detectTapGestures`로 토글을 구현한다. 센터 라벨은
  `TextMeasurer`로 존 이름 1줄 + 퍼센트 1줄을 그리고, 비선택 조각은 `ChartStyle.donutDimmedAlpha`
  (기본 0.3)로 낮춘다. `donutAutoDeselectDelaySeconds`(기본 3초, 0 이하면 자동 해제 없음) 경과 시
  자동 해제되고, `donutSelectionHapticsEnabled`(기본 true)면 선택 전환마다 햅틱을 낸다. 콜백
  캡처가 최신 람다를 참조하도록 `rememberUpdatedState`로 감쌌다 — 그렇지 않으면 자동 해제
  타이머가 구식 `onSelectSegment`를 호출하는 스테일 클로저 버그가 생긴다.
- **iOS** — `RDHeartRateZoneView`가 같은 토글을 구현한다. 센터는 `UILabel` 2개(존 이름·퍼센트)로
  그리고, 비선택 조각 디밍·`Timer` 기반 자동 해제·`UIImpactFeedbackGenerator(.light)` 햅틱을
  갖는다. `touchesBegan`/`touchesCancelled` 오버라이드를 제거했다 — 토글 모델에는 "누르는 동안"
  상태가 없어 두 메서드는 no-op이었다.

**breaking — `didSelectSegmentAt`/`onSelectSegment` 통지 의미 변경.** 종전에는 "누름 시 인덱스
통지 → 뗌/취소 시 `nil`/`null` 통지"였다(스크럽 모델). 이제는 "선택이 **확정**될 때 그 인덱스,
**해제**될 때 `nil`/`null`" — 누름·뗌이 아니라 선택 상태 전환에 매핑된다. 호스트 앱이 "손을 떼면
선택 해제"를 기대하고 작성한 코드(예: 뗌 시 별도로 상태를 지우는 로직)는 이제 중복 해제이거나
자동 해제 타이머와 경쟁할 수 있으니 제거해야 한다. render/data 교체로 인한 리셋은 선택 전환이
아니므로 통지가 발생하지 않는다.

**AOS `ChartStyle` — 신규 필수 색 2종.** `donutCenterLabelColor`/`donutCenterPercentColor`는
기본값이 없다(iOS `.secondaryLabel`/`.label` 대응이라 라이트/다크 팔레트에서 주입). 총 8필드가
추가됐다: 위 2색 외 `donutDimmedAlpha`·`donutCenterLabelFontSize`·`donutCenterPercentFontSize`·
`donutCenterPercentFontWeight`·`donutAutoDeselectDelaySeconds`·`donutSelectionHapticsEnabled`.
`ChartStyle`을 팩토리(`defaults()`/`.default`) 없이 **직접 생성**하던 코드는 필수 색 2종 누락으로
컴파일 에러가 난다 — `defaults()`/`copy()` 사용자는 영향 없음. iOS `ChartStyle`은 7필드 전부
기본값이 있어(색 포함) 직접 생성 호출부도 컴파일 에러가 나지 않는다.

**xcframework 재빌드 필요** — 코어 공개 타입(`DonutSegment`/`DonutSegmentLayout`/
`DonutEngine.toggleSelection`)이 바뀌므로 재생성 바이너리가 필요하다. Task 1(커밋 `372f971`)에서
이미 재생성·커밋 완료 — 이번 릴리스에서 추가 재빌드는 없다. iOS 스냅샷 1건
(`testDonutZoneSelected`)을 신규 추가했다.

### 레전드 등 외부 UI에서 도넛 선택 구동 (0.27.0, 2026-07-27)
목적: 심박존 도넛 밖의 UI(예: 레전드)가 도넛과 같은 선택 상태를 구동·표시할 수 있게 한다. 0.26.0의
탭 토글 선택은 도넛 자체 제스처로만 진입할 수 있었는데, 이제 레전드 항목 탭 같은 외부 입력도
동일한 전이·표시·타이머 경로를 탄다.

- **iOS** — `RDHeartRateZoneView`에 공개 메서드 `selectSegment(at:)` 신설. 기존 `handleTap`의
  몸통을 `applySelection(tapped:)`으로 추출해 탭과 외부 구동이 완전히 같은 경로(토글 전이,
  센터 라벨·디밍 갱신, 자동 해제 타이머 재시작, 햅틱, 델리게이트 통지)를 공유한다. 레이아웃에
  없는 인덱스(범위 밖이거나 `value <= 0`으로 필터된 세그먼트)를 넘기면 대응하는 호가 없으므로
  무시한다(`layoutContainsSegment(at:)`로 사전 검사).
- **AOS** — `DonutSelectionState` 클래스와 `rememberDonutSelectionState(data)` 신설. `toggle(index:)`가
  코어 `DonutEngine.toggleSelection` 규칙을 그대로 적용해 `selectedIndex`를 갱신하되, 레이아웃에
  없는 인덱스(범위 밖이거나 `value <= 0`으로 필터된 세그먼트)는 홀더가 생성 시 받아 둔
  선택 가능 인덱스 집합으로 사전 검사해 무시한다(iOS `layoutContainsSegment(at:)` 패리티).
  `RDHeartRateZoneChart` 시그니처 맨 끝에 `selection: DonutSelectionState =
  rememberDonutSelectionState(data)` 파라미터를 추가 — 앱이 레전드와 차트에 같은 홀더를 넘기면
  상태가 공유된다. `onSelectSegment`는 도넛 자체 제스처와 자동 해제 타이머에서만 발화하며, 앱이
  `selection.toggle()`로 직접 구동한 변경은 재통지하지 않는다(재진입 루프 방지). `onSelectSegment
  == null`이면 도넛 터치가 비활성인 기존 게이팅은 그대로다.

**명명 인자 기준으로 비파괴적 — 마이그레이션 불필요.** iOS는 신규 공개 메서드 추가뿐이고, AOS는
신규 파라미터가 `selection` 하나뿐이며 기본값이 있고 시그니처 맨 끝에 위치해 기존 위치 인자
호출부를 깨지 않는다. 기존 호출자가 이름 붙은 인자로 호출한다면 소스 수정 없이 그대로
컴파일된다 — 단, AOS에서 마지막 파라미터를 트레일링 람다로 넘기던 호출(예:
`RDHeartRateZoneChart(data, modifier) { ... }`)이 있었다면 그 자리가 함수 타입에서
`DonutSelectionState`로 바뀌므로 깨진다(저장소 내 실제 호출부는 전부 명명 인자라 해당 없음).

**xcframework 재빌드 불필요** — 이번 변경은 iOS/AOS 렌더러 전용이고 코어(`core` 모듈) 공개 API에
변경이 없다(0.26.0의 "재빌드 필요"와 대비).

### 라인·막대 Y축 헤드룸 — 입력 인플레이션 5% (0.28.0, 2026-07-27)
목적: Y축이 데이터 min/max에 딱 붙는 문제(예: 심박 100~180 → 축 정확히 100~180, 선이
테두리 밀착) 해소. `niceScale()`에 `headroomFraction`(기본 0.0) 파라미터를 추가해 Heckbert
계산 전에 min/max를 범위의 해당 비율씩 바깥으로 민다. 원래 min ≥ 0이면 0 아래로 내리지
않는다(심박·케이던스 보호, 고도 같은 음수 데이터는 클램프 없음). 라인(`LineChartEngine`,
전체·줌 공용 Y 경로)과 스플릿 막대(`BarChartEngine`)가 `Y_AXIS_HEADROOM_FRACTION`(0.05)으로
opt-in — 양단 대칭이라 렌더러가 축을 반전하는 페이스(위=빠름)도 화면 위쪽 여유를 얻는다.
X축은 기본값 0.0으로 현행(데이터 끝 밀착) 유지.

**비파괴적** — 기본값 있는 마지막 파라미터 추가라 기존 호출부 소스 호환이고, 렌더러는
`niceScale`을 직접 호출하지 않는다. 코어 변경이므로 **xcframework 재빌드 필요**.

### 슬롯 규약 변경 — 케이던스 보조축 합류 + slotAxis (0.29.0, 2026-07-27)
`assignSlots` 결과 index 해석을 "0=primary, 1=secondary, 2+=overlay"에서 **"0=primary,
1 이후=전부 secondary(도메인 공유), role 전부 MAIN"**으로 바꿨다. 4지표 선택 시 케이던스가
축 없는 오버레이(자체 min~max 정규화)로 그려져 우측 축 눈금과 무관한 위치에 놓이던 것이,
심박수와 보조축 도메인을 공유해 눈금으로 정확히 읽힌다. (0.28.0 헤드룸 전에는 심박 축
100~180 기록에서 케이던스 최대 180이 플롯 최상단과 우연히 겹쳐 맞아 보였고, 헤드룸이
축을 100~200으로 늘리자 불일치가 드러났다 — 구조적 문제였다.)

- **`SeriesSelection.slotAxis(index)` 신설** — 0=PRIMARY, 1 이후=SECONDARY, 음수 require.
  index→축 매핑을 앱마다 손으로 두면 플랫폼별로 어긋나므로 코어가 고정한다.
- `SeriesRole.OVERLAY`는 존치 — 축 슬롯 배정에서 안 나올 뿐, 비교선 등 직접 조립 용도는 유효.
- 보조축 도메인은 심박∪케이던스 min~max 병합 — 케이던스 급락(걷기 등) 기록은 축이 아래로
  넓어져 심박 진폭이 눌린다. 축 판독성의 대가로 수용한 제품 판단.
- iOS `paceHeartCadence` 픽스처를 새 규약으로 갱신, `testThreeSeriesStackedGradients` 재녹화.

**마이그레이션(앱)**: 슬롯 index→(axis, role) 매핑 코드를 `slotAxis(index)` + role 고정
`MAIN`으로 교체한다. 교체 전까지 화면은 종전과 같다(라이브러리만 올려도 동작 불변).

**xcframework 재빌드 필요** — 코어 공개 API 추가이므로 재생성 바이너리를 같은 커밋에 포함.

## 8. 1차 파일럿 — 라인차트 수직 슬라이스 (A+C)

> 완료된 파일럿의 당시 범위 기록이다. 아래 "기준선/목표선"은 0.17.0에서, "ghost 선"은
> 0.20.0에서 제거됐다(7장 참조).

KMP코어 → iOS네이티브 렌더 **전체 파이프라인을 끝까지** 증명하는 최소 단위.

**Core (KMP)**
- `LineChartData`(다중 시리즈 + 기준선 + 마커) 입력 → `LineChartLayout` 출력
- 축별 y min/max 자동, "nice tick", 구간 스플릿 통계, 근접점 질의

**iOS Renderer (Swift)**
- `RDChartView`가 `LineChartLayout` 받아 CoreGraphics로 렌더:
  - main 선 + 그라데이션, ghost 선(흐린 점선), 수평 기준선/밴드, 구간 구분선
- 0~1 × bounds → 픽셀 변환
- 등장 애니메이션(선 그리기) + 터치 마커(코어 `query` 사용)

**증명 목표 (Definition of Done)**
- xcframework 빌드 → SPM 소비 → 실 디바이스에서 **페이스+심박 겹친 그래프 1개 + 목표선**
  렌더 + 터치 동작 확인

## 9. 테스트 전략
- **Core**: KMP `commonTest`로 스케일/tick/**구간 스플릿 통계**/근접점 질의 단위 테스트
  (양 플랫폼 공통 1벌) — 공유 최대 이득
- **iOS Renderer**: 스냅샷 테스트(렌더 이미지 비교) + 0~1→픽셀 변환 단위 테스트
- Runday 통합 후: 기존 UI 테스트 플로우 영향 없는지 확인

## 10. 확정된 결정 (구 "미해결 결정")

| # | 결정 | 확정 | 이유 |
|---|---|---|---|
| 1 | 첫 적용 화면 | **분석 리포트 페이스 그래프** | 이번 논의 대상, 실사용 검증값 최고 |
| 2 | 곡선 스무딩 위치 | **직선 폴리라인부터, 스무딩은 나중에 코어 옵션** | YAGNI. 스무딩도 순수 계산이라 필요 시 코어로 |
| 3 | KMP iOS 산출물 | **xcframework를 SPM binary target으로 소비. SKIE는 파일럿 후 검토** | 최소 경로 우선, Swift API 다듬기는 나중 |
| 4 | 버전/배포 | **초기 Git 태그 + SPM. Maven 퍼블리시는 Android 통합 시점** | OSS 운영 부담 미루기 |

## 11. 다음 단계
1. 새 디렉토리 `lumipol-graph` 생성 + git init (현재 미초기화)
2. 이 문서를 `docs/DESIGN.md`로 이동
3. KMP 코어 스캐폴딩 → 라인차트 파일럿(위 DoD)부터 TDD
4. 이 프로젝트를 관리할 깃 주소: https://github.com/daehocho/lumipol-graph.git
