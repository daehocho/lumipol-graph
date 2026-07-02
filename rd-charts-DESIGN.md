# lumipol-graph — 크로스플랫폼 차트 라이브러리 설계

> 작성일: 2026-06-24 · 출처: Runday iOS 그래프 개선 논의에서 분리된 독립 프로젝트
> 이 문서는 새 리포 `lumipol-graph`의 `docs/DESIGN.md`로 옮겨 사용한다.

## 배경 / 문제

Runday iOS(Users/daeho/Runday_IOS)는 현재 DGCharts(danielgindi/Charts)를 로컬 벤더링(140개 .swift 파일)해
라인/바/파이 차트를 그린다. 주요 불만 두 가지:

1. **디자인/커스터마이징 한계** — 범용 라이브러리라 Runday 디자인에 딱 맞추기 어려움
2. **유지보수/레거시 부담** — 140개 벤더링 파일, ObjC 연동, 업데이트 어려움

이미 신규 차트(`RDStatisticsBarChartView`, `RDWorkoutPatternDonutChartView`,
`RDProfilePedometerBarChartView`)는 DGCharts 없이 CoreGraphics로 직접 구현 중 →
탈(脫)DGCharts 방향은 이미 시작됨.

## 결정된 방향 (확정)

| 항목 | 결정 | 이유 |
|---|---|---|
| 라이브러리 교체 vs 직접 구현 | **직접 구현** | 다른 범용 라이브러리도 같은 디자인 한계·새 레거시를 낳음 |
| 범위 | **크로스플랫폼 + 진짜 코드 공유** (iOS/Android) | 사용자 요구 |
| 아키텍처 | **A안: 공유 코어 + 네이티브 렌더러** | 공유 가치 높은 "계산"만 공유, 렌더링은 네이티브로 느낌·성능 유지 |
| 공유 코어 언어 | **KMP (Kotlin Multiplatform)** | 안드로이드 팀이 이미 Kotlin → Android는 FFI 0, iOS만 신규 비용 |
| 코어 출력 좌표 | **정규화 0~1 좌표만** | 코어가 화면 해상도/스케일을 몰라도 됨(more pure), 픽셀 변환은 렌더러 |
| 리포 구조 | **독립 리포 `lumipol-graph`** (양 앱이 의존) | iOS/Android 앱이 별도 리포라 공유 코어는 제3의 리포여야 함 |
| 첫 파일럿 | **라인 차트(페이스/심박수)** | 러닝 결과 분석의 핵심 + DGCharts에서 가장 아픈 부분 → 아키텍처 검증값 최고 |
| OSS | **"추출 가능한 구조"로 시작, 나중에 공개** | 처음부터 OSS 운영 부담(문서·이슈·버저닝) 지지 않음. 구조는 OSS 친화적 |

### 명시적 비목표 (YAGNI)
- 처음부터 모든 차트 종류 지원 ❌ (라인 1종부터)
- 웹 타겟 ❌
- "아무 데이터나" 범용 API ❌
- 기존 DGCharts 사용처 일괄 교체 ❌ (병존 후 점진 교체)

### 의도적으로 배제한 대안
- **라이브러리 교체** (다른 OSS 차트 도입): 디자인 한계·새 레거시 그대로 → 기각
- **B안 Compose Multiplatform**: 코드 공유 최대지만 iOS에 Kotlin/Skia UI 런타임 통째 탑재, 네이티브 느낌 포기 → 기각
- **C안 WebView + JS 차트**: 코드 공유 가장 쉽지만 스크롤/제스처 지연, 60fps·세밀 터치 약함 → 기각
- **Rust 코어**: iOS 측 더 깨끗하고 웹/WASM 보너스 있으나, 팀이 Kotlin 보유 → KMP가 총 신규 비용 최소

## 리포 구조

```
lumipol-graph/                  ← 신규 독립 GitHub 리포 (KMP)
├── core/                   ← Kotlin Multiplatform 공유 코어 (UI 0)
│   ├── 데이터 모델 (ChartData / LineChartData)
│   ├── 스케일링 (도메인 → 정규화 0~1)
│   ├── 축 눈금 "nice tick" 계산
│   ├── 보간 / 통계
│   └── commonTest 단위 테스트
├── ios-renderer/           ← Swift Package
│   ├── core의 xcframework 소비
│   └── RDChartView (UIView) — CoreGraphics/SwiftUI 렌더 + 터치
├── android-renderer/       ← Kotlin: Canvas/Compose 렌더러
└── samples/                ← 양 플랫폼 데모 앱
```

- iOS Runday는 `lumipol-graph`를 **SPM 의존성**으로 소비 (코어=xcframework, 렌더러=Swift Package).
- Runday는 라이브러리의 한 명의 "사용자"가 된다.

## 3계층 분리 (핵심 아키텍처)

| 계층 | 위치 | 책임 | 공유 |
|---|---|---|---|
| **① Core** | `core/` Kotlin (KMP) | 데이터 모델, 도메인→**정규화 0~1** 스케일 변환, 축 tick 계산, 보간, 통계 | ✅ |
| **② Renderer** | iOS Swift / Android Kotlin | 0~1 좌표 → 픽셀 변환, CoreGraphics/Canvas로 그림, 애니메이션 | ❌ |
| **③ Interaction** | iOS Swift / Android Kotlin | 터치 → 코어에 "이 x 데이터 뭐야?" 질의, 마커 표시 | ❌ (질의 로직은 코어) |

### 인터페이스 계약
- 입력: `LineChartData(points: [(x, y)], config)`
- 코어 출력 `LineChartLayout`:
  - 정규화 점 좌표 `[(0~1, 0~1)]`
  - 축 눈금 위치 `[(label, 0~1)]`
  - (선택) 보간된 곡선 점
  - y축 min/max 자동 산정
- 렌더러는 `LineChartLayout`만 받아 `0~1 × bounds → 픽셀` 변환 후 그린다.
- 이 경계가 명확해야 코어 내부 변경이 렌더러를 깨지 않는다.

## Runday 통합 / 마이그레이션 경로 (점진)

1. iOS 렌더러가 `RDChartView`(UIView 서브클래스) 제공 → DGCharts와 **병존**
2. 화면 1개에만 적용해 실사용 검증 (후보: 신규 화면 / 분석 리포트 페이스 그래프 — **미확정**)
3. 검증 후 나머지 라인차트 사용처 점진 교체, DGCharts 라인 의존 제거
4. 바/파이/도넛은 범위 밖 → DGCharts 또는 기존 CoreGraphics 커스텀 유지
- ObjC 화면(`RDAnalyticsReportViewController.m` 등)에서 쓰려면 `RDChartView`를 `@objc` 노출하거나 Swift 래퍼로 감쌈.

### Runday 프로젝트 규칙 연동 (중요)
- **단위 변환은 코어가 하지 않는다.** 거리·페이스·시간 포맷은 Runday의 `RunPaceUtils`로 처리한
  라벨 문자열을 코어/렌더러에 전달. (코어는 숫자만, 표시 포맷은 호출측)
- 신규 파일명 `RD` 접두사 규칙은 Runday 쪽 래퍼에만 적용 (라이브러리 내부는 자체 네이밍).

## 1차 파일럿 — 라인차트 수직 슬라이스

KMP코어 → iOS네이티브 렌더 **전체 파이프라인을 끝까지** 증명하는 최소 단위.

**Core (KMP)**
- `LineChartData` 입력 → `LineChartLayout`(정규화 좌표 + tick + 보간점) 출력
- y축 min/max 자동, "nice tick" 알고리즘

**iOS Renderer (Swift)**
- `RDChartView`가 `LineChartLayout` 받아 CoreGraphics로 선/그라데이션/점 렌더
- 0~1 × bounds → 픽셀 변환
- 등장 애니메이션(선 그리기) + 터치 마커

**증명 목표(Definition of Done)**
- xcframework 빌드 → SPM 소비 → 실 디바이스에서 페이스 그래프 1개 렌더 + 터치 동작 확인

## 테스트 전략
- **Core**: KMP `commonTest`로 스케일/tick/보간 단위 테스트 (양 플랫폼 공통 1벌) — 공유 최대 이득
- **iOS Renderer**: 스냅샷 테스트(렌더 이미지 비교) + 0~1→픽셀 변환 단위 테스트
- Runday 통합 후: 기존 UI 테스트 플로우 영향 없는지 확인

## 미해결 결정 (새 세션에서 정할 것)
1. **첫 적용 화면**: 신규 화면 먼저 vs 분석 리포트 페이스 그래프 먼저
2. **보간 위치**: 곡선 스무딩을 코어가 계산 vs 직선만 코어 + 곡선은 렌더러
3. **KMP iOS 산출물 형태**: xcframework 직접 vs SPM 래핑(예: KMMBridge/SKIE) 검토
4. **버전/배포**: 초기엔 Git 태그 + SPM, Maven 퍼블리시 시점

## 다음 단계
1. 새 디렉토리 `lumipol-graph` 생성 + git init
2. 이 문서를 `docs/DESIGN.md`로 이동
3. KMP 코어 스캐폴딩 → 라인차트 파일럿(위 DoD)부터 TDD
4. 이 프로젝트를 관리할 깃 주소 : https://github.com/daehocho/lumipol-graph.git
