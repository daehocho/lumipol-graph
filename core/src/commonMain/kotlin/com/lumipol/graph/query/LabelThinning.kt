package com.lumipol.graph.query

import kotlin.math.ceil
import kotlin.math.max

/**
 * 균등 라벨 솎아내기 stride 계산(양 플랫폼 공유).
 *
 * [count]개 슬롯(각 slot = [plotWidthPx]/[count])에 라벨을 균등 간격으로만 표시하기 위해,
 * 이웃한 표시 라벨의 중심 간격(stride*slot)이 (labelWidth+gap) 이상이 되는 **최소 stride**를 반환한다.
 * 렌더러가 `i % stride == 0`인 인덱스에만 라벨을 그리면 가로 겹침이 사라진다.
 *
 * 장거리(42km=43스플릿)에서 값 라벨(예: "5'30\"")·x축 인덱스가 슬롯보다 넓어 포개지던
 * 문제(iOS/Android 공통)를 해소한다. 텍스트 폭 측정은 각 렌더러가 수행해 [labelWidthPx]로 넘긴다.
 *
 * @param count 막대(슬롯) 수.
 * @param plotWidthPx 플롯 영역 가로 폭(px).
 * @param labelWidthPx 표시할 라벨 중 가장 넓은 폭(px). 보수적으로 max 폭을 넘겨 겹침을 확실히 제거.
 * @param gapPx 이웃 라벨 사이 최소 여백(px). 음수는 0으로 간주.
 * @return 1 이상. 입력이 축퇴(count/plotWidth/labelWidth ≤ 0)면 1(솎지 않음).
 */
fun labelStride(count: Int, plotWidthPx: Double, labelWidthPx: Double, gapPx: Double = 0.0): Int {
    if (count <= 0 || plotWidthPx <= 0.0 || labelWidthPx <= 0.0) return 1
    val slot = plotWidthPx / count
    if (slot <= 0.0) return 1
    val needed = labelWidthPx + max(0.0, gapPx)
    return max(1, ceil(needed / slot).toInt())
}

/**
 * 마지막 라벨 우측 클램프(0.47.0)를 반영한 stride(양 플랫폼 공유, 0.49.0).
 *
 * 렌더러는 마지막 라벨이 슬롯을 넘칠 때(`lastLabelWidthPx > slot`) 오른쪽 끝을 플롯 끝에 맞춰
 * `(lastLabelWidthPx - slot)/2`만큼 왼쪽으로 민다. [labelStride]는 중앙 정렬 전제로 간격을
 * 잡으므로 그 이동량이 확보해 둔 여백([gapPx])을 먹어 **직전 표시 라벨과 겹친다**
 * (예: 9막대·slot 25.8·라벨 44px → stride 2, 겹침 1.6px). 클램프 이동량을 미리 폭에 얹어
 * 겹침을 없앤다 — 클램프가 걸리는 조합에서만 stride가 올라간다(그 밖은 [labelStride]와 동일).
 *
 * 남는 한계: 첫(0)·마지막은 [isLabelVisible]이 강제 표시하므로 `count <= stride`면 둘의 간격이
 * stride 미만이 될 수 있다(막대 수가 stride보다 적은 극단 조합) — 클램프와 무관한 기존 성질이다.
 *
 * @param count 막대(슬롯) 수.
 * @param plotWidthPx 플롯 영역 가로 폭(px).
 * @param labelWidthPx 표시할 라벨 중 가장 넓은 폭(px).
 * @param lastLabelWidthPx 마지막 라벨 폭(px) — 클램프 판정·이동량의 원천. 0 이하면 클램프 없음.
 * @param gapPx 이웃 라벨 사이 최소 여백(px). 음수는 0으로 간주.
 * @return 1 이상. 입력이 축퇴면 1(솎지 않음).
 */
fun labelStride(
    count: Int,
    plotWidthPx: Double,
    labelWidthPx: Double,
    gapPx: Double,
    lastLabelWidthPx: Double,
): Int {
    if (count <= 0 || plotWidthPx <= 0.0 || labelWidthPx <= 0.0) return 1
    val slot = plotWidthPx / count
    if (slot <= 0.0) return 1
    // 렌더러 클램프 조건(마지막 막대 중심 + 라벨 절반 > plot.maxX)은 slot 기준으로 정확히
    // lastLabelWidthPx > slot이고, 그때 이동량은 (lastLabelWidthPx - slot)/2다.
    val clampShift = max(0.0, (lastLabelWidthPx - slot) / 2.0)
    return labelStride(count, plotWidthPx, labelWidthPx + clampShift, gapPx)
}

/**
 * [labelStride]로 솎을 때 [index] 라벨을 그릴지 여부(양 플랫폼 공유).
 *
 * 단순 `index % stride == 0`은 마지막 스플릿(피니시·부분 스플릿) 라벨을 떨어뜨린다 — 시작은 보이고
 * 끝은 비는 비대칭. 이 함수는 **첫(0)·마지막(count-1)을 항상 표시**하고, 그 사이는 stride 배수만
 * 표시하되 마지막 라벨과 stride 미만으로 붙는 최대 배수는 숨겨 간격을 유지한다.
 *
 * @param index 대상 인덱스(0..count-1).
 * @param count 막대 수.
 * @param stride [labelStride] 결과(1 이상).
 */
fun isLabelVisible(index: Int, count: Int, stride: Int): Boolean {
    if (index == 0 || index == count - 1) return true
    if (stride <= 1) return true
    if (index % stride != 0) return false
    // 마지막(강제 표시) 라벨과 너무 가까운 최대 배수는 숨겨 겹침을 막는다.
    val lastMultiple = ((count - 1) / stride) * stride
    if (index == lastMultiple && (count - 1) - lastMultiple < stride) return false
    return true
}
