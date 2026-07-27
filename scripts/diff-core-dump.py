#!/usr/bin/env python3
"""lumipol-graph 0.7단계 하네스 덤프 diff.

사용법:
    python3 scripts/diff-core-dump.py <jvm덤프.json> <ios덤프.json>

판정 기준 (docs/refactor/05-equivalence-criteria.md, 승인 완료):
  - T1: 구조(키·배열 길이), 문자열, 정수, bool — 완전 일치
  - T2: 실수 — 절대오차 max(1e-9, |a|*1e-9) 이내면 통과 (큰 도메인 값은 상대오차로 환산)
  - 허용 오차 안이지만 비트 불일치인 값은 "드리프트"로 별도 집계
    (JVM vs Kotlin/Native 수학 함수 ULP 차이의 실측 증거 — 5단계 골든 테스트 근거)

종료 코드: 0 = 동등, 1 = 허용 오차 초과 차이 존재, 2 = 사용 오류.
"""
import json
import sys

ABS_TOL = 1e-9  # 05 문서 승인값 — 코어 산출물(JVM vs iosNative)

diffs = []    # 허용 오차 초과 (실패)
drifts = []   # 허용 오차 내 비트 불일치 (정보)


def tol(a: float) -> float:
    return max(ABS_TOL, abs(a) * ABS_TOL)


def compare(a, b, path):
    if type(a) is not type(b):
        # int vs float 혼합은 생성기가 동일하므로 타입 불일치 자체가 이상 신호
        diffs.append((path, f"type: {type(a).__name__} != {type(b).__name__}", a, b))
        return
    if isinstance(a, dict):
        for k in sorted(a.keys() | b.keys()):
            if k not in a or k not in b:
                diffs.append((f"{path}/{k}", "missing key", k in a and "left-only" or "right-only", None))
                continue
            compare(a[k], b[k], f"{path}/{k}")
    elif isinstance(a, list):
        if len(a) != len(b):
            diffs.append((path, f"length: {len(a)} != {len(b)} (T1 위반)", len(a), len(b)))
        for i, (x, y) in enumerate(zip(a, b)):
            compare(x, y, f"{path}[{i}]")
    elif isinstance(a, float):
        if a == b:
            return
        if abs(a - b) <= tol(a):
            drifts.append((path, a, b, abs(a - b)))
        else:
            diffs.append((path, f"float: |Δ|={abs(a - b):.3e} > tol={tol(a):.3e}", a, b))
    else:  # str, int, bool, None
        if a != b:
            diffs.append((path, "value", a, b))


def main():
    args = [a for a in sys.argv[1:] if a != "--strict"]
    strict = "--strict" in sys.argv  # 골든 게이트: 드리프트도 실패로 취급
    if len(args) != 2:
        print(__doc__)
        return 2
    with open(args[0]) as f:
        left = json.load(f)
    with open(args[1]) as f:
        right = json.load(f)

    compare(left, right, "")

    if drifts:
        print(f"── 허용 오차 내 드리프트 {len(drifts)}건 (JVM↔Native 부동소수 차이 실측) ──")
        for path, a, b, d in drifts[:50]:
            print(f"  {path}: {a!r} vs {b!r} (|Δ|={d:.3e})")
        if len(drifts) > 50:
            print(f"  … 외 {len(drifts) - 50}건")
    if diffs:
        print(f"── 허용 오차 초과 차이 {len(diffs)}건 ──")
        for path, why, a, b in diffs[:100]:
            print(f"  {path}: {why}\n    left : {a!r}\n    right: {b!r}")
        if len(diffs) > 100:
            print(f"  … 외 {len(diffs) - 100}건")
        print(f"\n결과: 불일치 (diffs={len(diffs)}, drifts={len(drifts)})")
        return 1
    if strict and drifts:
        print(f"\n결과: 불일치(strict) — 드리프트 {len(drifts)}건은 골든 게이트에서 실패다")
        return 1
    print(f"결과: 동등 (diffs=0, drifts={len(drifts)})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
