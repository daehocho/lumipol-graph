# 50 — 재발 방지 장치 (5단계)

- 작성일: 2026-07-27 / 승인: 위임

## 1. 동일성 골든 테스트 (0.7 하네스 승격)

- 골든 파일: `core/golden/core-dump.golden.json` — JVM 덤프를 정본으로 커밋.
- 실행: `scripts/golden-check.sh` — `:core:jvmTest` + `:core:iosSimulatorArm64Test`(하네스 태스크)
  → 두 덤프를 골든과 diff(`scripts/diff-core-dump.py`).
  - **JVM diff는 완전 일치(diffs=0 && drifts=0)** — 빠른 피드백 게이트.
  - **iOS diff도 완전 일치 요구** — A1(niceScale 결정론화) 이후 코어에 libm 경로가 없으므로
    드리프트 0이 기대치다. 드리프트가 다시 생기면 새 libm 의존이 유입된 것(정책 §5 위반 신호).
  - JVM/Native가 실제로 어긋난다는 확인 테스트: **완료** — 07 문서 실측(tiny_range 틱 4vs3)이 그 증거.
- 커버: 0.7 입력 데이터셋 전체(하네스 픽스처가 곧 골든 입력).
- 골든 갱신 절차: 의도 변경 시 `LUMIPOL_DUMP_DIR`로 새 덤프 생성 → diff를 커밋 메시지에 요약 →
  골든 교체를 코드와 **같은 커밋**에.

## 2. 렌더러 SceneDigest (30 문서 §2 채택안)

- 각 렌더러의 레이어 이름 계약 + 논리 기하(RGBA·폭·논리 좌표)를 07 형식 JSON으로 덤프하는
  테스트 전용 진입점 — iOS↔AOS를 같은 diff 스크립트로 비교(T2 1e-6).
- 스냅샷 이미지 테스트는 **플랫폼 내 회귀 전용**으로 유지 — 크로스플랫폼 동등성 주장에 사용 금지.
- 구현 시점: 트랙 B 렌더러 전환 항목(B1/B2/B6/B14)과 함께 증분.

## 3. 코어 버전 고정 검사

- xcframework에 빌드 원본 커밋 기록: `sync-xcframework.sh`가 `git rev-parse HEAD`를
  `LumipolGraph.xcframework/BUILD_COMMIT` 파일로 동봉.
- 검사 스크립트 `scripts/check-version-lock.sh`:
  1. `BUILD_COMMIT` == 현재 커밋에서 `core/`를 마지막으로 바꾼 커밋의 조상 여부
  2. `core/build.gradle.kts`·`android-renderer/build.gradle.kts` version 일치
  3. (수동) 양 앱 고정 버전 — 릴리스 체크리스트 항목
- 00 문서 §4 한계("규약 기반 간접 확인")의 직접 해소.

## 4. API 표면 스냅샷

- Kotlin: `binary-compatibility-validator` 플러그인 — `apiDump`를 커밋, `apiCheck`를 검사에 포함.
- iOS: 생성 헤더 `LumipolGraph.h`가 이미 체크인됨 — **헤더 diff가 곧 API diff**. 릴리스 전
  `git diff -- '*.h'`를 리뷰 산출물로 명시(체크리스트).

## 5. 경계 정책 체크 (금지 패턴)

`scripts/boundary-lint.sh` — 렌더러 소스 대상 grep 게이트(위반 시 실패, 허용은 인라인 주석 `// boundary-allow: 사유`):

| 패턴 | 잡는 것 |
|---|---|
| `String.format\|"%\w"\|"\$\{.*\}"` 내 표시 문자열 생성 (Formatter 파일 외) | 렌더러 포맷팅 신설 (정책 §4-4) |
| `log10\|pow\|sin\|cos\|exp\(` (core commonMain) | libm 재유입 (정책 §5) |
| 정책성 숫자 리터럴 신설: `val .* = [0-9]+\.[0-9]` (ChartDefaults 참조 없는 새 상수) | 상수 복제 재발 — 휴리스틱이므로 리뷰 체크리스트와 병행 |
| `minByOrNull.*abs\|floor(.*/.*)` (렌더러) | 스냅/히트테스트 재구현 |
| 앱 차트 디렉토리에도 동일 검사 적용(C 회수 후 잔류 감시) | |

## 6. PR/릴리스 체크리스트 (rd-charts-DESIGN.md 7장 규약에 추가)

- [ ] 골든 체크 통과(JVM+iOS, diffs=0/drifts=0)
- [ ] `apiCheck` 통과, 헤더 diff 리뷰
- [ ] 코어 공개 API 변경 시 xcframework 재생성 동일 커밋 + BUILD_COMMIT 확인
- [ ] 새 계산·상수·포맷이 렌더러/앱에 추가되지 않았는가(boundary-lint)
- [ ] 화면이 바뀌는 항목은 44 문서에 결정 기록이 있는가
