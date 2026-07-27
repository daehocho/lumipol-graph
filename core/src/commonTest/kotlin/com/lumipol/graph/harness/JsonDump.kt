package com.lumipol.graph.harness

/**
 * 0.7단계 대조 하네스 전용 결정론적 직렬화 (docs/refactor/07-harness.md).
 *
 * 플랫폼 `Double.toString`에 의존하지 않는다 — JVM과 Kotlin/Native는 부동소수 문자열화
 * 규칙이 달라, 그대로 쓰면 "값 차이"와 "표기 차이"가 diff에서 섞인다. 고정 소수 12자리를
 * IEEE 기본 연산(+,-,*,floor/truncate)만으로 직접 만들며, 기본 연산은 두 타겟에서
 * 비트 동일하게 동작하므로 같은 입력 비트 → 같은 문자열이 보장된다.
 */

private const val FRACTION_DIGITS = 12

/** 정수부 floor→Long 변환이 정확한 상한(2^53 대비 여유). 차트 도메인 값은 전부 이 아래다. */
private const val MAX_ABS = 9.0e15

fun jnum(v: Double): String {
    if (v.isNaN()) return "\"NaN\""
    if (v == Double.POSITIVE_INFINITY) return "\"Infinity\""
    if (v == Double.NEGATIVE_INFINITY) return "\"-Infinity\""
    val neg = v < 0.0
    val a = if (neg) -v else v
    check(a < MAX_ABS) { "fixed-point formatter supports |v| < 9e15" }
    var ip = a.toLong()
    var frac = a - ip.toDouble()
    val digits = IntArray(FRACTION_DIGITS)
    for (i in 0 until FRACTION_DIGITS) {
        frac *= 10.0
        var d = frac.toInt()
        if (d > 9) d = 9
        if (d < 0) d = 0
        digits[i] = d
        frac -= d
    }
    // 13자리째가 5 이상이면 반올림 캐리
    if (frac >= 0.5) {
        var i = FRACTION_DIGITS - 1
        while (i >= 0 && digits[i] == 9) {
            digits[i] = 0
            i--
        }
        if (i >= 0) digits[i]++ else ip++
    }
    val sb = StringBuilder(FRACTION_DIGITS + 8)
    if (neg && (ip != 0L || digits.any { it != 0 })) sb.append('-') // "-0.000…" 방지
    sb.append(ip)
    sb.append('.')
    for (d in digits) sb.append('0' + d)
    return sb.toString()
}

fun jnum(v: Double?): String = if (v == null) "null" else jnum(v)
fun jint(v: Int): String = v.toString()
fun jbool(v: Boolean): String = if (v) "true" else "false"

fun jstr(s: String?): String {
    if (s == null) return "null"
    val sb = StringBuilder(s.length + 2)
    sb.append('"')
    for (c in s) {
        when {
            c == '"' -> sb.append("\\\"")
            c == '\\' -> sb.append("\\\\")
            c == '\n' -> sb.append("\\n")
            c == '\r' -> sb.append("\\r")
            c == '\t' -> sb.append("\\t")
            c.code < 0x20 -> {
                sb.append("\\u")
                sb.append(c.code.toString(16).padStart(4, '0'))
            }
            else -> sb.append(c)
        }
    }
    sb.append('"')
    return sb.toString()
}

/** 값들은 이미 렌더된 JSON 토큰이어야 한다. 키 순서 = 선언 순서(결정론). */
fun jobj(vararg fields: Pair<String, String>): String =
    fields.joinToString(",", "{", "}") { (k, v) -> "${jstr(k)}:$v" }

fun jarr(items: List<String>): String = items.joinToString(",", "[", "]")

/**
 * 큰 배열 다이제스트 — 개수(T1) + 결정론적 표본(처음부터 ceil(n/32) 간격 + 마지막).
 * 다운샘플 인덱스가 한 칸이라도 밀리면 표본 값이 어긋나므로 발산 탐지력은 유지된다.
 */
fun <T> digestList(items: List<T>, threshold: Int = 64, render: (T) -> String): String {
    if (items.size <= threshold) {
        return jobj("count" to jint(items.size), "items" to jarr(items.map(render)))
    }
    val step = (items.size + 31) / 32
    val sampled = buildList {
        var i = 0
        while (i < items.size) {
            add(i)
            i += step
        }
        if (last() != items.lastIndex) add(items.lastIndex)
    }
    return jobj(
        "count" to jint(items.size),
        "sampleStep" to jint(step),
        "sampleIndices" to jarr(sampled.map { jint(it) }),
        "samples" to jarr(sampled.map { render(items[it]) }),
    )
}
