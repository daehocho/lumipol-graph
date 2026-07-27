# 00 — 기반 확인 (0단계)

- 작성일: 2026-07-27
- 게이트 판정: **통과** — 중단 조건 미해당 (아래 §4)
- 검증 시점 기준 커밋: `3248252` (태그 `0.29.0`), 워킹트리 클린

---

## 1. 코어 KMP 타겟 구성과 배포 형태

### 1.1 타겟

`core/build.gradle.kts:15-32`:

```kotlin
kotlin {
    jvmToolchain(17)
    jvm() // 호스트에서 빠른 commonTest 실행용
    androidTarget {
        publishLibraryVariants("release")
        ...
    }
    if (System.getProperty("os.name").startsWith("Mac")) {
        val xcf = XCFramework("LumipolGraph")
        listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach { target ->
```

- `jvm` — commonTest 빠른 실행용 (배포 안 함)
- `androidTarget` — release variant만 Maven 발행
- `iosArm64` / `iosSimulatorArm64` / `iosX64` — **macOS 호스트에서만 등록** (조건부).
  근거: `core/build.gradle.kts:10-11` 주석
  > `// JitPack(Linux 빌더)은 Kotlin/Native iOS 타겟을 빌드할 수 없다 — macOS 호스트에서만 등록.`
  > `// iOS 소비는 Maven이 아니라 xcframework(SPM)라서 발행 아티팩트에 iOS 타겟이 없어도 무영향.`

### 1.2 배포

| 플랫폼 | 형태 | 좌표/경로 |
|---|---|---|
| Android | JitPack Maven | `com.github.daehocho.lumipol-graph:renderer:0.29.0` (`android-renderer/build.gradle.kts:8-9` `group = "com.github.daehocho.lumipol-graph"` / `version = "0.29.0"`, `artifactId = "renderer"`). 코어는 `api(project(":core"))`(`android-renderer/build.gradle.kts` dependencies 블록)로 POM compile 스코프 전이 |
| iOS | 체크인된 xcframework + SPM binaryTarget | `Package.swift:14-17` `.binaryTarget(name: "LumipolGraph", path: "ios-renderer/Frameworks/LumipolGraph.xcframework")` |

xcframework 재생성 절차: `scripts/sync-xcframework.sh:6-8`

```bash
./gradlew :core:assembleLumipolGraphReleaseXCFramework
mkdir -p ios-renderer/Frameworks
rsync -a --delete core/build/XCFrameworks/release/LumipolGraph.xcframework ios-renderer/Frameworks/
```

릴리스 규약: main 직커밋 + 버전 태그(`0.29.0` 형식, v 접두사 없음). 코어 공개 API 변경 시
재생성한 xcframework를 **같은 커밋**에 포함. 버전 선언은 `core/build.gradle.kts:13`과
`android-renderer/build.gradle.kts:9` 두 곳.

## 2. 브릿지 방식 (KMP → iOS)

- Kotlin/Native가 생성한 ObjC 프레임워크를 Swift가 직접 import:
  `ios-renderer/Sources/LumipolGraphUI/*.swift` 전부 `import LumipolGraph` (예: `AxisScale.swift:2`)
- 생성 헤더는 엄브렐라 헤더 1개: `ios-renderer/Frameworks/LumipolGraph.xcframework/ios-arm64/LumipolGraph.framework/Headers/LumipolGraph.h`
- Swift export(SKIE, swift-klib 등) 미사용 — 순정 ObjC interop. 따라서 2단계 브릿지 감사의
  전 항목(박싱, 널러빌리티 소실, 기본 인자 소실, sealed 비완전 매칭)이 그대로 적용 대상이다.
- 슬라이스: `ios-arm64`, `ios-arm64_x86_64-simulator` (xcframework `Info.plist`)

## 3. 코어의 플랫폼별 분기 — 전수 목록

**소스 코드 내 분기 없음.**

- `expect`/`actual` 선언: 0건 (`grep -rn "expect\b|actual\b" core/src` — 테스트의 변수명
  `expected/actual`만 검출됨)
- 소스셋: `commonMain`(21개 .kt, 전 로직), `commonTest`, `androidMain`(**`AndroidManifest.xml` 1개뿐**, 코드 없음). `iosMain` 없음
- 조건부 컴파일: 소스에는 없음. 유일한 플랫폼 분기는 빌드 스크립트의 호스트 OS 검사
  (`core/build.gradle.kts:24` `if (System.getProperty("os.name").startsWith("Mac"))`) — 타겟 등록용, 코드 동작과 무관

→ 코어 계산 로직은 100% commonMain. 코어 기인 차이가 있다면 원인 축은 3번(브릿지)이지
1번(중복)·2번(비대칭)이 아니다. JVM vs Native의 수학 함수·부동소수 차이 가능성은 남는다
(0.5단계 T2, 5단계 골든 테스트에서 검증).

## 4. 버전 정렬 검증 (중단 조건)

| # | 항목 | 값 | 근거 |
|---|---|---|---|
| 1 | 코어 HEAD | `3248252` = 태그 `0.29.0` | `git tag --points-at HEAD` → `0.29.0`; `core/build.gradle.kts:13` `version = "0.29.0"` |
| 2 | 체크인된 xcframework 빌드 원본 | `3248252` (규약 기반 판정, 아래 한계 참조) | `git log -1 -- ios-renderer/Frameworks` → `3248252 feat: 케이던스 보조축 합류 릴리스 (0.29.0)`; 그 커밋 이후 `core/` 변경 0건 (`git log 3248252..HEAD -- core/` 빈 출력) |
| 3a | Runday_AOS 고정 버전 | `0.29.0` | `Day_RunDayProject/gradle/watchlibs.versions.toml:85` `mobile-lumipol = "0.29.0"` |
| 3b | Runday_IOS 고정 버전 | `0.29.0`, 리비전까지 HEAD 일치 | `RunDay.xcodeproj/project.pbxproj:20899-20900` `kind = exactVersion; version = 0.29.0;`; `RunDay.xcworkspace/.../Package.resolved` `"revision" : "32482525301cf411e04911e0031055821536b494"` |

**판정: 세 버전 모두 일치. 1단계 진입 가능.**

한계 (5단계 재발 방지 장치 후보):
- xcframework 바이너리에 빌드 원본 커밋 해시가 기록되어 있지 않다 (`Info.plist`엔 라이브러리
  슬라이스 메타데이터만 존재). 위 #2는 "xcframework가 마지막으로 갱신된 커밋 = HEAD이고 이후
  core/ 변경이 없다"는 커밋 이력 + 동일 커밋 규약으로 간접 확인한 것이다. 규약을 어긴 커밋이
  과거에 있었는지까지 증명하지는 못한다 → **커밋 해시를 xcframework에 기록·대조하는 검사**가
  5단계 가드레일로 필요하다.

## 5. 저장소 구조 스냅샷 (다음 세션 참조용)

```
core/src/commonMain/kotlin/com/lumipol/graph/
├── LineChartEngine.kt, BarChartEngine.kt, DonutEngine.kt,
│   HeartRateZoneEngine.kt, PaceSeriesEngine.kt        # 차트별 엔진
├── PaceSeriesId.kt, SeriesSelection.kt                # 시리즈 식별·선택
├── model/  Input, Output, BarInput, PaceInput, DonutInput, HeartRateZoneInput
├── scale/  NiceScale.kt (Heckbert), AxisDomain.kt
├── query/  Nearest, BarHitTest, LabelThinning, HeightFractions, AreaInterpolation
└── stats/  Stats.kt
```

렌더러 — **양쪽에 동일 이름 파일이 다수 존재** (1단계 중복/비대칭 조사 시작점):

| iOS (`ios-renderer/Sources/LumipolGraphUI/`) | Android (`android-renderer/src/main/.../renderer/`) |
|---|---|
| ChartStyle.swift | ChartStyle.kt (`// iOS: ChartStyle.swift` 헤더 주석으로 대응 명시) |
| AxisScale.swift | AxisScale.kt |
| PlotArea.swift | PlotArea.kt |
| PaceColormap.swift | PaceColormap.kt |
| AreaSilhouette.swift | AreaSilhouette.kt |
| ZoomState.swift | ZoomState.kt |
| TouchMarker.swift | TouchMarker.kt |
| RDChartView.swift / RDBarChartView.swift / RDHeartRateZoneView.swift | RDLineChart.kt / RDBarChart.kt / RDHeartRateZoneChart.kt |
| ChartLayerBuilder.swift, Exports.swift | LineChartDrawing.kt, LineChartInteraction.kt, LineGestures.kt, LineChartMarkerController.kt, LineChartZoomController.kt, EntranceAnimation.kt, ChartCallbacks.kt, DonutSelectionState.kt |

관찰 메모 (판정 아님, 1단계 입력):
- 포맷팅 코드가 렌더러에 있다: AOS `RDBarChart.kt`·`RDLineChart.kt`, iOS `RDBarChartView.swift`·`RDChartView.swift`에서 `Formatter|format(` 검출. 코어엔 포맷팅 모듈 없음 → 목표 아키텍처상 회수 대상 후보 (추정)
- AOS `ChartStyle.kt` KDoc이 "숫자 값(라인 폭·여백·비율·dash)은 iOS와 정확히 일치한다"고
  **수동 동기화 규약**을 선언 — 상수 복사본 구조로, 2단계 상수 대조표(21-constants-diff.md)의 1순위 대상

## 6. 참고 — 검증·실행 커맨드 (사용자 실행용)

- iOS 테스트: `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild test -scheme LumipolGraphUI -destination "platform=iOS Simulator,name=iPhone 17 Pro"` (swift test는 UIKit 때문에 불가)
- AOS 앱 버전 반영 검증: `Day_RunDayProject`에서 `./gradlew :Rundayfree:dependencies --configuration rundayDebugCompileClasspath`
- JitPack 빌드는 태그 푸시 후 첫 아티팩트 요청 시 트리거됨
