# 41 — 트랙 B: 구조 정리 (4단계)

- 작성일: 2026-07-27 / 승인: 위임
- 원칙: 중복/비대칭을 코어로 흡수. 이관 후 렌더러엔 "코어 값 → 플랫폼 API" 어댑터만 남는다.
- 난이도 S/M/L, 회귀 위험은 골든+렌더러 단위테스트+스냅샷(플랫폼 내) 기준.

| # | 항목 | 난이도 | 회귀 위험 | 이관 후 렌더러 어댑터 형태 (의사코드) |
|---|---|---|---|---|
| B1 | **축 도메인 출력** — `LineChartLayout`에 `domains: ChartDomains(x: AxisDomain, yPrimary: AxisDomain?, ySecondary: AxisDomain?)` 추가. `AxisDomain`에 `denormalize(t)` 추가 | S | 낮음 — 추가 필드(기존 소비자 무영향) | `xScale.value(t)` → `layout.domains.x.denormalize(t)`; AxisScale.swift/kt 삭제, `ticksFor`만 유틸 잔류 |
| B2 | **스크럽 결과 보강** — `nearestScrub(data, x, window?) : ScrubResult(snappedX, perSeries: [id, x, y, nx, ny(축 정규화)], snapSourceId)` — 스냅 소스 규칙(main 우선)·창 epsilon·오버레이 정규화 y 포함 | M | 중간 — TouchMarker 재배선 | `for p in result.perSeries: dot = plot.point(p.nx, p.ny, axis)`; TouchMarker의 재탐색·Y역산 삭제. 05 승인대로 스냅 x/y **T1 승격** (원본 복사 확인됨 — `Nearest.kt:9` `minByOrNull` 결과 그대로) |
| B3 | **ZoomState 코어 이관** — `com.lumipol.graph.interaction.ZoomWindow` (pinch/pan/place/clamp, maxScale) | S | 낮음 — 순수 산술, 기존 테스트 이식 | 렌더러 ZoomState = typealias/thin wrapper → 삭제. iOS 사문 API(`pan(byFraction:)` 등) 이관 시 정리 |
| B4 | **도넛 히트테스트 코어** — `DonutEngine.hitTest(dxRatio, dyRatio, ringRatio, hitBandRatio, layout): Int?` (비율 공간 — px 무관) | S | 낮음 | 렌더러: `hitTest((px-cx)/r, (py-cy)/r, …)`. AOS 48dp 확장은 hitBandRatio 인자로 유지(의도적 완화 — 44에 명시) |
| B5 | **막대 색 앵커 공개** — `BarChartLayout.colorAnchors: BarColorAnchors(fastest, slowest, average)?` — 온전 스플릿 우선·2개 미만 폴백 규칙 포함. ref(런 총합 평균) 우선 규칙도 코어로 | S | 낮음 — 순수 축약 | 렌더러·앱의 앵커 블록 4벌 → `layout.colorAnchors` 소비. **비대칭 6번 부수 해소**: 앱이 layout을 직접 만들 이유가 없어지므로 렌더러 내부 호출로 통일 가능(43 문서 경로) |
| B6 | **페이스 컬러맵 코어** — `PaceColormap.rgba(value, anchors, colorBlind: Boolean): Long(0xAARRGGBB)` — 3구간 보간 + 색약 이산 4색(앱 소유였던 것 포함) | M | 중간 — 색 비트 일치 검증 필요(SceneDigest) | 렌더러: `Color(rgba)` 변환 1줄. 렌더러 PaceColormap 2벌 삭제(그림자 소거), 앱 колorizer는 C4에서 전환 |
| B7 | **수치 상수 코어 단일 원본** — `ChartDefaults` object: 스타일 밖 정책 상수(라벨 여백·마커 폭·barWidthRatio·도넛 시작각·자동해제 3s·maxZoomScale·epsilon…) + 팔레트 RGBA 쌍(라이트/다크) | M | 낮음(값 불변 이동) | ChartStyle 기본값이 `ChartDefaults.*` 참조. iOS는 동적 UIColor 유지하되 **SceneDigest가 해석값을 대조** |
| B8 | **경계 계약 정규화** — `layout(data, xMin, xMax)`: xMax<=xMin이면 전체 layout 폴백(require 제거). `slotAxis` 음수 → require 유지(정적 오용). maxTicks `coerceAtLeast(2)` | S | 낮음 | 렌더러의 `if (hi > lo)` 가드 삭제 가능(잔류해도 무해) |
| B9 | **기본 인자 브릿지 완화** — `BarChartData`·`ChartConfig`·`Series`에 ObjC용 보조 생성자(DonutSegment 패턴, `DonutInput.kt:13-14` 선례) | S | 낮음 | iOS 호출부가 코어 기본값을 자동 상속 — 하드코딩 재발 차단 |
| B10 | **시리즈 id 첫 우선 규칙 코어** — layout이 `roleById/axisById` 맵을 실어 보내거나 중복 id를 레이아웃 단계에서 정리 | S | 낮음 | 렌더러 firstWinsBy 2벌 삭제 |
| B11 | 도넛 선택 콜백 발화 통일 — AOS 홀더 `toggle()`도 `onSelectSegment` 발화 | S | 낮음 | AOS 렌더러 내부 |
| B12 | 접근성 문자열 주입화 — 렌더러 하드코딩 한국어 제거, `ChartA11y(labels…)` 주입 + 코어 기본 문자열 | M | 낮음 | 렌더러는 배치만. 내용은 앱 로컬라이즈 |
| B13 | 그림자 정리 — iOS `barColors` 죽은 기본값·ZoomState 사문 API 제거 | S | 낮음(공개 API면 43 전략 적용) | — |
| B14 | 막대 Y 좌표 경로 통일 — RDBarChart가 PlotArea.y 경유(반전은 코어 값 그대로) | S | 낮음(동일 결과 리팩토링) | 렌더러 내부 |

## 이관하지 말아야 할 것 (명시)

| 항목 | 이유 |
|---|---|
| PlotArea 픽셀 변환·클립·Path 조립·캐시 | 픽셀 좌표계는 플랫폼 소관 — 목표 아키텍처의 렌더러 정의 그 자체 |
| 텍스트 측정(tnum 포함) | 폰트 스택이 플랫폼 자산. 측정값은 코어 결정(labelStride)의 **입력**으로만 |
| 제스처 인식·상태머신·consume 정책 | UIKit/Compose 이벤트 모델이 본질적으로 다름. **의미 해석**(창 산술 B3, 스냅 B2)만 코어 |
| density/dp·헤어라인·fontScale 상한 | 플랫폼 디스플레이 정책 |
| 햅틱·애니메이션 구동 | 플랫폼 API. 파라미터 값만 코어 상수(B7) |
| Compose 요청 봉투(Zoom/MarkerController) | 선언형 UI 어댑터 — AOS 고유 계층으로 타당 |

## 실행 기록

- [x] **B1** — 코어 0.30.0(`e03a26f`) + 양 렌더러 채택(`026497a`). AxisScale.swift/kt·테스트 삭제(−263줄),
  줌 초기화의 tick 외삽도 정확한 도메인으로 대체. 양 플랫폼 전체 테스트·골든 게이트 통과
- [x] **B2 코어** — 0.31.0. `nearestScrub(data, layout, x): ScrubResult` — 창 필터(±1e-9 코어 상수
  `SCRUB_WINDOW_EPSILON`)·스냅 소스(main 우선)·정규화 좌표(nx/ny)·오버레이 자체 정규화 y·포맷 축까지
  코어 확정. 기존 `nearest` 2종 `@Deprecated(ReplaceWith)`. 스냅 x/y T1 승격을 05 문서에 확정 기록.
  API 편차: 명세의 `window?` 인자 대신 `layout`을 받는다 — 창은 항상 `layout.domains.x`이고(줌은
  창 layout을 다시 만듦) 오버레이 ny에 layout이 어차피 필요하다. 골든에 nearestScrub 섹션 추가.
  렌더러 전환 완료: 양 TouchMarker가 nearestScrub 소비 — 재탐색·firstWinsBy 캐시 주입·창 epsilon
  로직 삭제(AOS TouchMarkerContext 축소, iOS RDChartView 1e-9 리터럴 → 코어 상수). 양 플랫폼
  전체 테스트 통과(AOS testDebugUnitTest, iOS 174건)
- [x] **B3** — 0.32.0. `com.lumipol.graph.interaction.ZoomWindow` — pinch(기준 창+누적 배율)·
  pan(기준 창+누적 비율, 렌더러 산술 흡수)·setWindow·reset·ulp 재구성 방지 place. 렌더러
  ZoomState 2벌+테스트 삭제(테스트는 commonTest ZoomWindowTest로 이식, 기대값 불변).
  단발 pinch/pan 미러 API(양쪽 사문)는 이관에서 소거(B13 선행 해소). 골든 zoomWindow 섹션 추가.
  기준 창 스냅샷(상태 보유)은 렌더러 잔류 — 산술만 코어
- [x] **B4** — 0.33.0. `DonutEngine.hitTest(dxRatio, dyRatio, hitBandRatio, layout): Int?` —
  비율 공간(반경 r로 정규화, r=1이라 명세의 ringRatio 인자는 불필요해 소거). 반경 대역·각도→
  fraction·sourceIndex 규칙 코어 확정. `MIN_HIT_TARGET_DP=48` 코어 상수화, D7 결정 반영으로
  **iOS도 max(링, 48pt) 대역 채택**(탭 영역 확대, 시각 무변화). atan2는 이산 인덱스 출력이라
  골든 무해(§5 예외 근거 KDoc 기록). 골든 donutHitTest 섹션 추가. 양 렌더러는 픽셀→비율 환산만
- [x] **B5** — 코어 0.30.0 + 양 렌더러 채택(`d6f14ca`). 규칙 검증은 코어 테스트로 이동.
  앱 복사본 2벌 삭제는 C4(앱 커밋)에서
- [x] **B6** — 0.34.0. 코어 `PaceColormap.rgba(value, anchors, colorBlind): Long(0xAARRGGBB)` +
  `legendStops(anchors, count=40, colorBlind)`(D8 반영 — C5 색바 API 겸용). 렌더러 공식 2벌
  (defaultPaceColor)·전용 테스트 삭제, `ChartStyle.colorBlindMode` 신설(barColorProvider는 단계적
  폐기 예고 — 정식 @Deprecated는 C4 앱 전환 시). 색약 이산 4색 규칙은 앱 2벌이 상이해 D12 신설,
  iOS안 채택. 골든 paceColormap 섹션(색은 hex 문자열 = T1). 색 8비트 반올림 양자화 —
  렌더러 float 대비 ≤0.5/255, 스냅샷 테스트 무변화 확인
- [x] **B7** — 0.35.0. 코어 `ChartDefaults`: 정책 수치(라인·막대·도넛·여백·터치·maxZoomScale·
  등장 애니 파라미터(D5 easeOut 계수·기본 off·지속시간)·알파 9종) + Light/DarkPalette RGBA
  (AOS 실측 24쌍 이관). AOS: ChartStyle 기본값·팔레트 + 인라인 상수 8파일 코어 참조 전환.
  iOS: ChartStyle 수치 기본값 코어 참조 + `barWidthRatio` 필드 신설(주입 비대칭 해소) +
  ChartLayerBuilder/RDBarChartView/RDChartView 리터럴 소거. 알파는 base RGB와 분리
  (8비트 사전 합성 시 float 알파와 ±1/255 어긋남 방지). 골든 chartDefaults 섹션 —
  상수 변경이 골든 갱신(의도 선언)을 강제. 플랫폼 보정(HAIRLINE_MIN_PX·MAX_FONT_SCALE·
  롱프레스 0.5s(D10 T3))만 렌더러 잔류
- [x] **B8** — 0.30.0. 퇴화 창 폴백 + 기존 예외 테스트를 새 계약으로 갱신
- [x] **B9** — 0.30.0. Series/ChartConfig/BarChartData 축약 생성자
- [x] **B10** — 0.36.0. `SeriesLayout.axis` 신설 — 코어가 항목별 축을 확정해 출력(맵 방식 대신
  항목 내장 — 중복 id 모호성 자체가 소멸). 렌더러 firstWinsBy/firstWinsAxis(AOS)·
  Dictionary(uniquingKeysWith:)(iOS 드로잉) 삭제. 골든 라인 섹션에 axis 키 추가(의도 델타 26건)
- [x] **B11** — AOS `DonutSelectionState.toggle()`도 탭과 동일 경로로 햅틱+`onSelectSegment` 발화
  (iOS `selectSegment(at:)` 패리티, 30 문서 §3-7 비대칭 해소). 재진입은 전이 규칙상 1스텝 수렴.
  구 무통지 계약을 검증하던 테스트를 새 계약으로 갱신. 렌더러 내부 — 코어 무변경
- [x] **B12** — 0.37.0. 코어 `ChartA11y`(line/bar/donut/donutSelection, ko-KR 기본 문자열) +
  각 차트 주입점(`chartContentDescription`/AOS, `accessibilityDescriptionOverride`/iOS).
  D9 반영: iOS도 3차트 낭독(라인·막대 뷰 a11y 라벨 신설), 도넛은 전체 분포. AOS 도넛 낭독이
  colorRole 이름 대신 라벨 우선으로 개선. 렌더러 하드코딩 한국어 소거(내용은 앱 주입 가능)
- [ ] B13~B14 — 미착수

## 실행 순서 권장

B1 → B2 (레이아웃 출력 확장 한 릴리스) → B5+B6 (막대 색 한 릴리스, C4 선행조건) →
B3+B4 (상호작용 산술) → B8+B9 (브릿지) → B7+B10 (상수·계약) → B11~B14 (정리).
각 단계: 코어 구현+commonTest → 하네스/골든 재녹화 → 렌더러 어댑터 전환 → 렌더러 테스트 →
xcframework 재생성(같은 커밋) → 릴리스 태그.
