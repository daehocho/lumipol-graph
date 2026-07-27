# 40 — 트랙 A: 즉시 정합 (4단계)

- 작성일: 2026-07-27 / 승인: 위임
- 원칙: 구조 불변, 호출부 무변경, 항목 단위 커밋. **착수 전에 골든 테스트(50 문서) 먼저.**
- 21 문서 소결대로 SDK 내부 "값 맞추기" 대상은 적다 — 렌더러 상수 쌍은 이미 일치.

| # | 항목 | 수정 위치 | 기준과 근거 | 영향 | 회귀 위험 | 화면 변화 |
|---|---|---|---|---|---|---|
| A1 | **niceScale 결정론화** — `niceNum`의 `log10`/`pow`를 IEEE 기본 연산 기반 10진 지수 산출로 교체 | `core/scale/NiceScale.kt` | 하네스 실측(07): tiny_range JVM 4틱 vs iOS 3틱. 기준 = "양 타겟 동일"이며 어느 쪽 값이냐는 알고리즘 결과에 따름 — 실도메인 케이스는 변화 없어야 함(골든으로 증명) | 코어 전 차트 축 | 낮음(골든+기존 NiceScaleTest) | 실도메인 없음(경계 입력만) |
| A2 | **NaN 페이스 필터 통과 수정** — 필터 분기에 NaN 흡수(`pace = 0.0`) | `core/PaceSeriesEngine.kt:52` | NaN은 모든 비교 false로 유효 표본에 편입(07 관측). 기준 = 코어의 명시 의도("0 이하·범위 밖은 무효") | preprocess | 낮음(하네스 P07 케이스가 즉시 검증) | NaN 입력 기록에서만(현재는 선이 깨지던 케이스) |
| A3 | 라인 등장 애니 이징·기본값 통일 | iOS `RDChartView.swift:224`(easeOut) ↔ AOS `RDLineChart.kt`(EmphasizedDecelerate, 기본 on) | **화면 변화 항목 → 44 문서 결정 후 실행.** 잠정 기준: iOS(.easeOut) — 선행 구현이자 코어 의도 부재 시 기존 iOS 화면 유지 원칙 | 등장 0.6s 곡선 | 낮음 | 있음(미세) — 44 승인 대기 |
| A4 | AOS `normalizedStat` 사문 리터럴 — **앱 결함이므로 트랙 C2에 병합** (SDK 수정 아님) | — | C2에서 포맷 코어화와 함께 해소 | — | — | — |

트랙 B만으로 해결되는 항목은 여기서 제외했다(중복 작업 방지): 색 앵커·컬러맵(B5/B6),
AxisScale(B1), ZoomState(B3) 등은 "값이 이미 일치"하므로 A 단계 불필요.

## 실행 기록

- [x] A0 골든 테스트 기반 — `scripts/golden-check.sh` + `core/golden/` (커밋 cdaaa41)
- [x] A1 — niceNum 결정론화. 실측: JVM↔iOS **비트 동일**(diffs=0, drifts=0), 골든 대비
  변화는 tiny_range 3건뿐(4틱→3틱 — 종전 Native 결과로 수렴). 골든 동일 커밋 갱신
- [x] A2 — NaN/Inf 페이스 명시 무효화 + 단위 테스트. 골든 델타는 P07_nan_pace 케이스에만
  국한됨을 확인(26건 전부 해당 케이스) 후 갱신. 양 플랫폼 게이트 통과
- [x] **A3** — D5 확정(2026-07-28) 반영. 이징: AOS EmphasizedDecelerate(0.05,0.7,0.1,1.0) →
  easeOut(0,0,0.58,1) — 계수는 코어 `ChartDefaults.ENTRANCE_EASING_*`(B7), 렌더러는 구동만.
  기본값: 라인 등장 애니 iOS·AOS 모두 기본 off(`ENTRANCE_ENABLED_DEFAULT`) — 현재 동작을
  유지하려면 양 앱이 명시 on 1줄 필요(트랙 C 앱 커밋에 포함)
