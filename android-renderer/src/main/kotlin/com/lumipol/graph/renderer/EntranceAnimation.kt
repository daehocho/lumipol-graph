// 등장 애니 진행도 공용 헬퍼 — RDBarChart(성장)·RDHeartRateZoneChart(sweep)가 공유.
package com.lumipol.graph.renderer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.asFloatState
import androidx.compose.runtime.remember

/**
 * 등장 진행도(0→1). [trigger]가 바뀌거나 [animate]가 켜질 때마다 0부터 재생하고,
 * [animate]=false면 즉시 완성(1f)이다.
 */
@Composable
internal fun rememberEntranceProgress(trigger: Any?, animate: Boolean, durationMs: Int): State<Float> {
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(trigger, animate) {
        if (animate) {
            // 정착값(1f)에서 animateTo(1f)는 no-op — 토글·trigger 교체 재생은 0 스냅이 전제.
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMs, easing = EmphasizedDecelerate))
        } else {
            progress.snapTo(1f)
        }
    }
    return progress.asState()
}
