package com.lumipol.graph

import com.lumipol.graph.model.BarColorAnchors
import kotlin.math.roundToInt

/**
 * 페이스 컬러맵(B6) — 지도 페이스 색 3구간 공식의 단일 원본. 렌더러 2벌(그림자) +
 * 소비 앱 2벌(colorizer)이 각자 들고 있던 공식의 회수 목표 API.
 *
 * 색은 `0xAARRGGBB` Long — 플랫폼 타입(UIColor/Compose Color) 변환만 렌더러/앱 소관.
 *
 * 연속(비색약) 공식: 앵커([BarColorAnchors])로 3구간을 잡고 구간 내 선형 보간.
 * 빠름(값 낮음) 파랑↔청록, 평균 근처 초록↔노랑, 느림(값 높음) 노랑↔빨강.
 * average 앵커로 pace1/pace2를 잡아 평균 근처에 색 해상도를 더 준다.
 * `slowest <= fastest`(축퇴)는 중간 초록 폴백.
 *
 * 색약(colorBlind) 공식: 이산 4색(Okabe-Ito 계열) — 보간 없음.
 * 두 앱 규칙이 달랐다(iOS: 2구간을 50/50 초록/노랑 분할, AOS: 2구간 전부 노랑).
 * **iOS안 채택**(44 문서 D12): 4색 대역이 값 범위 전체에 온전히 나타나 구간 변별이 유지된다.
 * - 1구간(< pace1): `fastest + 0.2×len1` 미만 파랑, 이상 초록
 * - 2구간(< pace2): `pace1 + 0.5×len2` 미만 초록, 이상 노랑
 * - 3구간: 빨강. 값은 [fastest, slowest]로 클램프, 축퇴는 초록.
 */
object PaceColormap {

    /** 색바 범례 기본 샘플 수 — D8 결정(44 문서): 인자화 + 기본 40(부드러움 우위). */
    const val LEGEND_STOP_COUNT: Int = 40

    /** 색약 이산 4색 (Okabe-Ito 계열) — 앱 2벌이 공유하던 동일 RGBA. */
    const val COLOR_BLIND_BLUE: Long = 0xFF0000FF
    const val COLOR_BLIND_GREEN: Long = 0xFF009E73
    const val COLOR_BLIND_YELLOW: Long = 0xFFFFFF00
    const val COLOR_BLIND_RED: Long = 0xFFD55E00

    /** [value](sec/unit, 낮을수록 빠름)의 막대/구간 색. */
    fun rgba(value: Double, anchors: BarColorAnchors, colorBlind: Boolean): Long {
        val f = anchors.fastest
        val s = anchors.slowest
        val a = anchors.average
        if (s <= f) return if (colorBlind) COLOR_BLIND_GREEN else argb(0.0, 1.0, 0.0)
        val pace1 = a - (a - f) * 0.70
        val pace2 = a + (s - a) * 0.25
        val length1 = pace1 - f
        val length2 = pace2 - pace1
        val length3 = s - pace2

        if (colorBlind) {
            val p = value.coerceIn(f, s)
            return when {
                p < pace1 -> if (p < f + 0.2 * length1) COLOR_BLIND_BLUE else COLOR_BLIND_GREEN
                p < pace2 -> if (p < pace1 + 0.5 * length2) COLOR_BLIND_GREEN else COLOR_BLIND_YELLOW
                else -> COLOR_BLIND_RED
            }
        }

        val p = value
        return when {
            p < pace1 -> { // 파랑↔청록 (green 하한 0.6 — 어두워 보임 방지)
                val cv = if (length1 > 0) ((pace1 - maxOf(f, p)) / length1).coerceIn(0.0, 1.0) else 0.0
                argb(0.0, 1.0 - 0.4 * cv, 1.0)
            }
            p < pace2 -> { // 초록↔노랑
                val cv = if (length2 > 0) ((pace2 - p) / length2).coerceIn(0.0, 1.0) else 0.0
                argb(1.0 - cv, 1.0, 0.0)
            }
            else -> { // 노랑↔빨강
                val cv = if (length3 > 0) ((s - minOf(s, p)) / length3).coerceIn(0.0, 1.0) else 0.0
                argb(1.0, cv, 0.0)
            }
        }
    }

    /**
     * 색바 범례 스톱(D8/C5) — [fastest, slowest]를 [count]개 지점(양끝 포함)으로 등분 샘플.
     * 그리기는 앱 소관(뷰 배치), 색 산출만 코어. 색약 모드의 중복 스톱 접기(하드 스톱)도 앱 소관.
     * count<2 또는 축퇴 앵커면 단색 [count]개(최소 1개).
     */
    fun legendStops(
        anchors: BarColorAnchors,
        count: Int = LEGEND_STOP_COUNT,
        colorBlind: Boolean = false,
    ): List<Long> {
        val n = maxOf(1, count)
        if (n == 1 || anchors.slowest <= anchors.fastest) {
            return List(n) { rgba(anchors.fastest, anchors, colorBlind) }
        }
        val span = anchors.slowest - anchors.fastest
        return List(n) { i ->
            rgba(anchors.fastest + span * i / (n - 1), anchors, colorBlind)
        }
    }

    /** 채널 0.0~1.0 → 0xAARRGGBB(알파 FF). 반올림 양자화. */
    private fun argb(r: Double, g: Double, b: Double): Long {
        val ri = (r.coerceIn(0.0, 1.0) * 255).roundToInt().toLong()
        val gi = (g.coerceIn(0.0, 1.0) * 255).roundToInt().toLong()
        val bi = (b.coerceIn(0.0, 1.0) * 255).roundToInt().toLong()
        return 0xFF000000L or (ri shl 16) or (gi shl 8) or bi
    }
}
