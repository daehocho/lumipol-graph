package com.lumipol.graph.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumipol.graph.BarChartEngine
import com.lumipol.graph.model.Axis
import com.lumipol.graph.model.BarChartData
import com.lumipol.graph.model.ChartAxis
import com.lumipol.graph.model.ChartConfig
import com.lumipol.graph.model.DonutChartData
import com.lumipol.graph.model.DonutColorRole
import com.lumipol.graph.model.DonutSegment
import com.lumipol.graph.model.LineChartData
import com.lumipol.graph.model.Marker
import com.lumipol.graph.model.Point
import com.lumipol.graph.model.RefBand
import com.lumipol.graph.model.Series
import com.lumipol.graph.model.SeriesRole
import com.lumipol.graph.model.SplitSample
import com.lumipol.graph.renderer.RDBarChart
import com.lumipol.graph.renderer.RDHeartRateZoneChart
import com.lumipol.graph.renderer.RDLineChart
import kotlin.math.roundToInt

/**
 * android-renderer 데모 앱. 라인(스크럽·줌·고스트·배경 area)/바/심박존 3화면.
 * iOS 샘플(`samples/ios`)과 동일한 TestFixtures 데이터를 쓴다. material3 미의존(foundation만) —
 * 차트 SDK가 소비자 디자인시스템을 강요하지 않음을 데모에서도 지킨다.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SampleApp() }
    }
}

private enum class Screen(val title: String) { LINE("라인"), BAR("스플릿 바"), ZONE("심박존") }

@Composable
private fun SampleApp() {
    var screen by remember { mutableStateOf(Screen.LINE) }
    // 배경을 시스템 테마에 맞춘다(Minor-3): 차트는 defaults(isSystemInDarkTheme())로 다크 팔레트를
    // 자동 전환하므로, 흰 배경 하드코딩이면 다크모드에서 밝은 차트가 흰 배경 위에 그려져 대비가 사라진다.
    val dark = isSystemInDarkTheme()
    val background = if (dark) Color(0xFF000000) else Color(0xFFFFFFFF)
    val labelText = if (dark) Color.White else Color.Black
    val unselectedChip = if (dark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
    Column(Modifier.fillMaxSize().background(background).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Screen.entries.forEach { s ->
                val selected = s == screen
                BasicText(
                    text = s.title,
                    modifier = Modifier
                        .clickable { screen = s }
                        .background(if (selected) Color(0xFF007AFF) else unselectedChip)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    style = TextStyle(color = if (selected) Color.White else labelText, fontSize = 14.sp),
                )
            }
        }
        when (screen) {
            Screen.LINE -> LineScreen()
            Screen.BAR -> BarScreen()
            Screen.ZONE -> ZoneScreen()
        }
    }
}

/** 데모 텍스트 색(시스템 테마 연동 — Minor-3 시연). */
private fun headingColor(dark: Boolean): Color = if (dark) Color.White else Color.Black
private fun subtitleColor(dark: Boolean): Color = if (dark) Color(0xFFAEAEB2) else Color(0xFF3C3C43)

@Composable
private fun LineScreen() {
    var readout by remember { mutableStateOf("스크럽(길게 누르거나 드래그)하여 값을 확인하세요") }
    val dark = isSystemInDarkTheme()
    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        BasicText("이번 러닝 · 페이스 vs 심박", style = TextStyle(fontSize = 17.sp, color = headingColor(dark)))
        BasicText(readout, style = TextStyle(fontSize = 13.sp, color = subtitleColor(dark)))
        RDLineChart(
            data = SampleData.lineChart,
            modifier = Modifier.fillMaxWidth().height(320.dp).padding(top = 12.dp),
            invertedAxes = setOf(Axis.PRIMARY), // 페이스: 위=빠름
            backgroundArea = SampleData.altitudeArea,
            labelFormatter = SampleData::format,
            isZoomEnabled = true,
            onScrub = { values ->
                val pace = values["pace"]?.let { "페이스 $it" } ?: ""
                val hr = values["hr"]?.let { "심박 $it" } ?: ""
                readout = listOf(pace, hr).filter { it.isNotEmpty() }.joinToString("  ")
            },
            onScrubBackground = { alt -> readout += "  고도 ${alt.roundToInt()}m" },
            onScrubEnd = { readout = "스크럽 종료" },
        )
    }
}

@Composable
private fun BarScreen() {
    val layout = remember { BarChartEngine.layout(SampleData.barData) }
    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        BasicText("구간 스플릿 페이스", style = TextStyle(fontSize = 17.sp, color = headingColor(isSystemInDarkTheme())))
        RDBarChart(
            layout = layout,
            modifier = Modifier.fillMaxWidth().height(280.dp).padding(top = 12.dp),
            xAxisLabels = List(layout.bars.size) { "${it + 1}km" },
            yLabelFormatter = { secondsPerKm -> SampleData.paceLabel(secondsPerKm / 60.0) },
        )
    }
}

@Composable
private fun ZoneScreen() {
    var selected by remember { mutableStateOf("존을 탭하세요") }
    val dark = isSystemInDarkTheme()
    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        BasicText("심박존 분포", style = TextStyle(fontSize = 17.sp, color = headingColor(dark)))
        BasicText(selected, style = TextStyle(fontSize = 13.sp, color = subtitleColor(dark)))
        RDHeartRateZoneChart(
            data = SampleData.donutData,
            modifier = Modifier.fillMaxWidth().height(280.dp).padding(top = 12.dp),
            onSelectSegment = { index ->
                selected = if (index == null) "선택 해제" else "존 ${index + 1} 선택"
            },
        )
    }
}

/** iOS 샘플과 동일한 시연 데이터. */
private object SampleData {
    private val paceValues = listOf(6.1, 5.9, 5.75, 5.6, 5.7, 5.5, 5.35, 5.45, 5.3, 5.2, 5.4)
    private val heartRateValues = listOf(148.0, 152.0, 157.0, 160.0, 163.0, 166.0, 168.0, 170.0, 172.0, 174.0, 171.0)
    private val ghostValues = listOf(6.4, 6.2, 6.15, 6.0, 6.05, 5.9, 5.8, 5.85, 5.7, 5.65, 5.75)

    private fun series(id: String, values: List<Double>, axis: Axis, role: SeriesRole) =
        Series(id, values.mapIndexed { i, v -> Point(i * 0.5, v) }, axis, role)

    val lineChart = LineChartData(
        series = listOf(
            series("pace", paceValues, Axis.PRIMARY, SeriesRole.MAIN),
            series("pace_prev", ghostValues, Axis.PRIMARY, SeriesRole.GHOST),
            series("hr", heartRateValues, Axis.SECONDARY, SeriesRole.MAIN),
        ),
        referenceBands = listOf(RefBand(5.4, 5.6, Axis.PRIMARY)),
        segmentMarkers = (1..5).map { Marker(it.toDouble(), "${it}km", it == 5) },
        config = ChartConfig(segmentCount = 5, maxTicks = 5),
    )

    val altitudeArea = listOf(
        Point(0.0, 12.0), Point(1.0, 28.0), Point(2.0, 20.0),
        Point(3.0, 45.0), Point(4.0, 38.0), Point(5.0, 60.0),
    )

    val barData = BarChartData(
        samples = listOf(
            SplitSample(1000.0, 330.0), SplitSample(1000.0, 322.0), SplitSample(1000.0, 315.0),
            SplitSample(1000.0, 335.0), SplitSample(1000.0, 308.0), SplitSample(600.0, 190.0),
        ),
        splitDistanceMeters = 1000.0,
        targetPaceSecPerUnit = 320.0,
        toleranceSecPerUnit = 8.0,
    )

    val donutData = DonutChartData(
        segments = listOf(
            DonutSegment(4.0, DonutColorRole.ZONE1),
            DonutSegment(12.0, DonutColorRole.ZONE2),
            DonutSegment(20.0, DonutColorRole.ZONE3),
            DonutSegment(9.0, DonutColorRole.ZONE4),
            DonutSegment(3.0, DonutColorRole.ZONE5),
        ),
    )

    fun format(axis: ChartAxis, value: Double): String = when (axis) {
        ChartAxis.Y_PRIMARY -> paceLabel(value)
        ChartAxis.Y_SECONDARY -> value.toInt().toString()
        else -> "%gkm".format(value)
    }

    /** 분 단위 페이스(예: 5.5) → mm'ss". */
    fun paceLabel(minutes: Double): String {
        val m = minutes.toInt()
        val s = ((minutes - m) * 60).roundToInt()
        return "%d'%02d\"".format(m, s)
    }
}
