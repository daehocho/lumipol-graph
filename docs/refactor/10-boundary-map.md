# 10 — 경계 지도 (1단계)

- 작성일: 2026-07-27
- **기능 목록 승인: 위임 승인** — 사용자가 2026-07-27 "너가 다 진행하고 코드 수정까지 끝내"로
  이후 게이트 전체를 위임(메모리 `lumipol-boundary-refactor` 기록). 아래 기능 목록이 승인된 목록이며
  이후 세션은 재승인 없이 이 표를 이어받는다.
- 근거 원자료(전체 인용 포함): `docs/refactor/raw/{ios,aos}-{renderer,app}-inventory.md` — 이 문서의
  각 행은 원자료의 해당 절을 압축한 것이다. 인용이 필요하면 원자료를 참조.
- `실측 차이` 칸: 0.7 하네스는 코어만 커버하므로 코어 항목 외에는 "정적 대조"로 표기.
  렌더러 덤프는 트랙 실행 시 회귀 검증이 필요한 항목부터 증분 추가.

## 판정 요약 (통계)

| 판정 | 건수(주요) | 대표 |
|---|---|---|
| 코어 단일 | 10 | 페이스 전처리, niceScale, 스플릿 집계, 존 집계, 도넛 각도, 스냅, 토글, 선택 규칙, 라벨 솎기, barHitTest |
| 중복 | 17 | AxisScale 역변환, ZoomState, 도넛 히트테스트, 색 앵커, PaceColormap, ChartStyle 45필드, √n 감쇠, 스냅 소스 정책 … |
| 비대칭 | 3 | BarChartEngine.layout 호출 주체(라인=렌더러 내부 vs 막대=앱), 도넛 선택 콜백 발화 조건, 접근성 라벨 |
| 그림자 | 2 | 렌더러 PaceColormap(프로덕션에서 앱 provider가 항상 대체), AOS ZoomState.pan/pinch 단발 API(사문화) |
| **앱 구현** | 8 | **2점 페이스 계산(양 앱 상이)**, **페이스 문자열(양 앱 상이)**, x값 산출, 센티널 해석, 최대심박(성별 불명 처리 상반), HR존 dt 재구성, 색 앵커 복사본(AOS), 색바 범례 재구성 |
| 정상(플랫폼 고유) | 다수 | 픽셀 변환, 텍스트 측정, 제스처 인식, density 환산, 햅틱, 프레임 구동 |

---

## A. 데이터 입력·검증·정규화·다운샘플링

| 기능 | 코어 | iOS렌더러 | AOS렌더러 | iOS앱 | AOS앱 | 티어 | 판정 | 실측 차이 / 비고 |
|---|---|---|---|---|---|---|---|---|
| 페이스 필터(120s/avg+600s)·p95 컷·평활·다운샘플·결측 승계·가용성 | `PaceSeriesEngine` | — | — | 위임 | 위임 | T1/T2 | **코어 단일** | 하네스 전 케이스 일치. 단 NaN 페이스가 필터 통과(07 문서 관측 후보) |
| 결측 센티널 해석 (hr≤0, alt≤−100, cad 0/250클램프) | — | — | — | `RDPaceChartDataBuilder.swift:53-63` | `ChartSamples.kt:31-45` | T1 | **앱 구현(양 앱 동일)** | 정적 대조: 상수·규칙 동일. 단 HR존 카드에선 iOS가 nil 대신 0을 넘겨 카드 간 표현 불일치(iOS 내부) |
| **2점 페이스 계산(paceSeconds)** | — | — | — | `RunPaceUtils` mphToPace/pace + `floorToDecimal(2)` 내림, 워치/GPS 분기 | `Util.getNowPaceV2`: Haversine 폴백 + 속도 게이트 1~41km/h + float 중간연산 + `16.666666666666998` 근사상수 | T1 | **앱 구현(양 앱 상이)** — 최우선 | 정적 대조: **계산식 자체가 다름**. 같은 기록이라도 무효 판정 규칙(iOS: 0/NaN/inf만 vs AOS: 속도 게이트)과 정밀도가 갈림 |
| x값 산출 (거리/시간) | — | — | — | 델타 재누적 `abs(distance×unit)/1000` (`:156-157`) | DB 누적 `realDistance` 직독 ×0.621371 (`UnifiedChartDataBuilder.kt:165-167`) | T2 | **앱 구현(양 앱 상이)** | 정적 대조: iOS는 음수 델타를 `abs`로 방어, AOS는 누적값 신뢰. 첫 포인트 소비 규칙도 다름(AOS는 location[0] 제외) |
| 원본 정렬 | — | — | — | `sortedTrackPointsA2`(exerciseTime 승순 + 0-포인트 정리) | 정렬 없음(DB 순서 신뢰) | T1 | **앱 구현(양 앱 상이)** | 정적 대조 |
| area x 오름차순 정렬 | 계약만(`AreaInterpolation.kt:7-8`) | render 시 정렬 | `RDLineChart.kt:105` | — | — | T1 | 중복 | 계약은 코어 문서, 구현은 렌더러 2벌 |
| 시리즈 id 유일성("첫 우선") | 미강제 | `ChartLayerBuilder.swift:17`, `TouchMarker.swift:56-61` | `LineChartDrawing.kt:223-232` | — | — | T1 | 중복 | 코어가 계약을 강제하거나 매핑을 출력해야 소거 |
| 2점 미만 시리즈 제외 | — | `ChartLayerBuilder.swift:70` | `LineChartDrawing.kt:242-249` | — | — | T1 | 중복 | 값 일치 |
| **막대 색 앵커(fastest/slowest/average + 온전 스플릿 폴백)** | 없음(`Stats`는 미연결) | `RDBarChartView.swift:104-109` | `RDBarChart.kt:243-248` | `RDSplitChartView.swift:113-130` | `SplitChartCard.kt:69-83` (**"private 규칙 복사본" 경고 주석**) | T1 | **중복+앱 구현 — 4벌** | 정적 대조: 현재 4벌 일치. AOS 앱 주석이 "이미 두 번 어긋났었다"고 실사고 기록 |
| HR존 dt 재구성 | — | — | — | `exerciseTime` 델타+타임인터벌 폴백 (`RDHeartRateZoneCardView.swift:94-115`) | `realExerciseTime` 델타 단일 (`ChartSamples.kt:53-61`) | T1 | **앱 구현(방식 상이)** | 정적 대조: 구 기록(폴백 필요 데이터)에서 갈릴 수 있음 |
| 스플릿 델타 산출 | — | — | — | 저장된 per-point 델타 직사용 | 누적→델타 재구성 + 첫 구간 특례 (`SplitChartDataBuilder.kt:12-31`) | T1 | **앱 구현(방식 상이)** | 정적 대조: 필터(>0)는 동일 |

## B. 스케일·틱·축 라벨

| 기능 | 코어 | iOS렌더러 | AOS렌더러 | iOS앱 | AOS앱 | 티어 | 판정 | 실측 차이 / 비고 |
|---|---|---|---|---|---|---|---|---|
| niceScale(Heckbert)·헤드룸·X도메인 규칙 | `NiceScale.kt`, `LineChartEngine.kt:18-26` | — | — | — | — | T1(개수)+T2(값) | **코어 단일** | **하네스 실측: tiny_range에서 JVM/Native 틱 4vs3 (07 문서)** — 실도메인 케이스는 일치 |
| 정규화↔도메인 역변환 | `AxisDomain.normalize`만(역함수·도메인 출력 없음) | `AxisScale.swift` 전체 | `AxisScale.kt:22-46` | — | — | T2 | **중복** (코어 출력 공백이 원인) | 정적 대조: 구현 완전 동일. 코어가 xDom/yDom을 출력하면 양쪽 소거 |
| 전체 X도메인 역산(줌 초기화, tick 외삽) | `AxisDomain(xNice.niceMin, xMax)` 계산하고 미출력 | `RDChartView.swift:322-323` | `LineChartInteraction.kt:220-227` | — | — | T2 | **중복** | 외삽이라 마지막 tick < xMax일 때 재구성 오차 내재 |
| 라벨 솎아내기 stride/표시 규칙 | `LabelThinning.kt` | 위임 | `RDBarChart.kt:205,275` 위임 | — | — | T1 | **코어 단일** | 하네스 일치 |
| 라벨 폭 측정 | — | `NSString.size` (tnum 미지정) | `measureLabelWidthPx` (tnum 고정, `LineChartDrawing.kt:699-714`) | — | — | T3→T1 주의 | 정상(플랫폼) | **tnum 유무로 측정폭이 갈려 stride 입력이 달라질 수 있음** — T3 위장 경계(05 문서) 해당 |
| 그리드 Y축 폴백(primary→secondary) | — | 있음 | `LineChartDrawing.kt:271-280` | — | — | T1 | 중복 | 값 일치 |
| 축 tick 조회 헬퍼 | — | 인라인 4곳 | `AxisScale.kt:12-13` | — | — | — | 중복(사소) | |
| 기본 축 포매터(%g) | — | `String(format:"%g")` 1줄 | `defaultLineChartFormatter` 15줄 재구현 (`RDLineChart.kt:45-61`) | — | — | T1 | **중복** | 정적 대조: 출력 일치 목표로 작성됨. NaN/Inf·트레일링0 처리를 AOS만 명시 — 검증은 렌더러 덤프에서 |
| 축 라벨 실제 문자열(페이스/정수/시간/거리) | — | — | — | `formatAxisLabel` (`RDUnifiedChartView.swift:187-207`) | `formatAxisValue` (`UnifiedChartCard.kt:317-335`) | T1 | **앱 구현** | 정적 대조: 구조 동일하나 페이스 문자열이 F-1로 인해 상이. AOS `%g` 로케일 노출면 |

## C. 좌표 변환·레이아웃

| 기능 | 코어 | iOS렌더러 | AOS렌더러 | iOS앱 | AOS앱 | 티어 | 판정 | 실측 차이 / 비고 |
|---|---|---|---|---|---|---|---|---|
| 정규화 좌표·틱 position·밴드·마커 | `LineChartEngine` | — | — | — | — | T2 | **코어 단일** | 하네스 일치 |
| Y반전(라인): 정규화→픽셀 | — | `PlotArea.swift:22` | `PlotArea.kt:39-42` | 반전 여부는 앱 주입(`invertedAxes`) | 동일 주입, 단 슬롯 0/1만 처리(`UnifiedChartCard.kt:151-155` — 슬롯2+ 누락 버그 후보) | T1(규칙) | 정상(플랫폼)+**앱 주입 규칙 불일치 위험** | AOS 앱의 슬롯2+ 미처리는 코어 우선순위 변경 시 발현 |
| **Y반전(막대): 코어 1−normalize + 렌더러 maxY−pos** | `BarChartEngine.kt:66-69` | `RDBarChartView.swift:76` 우회 | `RDBarChart.kt:209,265,297` 우회(`invertedAxes` 미사용) | — | — | T1 | **중복(구조)** — 반전이 코어/렌더러에 분산 | 라인과 다른 제2 좌표 경로. 세 번째 우회: `AreaSilhouette` 바닥 기준 |
| 막대 슬롯·폭·x위치 수학 | `barIndexAtX`(히트용) | `RDBarChartView.swift:97-98` | `RDBarChart.kt:251-256` | — | — | T2 | 중복 | 히트 슬롯과 시각 슬롯이 별도 구현 — 갈리면 탭과 그림 불일치 |
| barMinHeight clamp(코어 출력 덮어씀) | — | `RDBarChartView.swift:127` | `RDBarChart.kt:254` | — | — | T1 | 중복 | 값 일치(2) |
| 도넛 반경·중심·fraction→각도 | `DonutEngine`(fraction) | 라디안 | 도(°) (`RDHeartRateZoneChart.kt:203-223`) | — | — | T2 | 코어 단일+정상 | 단위만 다르고 식 동일 |
| 실루엣 픽셀 매핑(바닥 기준, 0~1 클램프) | `heightFractions`(코어) | `AreaSilhouette.swift:31-48` | `AreaSilhouette.kt:42-48` | — | — | T2 | 코어 단일+중복(픽셀부) | 식 동일 |
| plotInsets→플롯 사각형, px 변환 | — | `PlotArea.swift` | `PlotArea.kt` | — | — | T1(논리값) | 정상(플랫폼) | 식·클램프 완전 동일 |
| **LineChartLayout 도메인(xDom/yDom) 미출력** | 공백 | 역산 | 역산 | — | — | — | **구조 공백** | B-2·B-3·E-1의 공통 원인 |

## D. 스타일 상수

| 기능 | 코어 | iOS렌더러 | AOS렌더러 | iOS앱 | AOS앱 | 티어 | 판정 | 실측 차이 / 비고 |
|---|---|---|---|---|---|---|---|---|
| ChartStyle 기본값 ~45필드 | — | `ChartStyle.swift` | `ChartStyle.kt` (수동 동기화 선언) | 3필드만 덮어씀 | 2필드만 덮어씀 | T1 | **중복** | 정적 대조(21 문서): **현재 전부 일치**. 구조상 한쪽만 갱신 가능 |
| 색 팔레트(라이트/다크) | 역할 enum만 | 동적 UIColor | 실측 RGB 고정 2세트 | 지표 고정색 주입 | 동일 값 주입 | T1 | 중복(구조 상이·값 일치) | iOS OS 업데이트로 시스템색 변하면 조용히 갈림 |
| 스타일 밖 하드코딩 상수(라벨 여백·마커 폭·barWidthRatio 0.6 등) | — | 인라인 ~10개 | `private const` ~15개 | — | — | T1 | 중복 | 값 일치. iOS `barWidthRatio`는 주입 불가(하드코딩), AOS는 스타일 필드 — API 비대칭 |
| iOS `barColors` 기본값 | — | 죽은 기본값(어디서도 안 읽음) | 사용 | — | — | — | **그림자(iOS 내)** | |
| **3구간 페이스 컬러맵(0.70/0.25/0.4)** | — | `PaceColormap.swift` | `PaceColormap.kt` | `RDRouteColorizer.swift:127-128` | `PaceColorUtil.kt:7-59` | T1 | **중복+앱 구현 — 4벌** | 프로덕션은 앱 provider가 렌더러 기본을 항상 대체 → **렌더러 2벌은 그림자성**. 색약 모드는 앱에만 존재 |
| √n 그라데이션 감쇠 | 규칙이 주석 산문 | `ChartLayerBuilder.swift:41` | `LineChartDrawing.kt:180` | — | — | T1(식) | 중복 | 식 일치 |
| 도넛 자동해제 3s·디밍 0.3/0.45 | — | 3.0 | 3f | 기본값 사용 | 기본값+범례 0.45 자체 | T1 | 중복 | 값 일치 |

## E. 상호작용

| 기능 | 코어 | iOS렌더러 | AOS렌더러 | iOS앱 | AOS앱 | 티어 | 판정 | 실측 차이 / 비고 |
|---|---|---|---|---|---|---|---|---|
| 근접 스냅(창 인식) | `Nearest.kt` | 위임+epsilon 창 확장 | 위임+동일 (`TouchMarker.kt:64-69`) | — | — | T1 | **코어 단일**(+중복 부속) | 하네스 일치. epsilon 1e-9 양쪽 동일 |
| 스냅 소스 선택(main 우선) | — | `TouchMarker.swift` | `TouchMarker.kt:73-75` | — | — | T1 | 중복 | 플랫폼 중립 정책이 렌더러에 |
| 오버레이 도트 재-최근접 + Y역산 | `nearest`가 정규화 y 미제공 | `TouchMarker.swift:102,132-136` | `TouchMarker.kt:99-104,153-155` | — | — | T2 | **중복** (코어 출력 공백) | `NearestResult`에 정규화 좌표 추가로 소거 |
| 막대 히트테스트 | `BarHitTest.kt` | 위임 | 위임 (`RDBarChart.kt:99`) | — | — | T1 | **코어 단일** | 하네스 일치 |
| **도넛 히트테스트(반경+각도 역산)** | 없음 | `RDHeartRateZoneView.swift:237-256` | `RDHeartRateZoneChart.kt:261-278` | — | — | T1 | **중복** (코어 공백) | AOS만 48dp 히트밴드 확장(의도적 완화 — 동작 차이) |
| 도넛 탭 토글 전이 | `DonutEngine.toggleSelection` | 위임 | 위임 | `selectSegment(at:)` | `selection.toggle(i)` | T1 | **코어 단일** | 하네스 일치 |
| 도넛 선택 콜백·햅틱 발화 조건 | — | 뷰가 통지·햅틱·타이머 통합 | 상태 홀더 분리 — 앱 구동 변경은 `onSelectSegment` 미발화 | 델리게이트 수신 | 앱이 직접 햅틱 | T1 | **비대칭** | `DonutSelectionState.kt:29-31`·`RDHeartRateZoneChart.kt:64-66` 문서화된 차이 |
| ZoomState(핀치·팬·클램프) | 없음 | `ZoomState.swift` | `ZoomState.kt` | — | — | T2 | **중복** | 구현 완전 동일. iOS `pan(byFraction:)` 등 일부 사문화(그림자) |
| 제스처 인식(슬롭·임계) | — | 롱프레스 0.5s 하드코딩 | 시스템 `viewConfiguration` 값 | — | — | T3(인식)+T1(SDK 지정 임계) | 정상(플랫폼) — 단 임계 소스 불일치 | iOS 고정 0.5s vs AOS 접근성 설정 연동 |
| 팬/핀치→창 산술, maxZoomScale 10 | 없음 | `RDChartView.swift:423-427` (ZoomState.pan 미사용) | `LineChartInteraction.kt:201-204` | — | — | T2 | 중복(+iOS 내 그림자) | |
| 시리즈 선택 규칙(toggled/normalized/slots) | `SeriesSelection` | — | — | 위임 | 위임+미가용 선택 보존 래퍼(`UnifiedChartDataBuilder.kt:89-101`) | T1 | **코어 단일** | AOS 앱의 의도 보존 로직은 iOS에 대응 없음(선택 영속 자체가 AOS만) — 확인 필요 |
| `BarChartEngine.layout` 호출 주체 | — | 렌더러 미호출 | 렌더러 미호출 | **앱이 직접 호출** | **앱이 직접 호출** | — | **비대칭(라인 대비)** | 라인=렌더러 내부, 막대=앱. 색 앵커가 layout 밖이라 생긴 구조 |

## F. 포맷팅

| 기능 | 코어 | iOS렌더러 | AOS렌더러 | iOS앱 | AOS앱 | 티어 | 판정 | 실측 차이 / 비고 |
|---|---|---|---|---|---|---|---|---|
| **페이스 문자열** | 없음 | 없음 | 없음 | `4'30"` — `%d'%02d\"` 절삭, 무효 `-'--"` (`RunPaceUtils.swift:648-659`) | `5'30''` — 작은따옴표 2개, 절삭, 무효 `-'--''`, 상한 99분 (`FormatUtil.kt:150-188`) | T1 | **앱 구현(양 앱 상이)** — 최우선 | 정적 대조: **접미사가 눈에 보이게 다름**(`"` vs `''`). AOS `normalizedStat`의 `--` 폴백이 리터럴 불일치로 사문화(`UnifiedChartCard.kt:180`) |
| 존 시간 HH:MM:SS | 없음 | `stringHHMMSS` | `formatZoneTime` (`HeartRateZoneCard.kt:187-195`) | 앱 | 앱 | T1 | 앱 구현(동일 규칙) | 정적 대조 일치 |
| x축 시간 "N:00"/거리 %g | 없음 | — | — | `RDUnifiedChartView.swift:210` | `UnifiedChartCard.kt:358-379` | T1 | 앱 구현(동일 규칙) | AOS `%g` 로케일 의존(`,` 소수점 로케일에서 trim 실패 가능), iOS는 비의존 |
| y 정수 라벨(절삭) | 없음 | — | — | `Int(value)` | `value.toInt()` | T1 | 앱 구현(동일) | |
| 고도 스크럽 "{round}m" | 없음 | — | — | `RDUnifiedChartView.swift:411` | `UnifiedChartCard.kt:170` | T1 | 앱 구현(동일) | 유일한 반올림+단위 부착 지점 |
| 도넛 센터 % | 없음 | `RDHeartRateZoneView.swift:205` | `RDHeartRateZoneChart.kt:239` | — | — | T1 | 중복(동일) | |
| 접근성 요약 문자열 | 없음 | 도넛만 "심박존 도넛" | 라인·막대·도넛 3종 한국어 하드코딩 + `colorRole.name` 낭독 | — | — | T1 | **비대칭** | 로컬라이즈 불가 문자열이 렌더러에. 내용도 상이 |
| bpm 범위·존 이름·카드 제목 | `ZoneBpmRange` 값만 | — | — | StringTable id 6627~6633 | 동일 id 세트 | T1 | 앱 구현(동일 id) | 원격 스트링 테이블 공유 |
| 스크럽 콜백 계약 | — | `Map<String,String>` (포맷된 문자열) | 동일 (`ChartCallbacks.kt:13`) | 포매터 주입 | 포매터 주입 | — | 구조 공통 | 실값이 앱에 안 감 — 코어 포맷팅 회수 시 계약 재설계 지점 |

## G. 애니메이션·전환

| 기능 | 코어 | iOS렌더러 | AOS렌더러 | 티어 | 판정 | 비고 |
|---|---|---|---|---|---|---|
| 라인 등장 600ms | — | 0.6s `.easeOut` | 600ms `EmphasizedDecelerate(0.05,0.7,0.1,1.0)` | T1(파라미터) | **중복 — 이징 불일치** | 지속시간 일치, 곡선 상이. 기본값도 상이(iOS 플래그 vs AOS 기본 on) |
| 막대 300ms/도넛 550ms 등장 | — | 없음(정적) | `EntranceAnimation.kt` (기본 off) | T1 | AOS 전용(패리티 유지) | 켜면 갈림 — 정책 필요 |
| 도넛 자동해제 타이머 | 상수만 스타일 | 뷰 내장 | 컴포저블 내장 | T1(3s) | 정상 | 값 일치 |
| 프레임 구동·트림 | — | CABasicAnimation | PathMeasure 트림 | T3 | 정상(플랫폼) | |

---

## 판정 소결 (3단계 정책 입력)

우선순위 기준(프롬프트: 앱 구현 > 비대칭 > 중복 > 그림자 > 브릿지)으로 정렬한 상위 항목:

1. **[앱·상이] 2점 페이스 계산** — 같은 기록에서 다른 페이스가 나오는 유일한 최상류 지점. SDK를 아무리 고쳐도 이게 남으면 화면은 계속 다르다.
2. **[앱·상이] 페이스 문자열 포맷** — `4'30"` vs `5'30''`, 무효 표현 상이 + AOS 폴백 사문화 버그.
3. **[앱·상이] 최대심박 성별 불명 처리** — iOS 여성 공식 vs AOS 남성 공식 (정반대).
4. **[앱·4벌] 막대 색 앵커 + 컬러맵** — SDK 안 2벌 + 앱 2벌. AOS 주석이 실사고 2회 기록.
5. **[앱·상이] x값 산출·정렬·HR존 dt** — 데이터 원천 가공 경로 불일치.
6. **[비대칭] BarChartEngine.layout 호출 주체 / 도넛 콜백 발화 / 접근성 라벨.**
7. **[중복·코어 공백 기인] AxisScale·ZoomState·도넛 히트테스트·스냅 소스 정책·오버레이 재탐색** — 코어가 도메인/역변환/정규화 y를 출력하지 않아 생긴 구조적 중복.
8. **[중복] ChartStyle 45필드 + 스타일 밖 상수 + 팔레트** — 현재 일치하나 수동 동기화.
9. **[그림자] 렌더러 PaceColormap 2벌(앱 provider가 항상 대체), iOS ZoomState 일부 사문 API.**
10. **[코어 결함] niceScale JVM/Native 발산(실측), NaN 페이스 필터 통과.**
