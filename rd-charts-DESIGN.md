# lumipol-graph — 크로스플랫폼 차트 라이브러리 설계

> 최초 작성일: 2026-06-24 · 갱신: 2026-07-01
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
  referenceLines: [RefLine],   // 0개 이상 (목표선; PB/목표/평균 무엇이든 앱이 값만 줌)
  referenceBands: [RefBand],   // 0개 이상 (목표 구간)
  segmentMarkers: [Marker],    // 0개 이상 (km 구분선 등)
  config: Config
)
Series(id, points: [(x, y)], axis: .primary | .secondary, role: .main | .ghost)
RefLine(value, axis, label?)   // label은 앱이 이미 포맷한 문자열(RunPaceUtils 등)
RefBand(lower, upper, axis)
Marker(x, label?, emphasis: Bool)
```

### Core 출력 `LineChartLayout` — 렌더러가 받는 전부, 픽셀 모름
```
LineChartLayout(
  series:    [ (id, normalizedPoints: [(0~1, 0~1)]) ],
  axisTicks: [ (axis, [(label, 0~1)]) ],        // 축별 tick 위치
  refLines:  [ (0~1, label?) ],
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

> **불변식(축-출력 일관성)**: 출력 요소(시리즈·기준선·밴드)가 걸린 모든 축은 반드시 `axisTicks`에 대응 항목을 가진다 — 렌더러가 기준선을 그릴 축을 항상 찾을 수 있게. (기준선/밴드 값이 `yValues`에 포함되어 도메인·tick이 생성되므로 구조적으로 보장됨.)

### A·C가 이 계약에 요구하는 능력
- **A →** 한 차트에 **2개 이상 시리즈 + 이중 Y축**(`.primary`/`.secondary` — 페이스·심박 스케일 다름)
- **C →** **ghost 시리즈**(`role: .ghost`) 겹치기 + **수평 기준선/밴드**(`referenceLines`/`referenceBands`)
- **공통 →** **구간(km) 스플릿 통계**(`stats.segments`) + 터치 근접점 질의(`query`)

인사이트 "탐지" 로직(예: 심박↑와 페이스↓가 겹치는 구간 찾기)은 처음엔 SDK에 안 넣음(YAGNI).
SDK는 스플릿·통계 같은 순수 값만 제공하고, 문장은 앱이 만든다. 필요해지면 나중에 코어의 순수 함수로 승격.

## 6. 리포 구조

```
lumipol-graph/                  ← 신규 독립 GitHub 리포 (KMP)
├── core/                   ← Kotlin Multiplatform 공유 코어 (UI 0)
│   ├── 데이터 모델 (LineChartData / Series / RefLine / RefBand / Marker)
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

## 8. 1차 파일럿 — 라인차트 수직 슬라이스 (A+C)

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
