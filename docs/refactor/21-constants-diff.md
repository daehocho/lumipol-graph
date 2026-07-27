# 21 — 상수/기본값 5열 대조표 (2단계)

- 작성일: 2026-07-27
- 열 정의: **코어** = core commonMain / **iOS-R**·**AOS-R** = 렌더러 기본값 / **iOS-App**·**AOS-App** = 앱이 명시 주입·덮어쓰기한 값. `(기본)` = 앱이 SDK 기본값을 그대로 사용. `—` = 해당 계층에 존재하지 않음.
- 근거: `raw/aos-renderer-inventory.md` §1(33행 전수 대조), `raw/ios-renderer-inventory.md`, `raw/{ios,aos}-app-inventory.md` §5.
- **판정 요약: 렌더러 쌍 33필드+인라인 상수 전부 값 일치(수동 동기화 성공 상태). 불일치는 앱 계층과 구조(주입 가능성·죽은 값)에 있다.**

## 1. 코어 소유 상수 (유일 원본 — 복사본 없음 확인)

| 상수 | 코어 값 | iOS-R | AOS-R | iOS-App | AOS-App |
|---|---|---|---|---|---|
| `Y_AXIS_HEADROOM_FRACTION` | 0.05 (`NiceScale.kt:17`) | 미참조 | 미참조 | — | — |
| `PaceSeriesId` 0/1/2/3 | PACE/HEART/CADENCE/ALTITUDE | — | — | 심볼 참조(복사 없음) | 심볼 참조 — enum이 `selectionId`로 채택, 저장은 enum명 CSV(번호 비저장, 의도적) |
| 페이스 필터 120s/+600s/p95×1.25/20표본/3000/윈도15/최소11 | `PaceSeriesEngine.kt:16-30` (private) | — | — | 주석 문서화만 | 테스트 문서화만 |
| 존 경계 0.50~0.90 | `HeartRateZoneEngine.kt:14` | — | — | 주석 문서화만 | — |
| 시간 버킷 후보 1/2/5/10분, MAX_BARS 10 | `BarChartEngine.kt:23-24` | — | — | `chooseTimeBucketSeconds` 위임 | 위임 |

## 2. ChartStyle — 렌더러 쌍 대조 (전 필드)

값이 일치하는 필드는 묶어서 기재. 개별 인용은 `raw/aos-renderer-inventory.md` §1 표 참조.

| 필드군 | iOS-R = AOS-R 값 | 일치 | iOS-App | AOS-App |
|---|---|---|---|---|
| lineWidth 2 / gridLineWidth 0.5 / overlayLineWidth 1.5 / dash [3,3]·[6,3] / gradientMaxAlpha 0.25 / areaHeightFraction 0.35 / areaMinValueSpan 0.5 | 좌동 | ✅ | gradientMaxAlpha 0.25 명시(동값 고정), areaFillColor만 교체 | (기본) + areaFillColor 교체 |
| barCornerRadius 3 / barMinHeight 2 / barDimOpacity 0.35 / barShow{X,Y}AxisLabels true / barCalloutFont 12·semibold | 좌동 | ✅ | (기본) | (기본) |
| `barWidthRatio` | AOS-R 필드 0.6f vs **iOS-R 필드 없음** — `slot*0.6` 하드코딩(`RDBarChartView.swift:98`) | ⚠️ 값 동일·**주입 가능성 비대칭** | — | — |
| `partialBarAlpha`(AOS) = `barPartialOpacity`(iOS) 0.6 | 이름 상이·값 동일 | ✅ | **1.0 덮어씀** | **1f 덮어씀** (양 앱 동일 의도) |
| donutRingWidth 28 / donutDimmedAlpha 0.3 / centerLabel 13 / centerPercent 28·bold / autoDeselect 3s / haptics true | 좌동 | ✅ | ringWidth × 크기비율 | 동일 공식 |
| axisLabelFontSize 10 / plotInsets (16,44,20,44) / touchDotRadius 4 | 좌동 | ✅ | (기본) | (기본 — 인셋은 SDK에 미전달, Modifier로 자체) |
| `HAIRLINE_MIN_PX` 1 / `MAX_FONT_SCALE` 1.3 / dp→px 스케일러 | — / AOS-R 전용 | AOS 전용(플랫폼 타당) | — | — |
| `fallbackDataColor` 8E8E93 | iOS는 호출부 `?? .systemGray` | ✅ 값 동일 | — | — |

## 3. 색 팔레트 — 구조 상이·값 일치

iOS-R: 동적 `UIColor`(시스템이 라이트/다크 해석) / AOS-R: 실측 RGB 2세트 고정(`ChartStyle.kt:141-203`).
전 24쌍 값 일치 확인(원자료 §1 표). **위험**: iOS 시스템색이 OS 업데이트로 바뀌면 AOS 고정값과 조용히 갈림 — 5단계 가드레일 후보.

앱 주입 색(양 앱 값 일치, `LumipolChartStyle.kt:8-18`에 iOS 대응 주석):

| 용도 | iOS-App | AOS-App | 일치 |
|---|---|---|---|
| pace / heart / cadence / altitude | 7B7BFF / FF789B / GraphGreen / GraphOrange(α0.22) | 0xFF7B7BFF / 0xFFFF789B / 0xFF04DE71 / 0xFFFF8100(α0.22) | ✅ |
| 도넛 Z1~Z5 | WarmGreyTwo/GraphBlue/GraphGreen/GraphOrange/GraphRed | 0xFF757575/0xFF5AC8FA/0xFF04DE71/0xFFFF8100/0xFFFA114F | ✅ |
| 칩 선택/미선택 | darkPeriwinkle(116,96,217)/wisteria(154,147,191) | 0xFF7460D9/0xFF9A93BF | ✅ |

## 4. 스타일 밖 하드코딩 상수 (렌더러 쌍)

| 상수 | iOS-R | AOS-R | 일치 |
|---|---|---|---|
| LABEL_GAP 2 / AXIS_LABEL_GAP 4 / 마커 폭 1·1.5 / 터치선 1 / WINDOW_EPSILON 1e-9 / maxZoomScale 10 / BAR_LABEL_GAP 4 / BAR_LABEL_MIN_GAP 6 / 말풍선 pad 8·4·코너 6 / 도넛 −90°·센터 0.9·Butt cap | 인라인 | `private const` | ✅ 전부 |
| 컬러맵 앵커 0.70/0.25/청록감쇠 0.4/축퇴 (0,1,0) | `PaceColormap.swift` | `PaceColormap.kt` | ✅ — 단 **앱 2벌**(`RDRouteColorizer.swift:127-128`, `PaceColorUtil.kt:7-59`)까지 4벌. 색약 이산 4색은 앱에만 |
| 색 앵커 규칙(온전 스플릿·2개 미만 폴백) | `RDBarChartView.swift:104-109` | `RDBarChart.kt:243-248` | ✅ — **앱 2벌 추가**(`RDSplitChartView.swift:113-130`, `SplitChartCard.kt:69-83`) 총 4벌 |
| 롱프레스 임계 | **0.5s 하드코딩** | **시스템 viewConfiguration** | ⚠️ 소스 상이(값 근사) |
| 히트 대역 | 링 폭 그대로 | `max(링, 48dp)` 확장 | ⚠️ **동작 상이(의도적)** |
| 등장 이징 | `.easeOut` | `EmphasizedDecelerate(0.05,0.7,0.1,1.0)` | ❌ **불일치** |
| 등장 기본값(라인) | 플래그 제어 | 기본 `true` | ⚠️ 상이 |
| iOS `barColors` 기본값 | 존재하나 **미사용(죽음)** | 사용 | ⚠️ 그림자 |

## 5. 앱 계층 상수 불일치 (SDK 밖 — 트랙 C 근거)

| 상수 | iOS-App | AOS-App | 판정 |
|---|---|---|---|
| **마일 환산(각 앱 내부 3벌)** | 1.609344000000865 / 0.621371 / 1609.344 | 동일 3값 (단 `MILE_PER_KM` **이름이 값과 반대**) | 세트는 일치하나 역수 불일치(0.621371×1.609344≠1) — **코어 단일 상수화 대상** |
| `KM_PER_MIN_VALUE` | 사용 안 함(1000/60 직산) | `16.666666666666998` 근사 리터럴 | ❌ ~1e-13 상대오차 |
| 페이스 무효 문자열 | `-'--"` | `-'--''` | ❌ **표기 자체 상이** |
| segmentCount | 항상 5 | `floor(총거리단위)`·상한 120·시간모드 0 | ❌ **스플릿 통계 개수가 다름** |
| 최대심박 성별 불명 | 여성 공식 | 남성 공식 | ❌ **정반대** |
| 무효 페이스 속도 게이트 | 없음 | 1~41km/h | ❌ |
| 다크 테마 | 시스템 추종 | false 고정 | ❌ (제품 결정 필요 — 44 문서) |

## 6. 소결

- 렌더러 쌍의 수동 동기화는 **현재 시점 값 기준 전부 성공** 상태다. 따라서 트랙 A에서 "값 맞추기"가
  필요한 SDK 내부 상수는 이징·등장 기본값 정도이고, 진짜 문제는 **구조**(주입 비대칭, 죽은 기본값,
  4벌 복제)와 **앱 계층**(§5)이다.
- §5의 각 행이 42-track-c 항목과 1:1로 대응한다.
