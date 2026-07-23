package com.lumipol.graph.renderer

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.lumipol.graph.model.Axis
import com.lumipol.graph.model.AxisTick
import com.lumipol.graph.model.BarChartLayout
import com.lumipol.graph.model.BarColorRole
import com.lumipol.graph.model.BarLayout
import com.lumipol.graph.model.DonutChartData
import com.lumipol.graph.model.DonutColorRole
import com.lumipol.graph.model.DonutSegment
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 컴포저블·제스처 배선 검증(Robolectric + compose-ui-test, JVM·에뮬 불필요).
 *
 * 순수 로직은 [LineChartInteractionTest] 등에서 이미 닫혀 있으므로, 여기서는 순수 테스트로 닿지 않는
 * "pointerInput 배선"만 확인한다: (a) 배치2가 미룬 도넛 cancel→deselect, (b) 라인 드래그 스크럽 콜백.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
@OptIn(ExperimentalTestApi::class)
class ComposeInteractionTest {

    @get:Rule
    val rule = createComposeRule()

    private val donutData = DonutChartData(
        segments = listOf(
            DonutSegment(30.0, DonutColorRole.ZONE1),
            DonutSegment(40.0, DonutColorRole.ZONE3),
            DonutSegment(30.0, DonutColorRole.ZONE5),
        ),
    )

    @Test
    fun entranceProgressReplaysWhenToggledOnAfterFirstComposition() {
        // animateEntrance=false로 최초 컴포지션 후 true로 토글하면 0부터 재생되어야 한다 —
        // 키 없는 remember{Animatable(1f)} + animateTo(1f)는 no-op이라 애니가 영영 재생되지 않는다.
        rule.mainClock.autoAdvance = false
        var animate by mutableStateOf(false)
        var observed = -1f
        rule.setContent {
            val p by rememberEntranceProgress(trigger = "fixed", animate = animate, durationMs = 300)
            observed = p
        }
        rule.mainClock.advanceTimeByFrame()
        assertEquals(1f, observed, "animate=false면 즉시 완성(1f)")
        animate = true
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications() // 일시정지 클록에선 수동 전파
        rule.mainClock.advanceTimeByFrame() // 리컴포지션 + 이펙트 시작(0으로 스냅)
        rule.mainClock.advanceTimeByFrame() // 첫 애니 프레임
        assertTrue(observed < 0.9f, "토글 시 0부터 재생되어야 함 — observed=$observed")
        rule.mainClock.advanceTimeBy(1000)
        assertEquals(1f, observed, "재생 완료 후 1f")
    }

    @Test
    fun entranceProgressReplaysWhenTriggerChanges() {
        // trigger(bar layout/donut data) 교체 시 0부터 재생 — 정착값(1f)에서 animateTo(1f) no-op 금지.
        rule.mainClock.autoAdvance = false
        var trigger by mutableStateOf("a")
        var observed = -1f
        rule.setContent {
            val p by rememberEntranceProgress(trigger = trigger, animate = true, durationMs = 300)
            observed = p
        }
        rule.mainClock.advanceTimeBy(1000)
        assertEquals(1f, observed, "최초 재생 완료")
        trigger = "b"
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications() // 일시정지 클록에선 수동 전파
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        assertTrue(observed < 0.9f, "trigger 교체 시 0부터 재생되어야 함 — observed=$observed")
    }

    @Test
    fun donutTouchCancelSendsDeselect() {
        val events = mutableListOf<Int?>()
        rule.setContent {
            RDHeartRateZoneChart(
                data = donutData,
                modifier = Modifier.size(240.dp),
                onSelectSegment = { events.add(it) },
            )
        }
        // 링 위의 한 점(상단 12시 부근)을 눌렀다가 터치 취소(스크롤뷰 가로챔 시나리오) → 마지막은 해제(null).
        rule.onRoot().performTouchInput {
            down(Offset(width / 2f, height * 0.06f))
            cancel()
        }
        rule.waitForIdle()
        assertTrue(events.isNotEmpty(), "down 시 선택 통지가 있어야 함")
        assertEquals(null, events.last(), "취소는 선택 해제(null)를 통지")
    }

    @Test
    fun donutTapOnRingSelectsSegment() {
        val events = mutableListOf<Int?>()
        rule.setContent {
            RDHeartRateZoneChart(
                data = donutData,
                modifier = Modifier.size(240.dp),
                onSelectSegment = { events.add(it) },
            )
        }
        // 12시 부근 링 = 첫 세그먼트(startFraction 0). 다운 즉시 원본 인덱스 통지.
        rule.onRoot().performTouchInput {
            down(Offset(width / 2f, height * 0.06f))
            up()
        }
        rule.waitForIdle()
        assertEquals(0, events.first(), "12시 링 탭은 첫 세그먼트(원본 인덱스 0)")
        assertEquals(null, events.last(), "손 뗌 → 해제")
    }

    @Test
    fun donutTapInvokesLatestOnSelectSegmentAfterRecomposition() {
        // data는 그대로 두고 onSelectSegment 람다만 교체(리컴포지션) — pointerInput 키가 안 바뀌어도
        // 탭은 항상 최신 람다로 통지되어야 한다(낡은 클로저로 통지되면 stale 선택 상태 버그).
        val first = mutableListOf<Int?>()
        val second = mutableListOf<Int?>()
        var generation by mutableStateOf(0)
        rule.setContent {
            val t = if (generation == 0) first else second
            RDHeartRateZoneChart(
                data = donutData,
                modifier = Modifier.size(240.dp),
                onSelectSegment = { t.add(it) },
            )
        }
        rule.waitForIdle()
        generation = 1
        rule.waitForIdle()
        rule.onRoot().performTouchInput {
            down(Offset(width / 2f, height * 0.06f))
            up()
        }
        rule.waitForIdle()
        assertTrue(first.isEmpty(), "교체 전 람다로 통지되면 안 됨(stale 클로저)")
        assertEquals(0, second.firstOrNull(), "교체 후 람다가 탭 선택을 받아야 함")
    }

    private fun barLayout4() = BarChartLayout(
        bars = (0 until 4).map {
            BarLayout(index = it, value = 300.0 + it * 20, heightFraction = 0.3 + 0.1 * it,
                colorRole = BarColorRole.ON_TARGET, isPartial = false)
        },
        yTicks = listOf(AxisTick(300.0, 0.0), AxisTick(360.0, 1.0)),
        referenceLinePosition = null,
    )

    @Test
    fun barLongPressSelectsThenReleases() {
        val events = mutableListOf<Int?>()
        rule.setContent {
            RDBarChart(
                layout = barLayout4(),
                modifier = Modifier.size(width = 320.dp, height = 200.dp),
                barLabels = listOf("5'00\"", "5'10\"", "5'20\"", "5'30\""),
                onSelectedIndexChange = { events.add(it) },
            )
        }
        // 뷰 중앙 롱프레스(press→hold→release) → 인덱스 2 선택 후 해제.
        rule.onRoot().performTouchInput { longClick(Offset(width / 2f, height / 2f)) }
        rule.waitForIdle()
        assertEquals(2, events.first(), "중앙 롱프레스는 인덱스 2 선택")
        assertEquals(null, events.last(), "손 뗌 → 해제(null)")
        // setSelection의 no-op 가드(idx == selectedIndex → return)가 onDragStart+onDrag 중복 통지를
        // 억제해야 하므로 선택 1회 + 해제 1회 = 2건만 발화한다(스퓨리어스 중복 통지 방지 회귀 가드).
        assertEquals(2, events.size, "선택 1 + 해제 1만 통지(중복 억제)")
    }

    @Test
    fun barScrubDisabledWithoutLabels() {
        val events = mutableListOf<Int?>()
        rule.setContent {
            RDBarChart(
                layout = barLayout4(),
                modifier = Modifier.size(width = 320.dp, height = 200.dp),
                barLabels = null, // 값 소스 없음 → 스크럽 무효
                onSelectedIndexChange = { events.add(it) },
            )
        }
        rule.onRoot().performTouchInput { longClick(Offset(width / 2f, height / 2f)) }
        rule.waitForIdle()
        assertTrue(events.isEmpty(), "barLabels 없으면 선택 통지 없음")
    }

    @Test
    fun lineChartDragReportsScrubValues() {
        val scrubbed = mutableListOf<Map<String, String>>()
        var ended = false
        rule.setContent {
            RDLineChart(
                data = TestFixtures.paceAndHeartRate,
                modifier = Modifier.size(width = 360.dp, height = 280.dp),
                invertedAxes = setOf(Axis.PRIMARY),
                labelFormatter = TestFixtures::format,
                animateEntrance = false,
                onScrub = { scrubbed.add(it) },
                onScrubEnd = { ended = true },
            )
        }
        // 비확대 드래그 = 스크럽. 플롯 중앙 부근을 가로로 끈다.
        rule.onRoot().performTouchInput {
            down(Offset(width * 0.4f, height * 0.5f))
            moveBy(Offset(width * 0.2f, 0f))
            up()
        }
        rule.waitForIdle()
        assertFalse(scrubbed.isEmpty(), "드래그 스크럽은 값을 통지해야 함")
        assertTrue(scrubbed.last().containsKey("pace"), "페이스 값 포함")
        assertTrue(ended, "손 뗌 → 스크럽 종료 통지")
    }

    @Test
    fun dataUpdateDoesNotReplayEntranceAnimation() {
        // 라이브 스트리밍처럼 data 값이 갱신될 때 600ms 입장 애니가 0%부터 재생되면 라인이 매번
        // 사라졌다 다시 그려진다 — 등장 애니는 컴포저블 수명당 1회여야 한다. 재생 여부는
        // "data 갱신 → idle까지 가상 클록이 얼마나 흘러야 하는가"로 관찰한다(재생 시 ≥600ms).
        var gen by mutableStateOf(0)
        rule.setContent {
            RDLineChart(
                data = if (gen == 0) TestFixtures.paceOnly else TestFixtures.paceAndHeartRate,
                modifier = Modifier.size(200.dp),
                invertedAxes = setOf(Axis.PRIMARY),
                labelFormatter = TestFixtures::format,
                animateEntrance = true,
            )
        }
        rule.waitForIdle() // 최초 입장 애니 완료
        val t0 = rule.mainClock.currentTime
        gen = 1
        rule.waitForIdle()
        val elapsed = rule.mainClock.currentTime - t0
        assertTrue(
            elapsed < 300,
            "data 갱신이 입장 애니(600ms)를 재생하면 안 됨 — idle까지 ${elapsed}ms 소요",
        )
    }

    @Test
    fun dataChangeMidScrubStillNotifiesScrubEnd() {
        // 스크럽 진행 중(마커 표시, onScrub 발화 후) 데이터가 갱신되면 interaction·pointerInput이
        // 재시작되는데, 그때도 onScrub/onScrubEnd 짝 불변식이 지켜져야 한다(라이브 스트리밍 시나리오).
        val scrubbed = mutableListOf<Map<String, String>>()
        var ended = 0
        var gen by mutableStateOf(0)
        rule.setContent {
            RDLineChart(
                data = if (gen == 0) TestFixtures.paceAndHeartRate else TestFixtures.paceOnly,
                modifier = Modifier.size(width = 360.dp, height = 280.dp),
                invertedAxes = setOf(Axis.PRIMARY),
                labelFormatter = TestFixtures::format,
                animateEntrance = false,
                onScrub = { scrubbed.add(it) },
                onScrubEnd = { ended++ },
            )
        }
        // 가로 드래그로 스크럽 진입, 손가락은 뗀 상태가 아님.
        rule.onRoot().performTouchInput {
            down(Offset(width * 0.4f, height * 0.5f))
            moveBy(Offset(width * 0.2f, 0f))
        }
        rule.waitForIdle()
        assertFalse(scrubbed.isEmpty(), "스크럽이 시작되어야 함")
        assertEquals(0, ended, "손을 떼기 전엔 종료 통지가 없어야 함")
        // 스크럽 도중 데이터 갱신 → interaction 재생성 → 이전 스크럽은 종료 통지로 닫혀야 함.
        gen = 1
        rule.waitForIdle()
        assertEquals(1, ended, "데이터 갱신으로 스크럽이 끊기면 onScrubEnd가 발화해야 함(dangling onScrub 금지)")
    }

    @Test
    fun latePinchAfterLongPressTimeoutStillZooms() {
        // 두 번째 손가락이 롱프레스 타임아웃 뒤에 닿는 느린 핀치도 줌이어야 한다(iOS 독립
        // 핀치 인식기 parity). 줌 성립의 관찰 가능한 증거: 이후 가로 드래그가 스크럽이 아니라
        // 팬으로 라우팅되어 onScrub이 발화하지 않는다.
        val scrubbed = mutableListOf<Map<String, String>>()
        rule.setContent {
            RDLineChart(
                data = TestFixtures.paceAndHeartRate,
                modifier = Modifier.size(width = 360.dp, height = 280.dp),
                invertedAxes = setOf(Axis.PRIMARY),
                labelFormatter = TestFixtures::format,
                animateEntrance = false,
                isZoomEnabled = true,
                onScrub = { scrubbed.add(it) },
            )
        }
        // 1단계: 느린 핀치 — 첫 손가락, 롱프레스 타임아웃 경과, 두 번째 손가락, 벌리기.
        rule.onRoot().performTouchInput {
            down(0, Offset(width * 0.5f, height * 0.5f))
            advanceEventTime(800)
            move()
            down(1, Offset(width * 0.55f, height * 0.5f))
            repeat(6) {
                updatePointerTo(0, Offset(width * 0.5f - (it + 1) * 15f, height * 0.5f))
                updatePointerTo(1, Offset(width * 0.55f + (it + 1) * 15f, height * 0.5f))
                move()
            }
            up(0)
            up(1)
        }
        rule.waitForIdle()
        // 2단계: 가로 드래그 — 줌이 걸렸다면 팬이라 스크럽 무발화, 줌 실패면 스크럽 발화.
        scrubbed.clear()
        rule.onRoot().performTouchInput {
            down(Offset(width * 0.3f, height * 0.5f))
            moveBy(Offset(width * 0.3f, 0f))
            up()
        }
        rule.waitForIdle()
        assertTrue(scrubbed.isEmpty(), "느린 핀치 후 가로 드래그는 팬이어야 함(줌 성립) — 스크럽 발화는 줌 실패 의미")
    }

    @Test
    fun zoomedPanShiftsWindowByPlotWidthFraction() {
        // 줌 팬 비율의 분모는 플롯 폭(insets 제외)이어야 한다(iOS plotArea.rect.width parity).
        // 창 [2,3]에서 왼쪽으로 플롯 폭 1.5배 드래그 → 창 [3.5,4.5] → 중앙 탭 스크럽은 x=4.0
        // (pace 5.3 = "5'18\""). 캔버스 폭으로 나누면 1.13스팬만 이동해 x=3.5(5'27")가 잡힌다.
        val scrubbed = mutableListOf<Map<String, String>>()
        var densityV = 1f
        rule.setContent {
            densityV = androidx.compose.ui.platform.LocalDensity.current.density
            RDLineChart(
                data = TestFixtures.paceOnly,
                modifier = Modifier.size(width = 360.dp, height = 280.dp),
                invertedAxes = setOf(Axis.PRIMARY),
                labelFormatter = TestFixtures::format,
                animateEntrance = false,
                isZoomEnabled = true,
                zoomXRange = 2.0..3.0,
                onScrub = { scrubbed.add(it) },
            )
        }
        rule.waitForIdle()
        rule.onRoot().performTouchInput {
            val plotLeft = 44f * densityV
            val plotWidth = width - 88f * densityV
            down(Offset(width * 0.95f, height * 0.5f))
            moveBy(Offset(-40f * densityV, 0f)) // 슬롭 초과 → 팬 진입
            moveBy(Offset(-1.5f * plotWidth, 0f))
            up()
        }
        rule.waitForIdle()
        scrubbed.clear()
        // 플롯 중앙 탭 → 스크럽 값으로 창 위치를 관찰.
        rule.onRoot().performTouchInput {
            val plotLeft = 44f * densityV
            val plotWidth = width - 88f * densityV
            down(Offset(plotLeft + plotWidth / 2f, height * 0.5f))
            advanceEventTime(700) // 더블탭 대기 창 경과 → 단일탭 스크럽 확정
            up()
        }
        rule.waitForIdle()
        assertEquals("5'18\"", scrubbed.lastOrNull()?.get("pace"), "팬 비율이 플롯 폭 기준이면 창 중앙은 x=4.0")
    }

    @Test
    fun zoomControllerReappliesSameRangeAfterUserPansAway() {
        // 같은 구간을 다시 요청해도 매번 적용되어야 한다(iOS zoom(toXRange:) 재호출 parity):
        // [2,3] 줌 → 사용자가 팬으로 벗어남 → 같은 [2,3] 재요청 → 창이 [2,3]으로 복귀해야 한다.
        // 복귀 검증은 중앙 롱프레스 스크럽 값(x=2.5 → "5'30\"")으로 관찰.
        val scrubbed = mutableListOf<Map<String, String>>()
        val controller = LineChartZoomController()
        var densityV = 1f
        rule.setContent {
            densityV = androidx.compose.ui.platform.LocalDensity.current.density
            RDLineChart(
                data = TestFixtures.paceOnly,
                modifier = Modifier.size(width = 360.dp, height = 280.dp),
                invertedAxes = setOf(Axis.PRIMARY),
                labelFormatter = TestFixtures::format,
                animateEntrance = false,
                isZoomEnabled = true,
                zoomController = controller,
                onScrub = { scrubbed.add(it) },
            )
        }
        rule.waitForIdle()
        controller.zoomTo(2.0..3.0)
        rule.waitForIdle()
        // 사용자 팬으로 창을 벗어난다.
        rule.onRoot().performTouchInput {
            down(Offset(width * 0.95f, height * 0.5f))
            moveBy(Offset(-40f * densityV, 0f))
            moveBy(Offset(-(width - 88f * densityV), 0f))
            up()
        }
        rule.waitForIdle()
        // 같은 구간 재요청 → 복귀해야 한다.
        controller.zoomTo(2.0..3.0)
        rule.waitForIdle()
        scrubbed.clear()
        rule.onRoot().performTouchInput {
            val plotWidth = width - 88f * densityV
            down(Offset(44f * densityV + plotWidth / 2f, height * 0.5f))
            advanceEventTime(700)
            up()
        }
        rule.waitForIdle()
        assertEquals("5'30\"", scrubbed.lastOrNull()?.get("pace"), "같은 구간 재요청 후 창 중앙은 x=2.5여야 함")
    }

    @Test
    fun lineChartVerticalDragYieldsToParentScroll() {
        // Arch M1 / UX Major-2: 비확대 상태에서 세로 우세 드래그는 차트가 스크럽으로 트랩하지 않는다
        // (부모 스크롤에 양보). 따라서 세로 드래그로는 onScrub이 발화하지 않아야 한다.
        val scrubbed = mutableListOf<Map<String, String>>()
        rule.setContent {
            RDLineChart(
                data = TestFixtures.paceAndHeartRate,
                modifier = Modifier.size(width = 360.dp, height = 280.dp),
                invertedAxes = setOf(Axis.PRIMARY),
                labelFormatter = TestFixtures::format,
                animateEntrance = false,
                onScrub = { scrubbed.add(it) },
            )
        }
        // 플롯 중앙에서 순수 세로 스와이프(수직 우세).
        rule.onRoot().performTouchInput {
            down(Offset(width * 0.5f, height * 0.35f))
            moveBy(Offset(0f, height * 0.3f))
            up()
        }
        rule.waitForIdle()
        assertTrue(scrubbed.isEmpty(), "세로 우세 드래그는 스크럽하지 않고 부모 스크롤에 양보해야 함")
    }

    @Test
    fun markerControllerShowFiresScrubAndHideFiresEndOnce() {
        // iOS showTouchMarker(atX:)/hideTouchMarker() parity: show는 근접점 값 콜백 발화,
        // 같은 x 재요청도 매번 발화(봉투 항등성), hide는 표시 중이었을 때만 onScrubEnd 1회.
        val scrubbed = mutableListOf<Map<String, String>>()
        var ended = 0
        val controller = LineChartMarkerController()
        rule.setContent {
            RDLineChart(
                data = TestFixtures.paceOnly,
                modifier = Modifier.size(width = 360.dp, height = 280.dp),
                invertedAxes = setOf(Axis.PRIMARY),
                labelFormatter = TestFixtures::format,
                animateEntrance = false,
                markerController = controller,
                onScrub = { scrubbed.add(it) },
                onScrubEnd = { ended++ },
            )
        }
        rule.waitForIdle()
        controller.show(2.5)
        rule.waitForIdle()
        assertEquals("5'30\"", scrubbed.lastOrNull()?.get("pace"), "show(2.5)는 x=2.5 근접점 값을 통지해야 함")
        assertEquals(0, ended, "show만으로 종료 통지가 나가면 안 됨")
        controller.show(2.5) // 같은 x 재요청 — iOS 재호출과 동일하게 매번 발화.
        rule.waitForIdle()
        assertEquals(2, scrubbed.size, "같은 x 재요청도 콜백을 다시 발화해야 함")
        controller.hide()
        rule.waitForIdle()
        assertEquals(1, ended, "hide는 표시 중이던 마커에 onScrubEnd 1회")
        controller.hide() // 마커 없는 hide — 짝 깨진 종료 통지 금지.
        rule.waitForIdle()
        assertEquals(1, ended, "마커가 없으면 hide는 무통지여야 함")
    }

    @Test
    fun markerShowRequestIsNotReappliedAfterDataUpdate() {
        // QA r2 Minor: 데이터 갱신으로 interaction이 재생성돼도 이전 show 요청 봉투가 재적용되어
        // onScrub이 재발화되면 안 된다 — iOS render()는 마커를 **무통지** 제거하고 이전 show를
        // 기억하지 않는다(onScrubEnd도 미발화). 갱신 후 새 show는 정상 동작해야 한다.
        val scrubbed = mutableListOf<Map<String, String>>()
        var ended = 0
        var gen by mutableStateOf(0)
        val controller = LineChartMarkerController()
        rule.setContent {
            RDLineChart(
                data = if (gen == 0) TestFixtures.paceOnly else TestFixtures.paceAndHeartRate,
                modifier = Modifier.size(width = 360.dp, height = 280.dp),
                invertedAxes = setOf(Axis.PRIMARY),
                labelFormatter = TestFixtures::format,
                animateEntrance = false,
                markerController = controller,
                onScrub = { scrubbed.add(it) },
                onScrubEnd = { ended++ },
            )
        }
        rule.waitForIdle()
        controller.show(2.5)
        rule.waitForIdle()
        assertEquals(1, scrubbed.size, "show는 1회 통지")
        gen = 1 // 데이터 갱신 → interaction 재생성(마커 무통지 소멸).
        rule.waitForIdle()
        assertEquals(1, scrubbed.size, "데이터 갱신이 이전 show 요청을 재적용해 onScrub을 재발화하면 안 됨")
        assertEquals(0, ended, "마커 무통지 제거 — onScrubEnd도 미발화(iOS render() parity)")
        controller.show(1.0) // 갱신 후 새 요청은 정상 동작.
        rule.waitForIdle()
        assertEquals(2, scrubbed.size, "데이터 갱신 후 새 show 요청은 적용되어야 함")
    }

    @Test
    fun zoomRequestIsNotReappliedAfterDataUpdate() {
        // 명령형 줌 요청도 마커와 동일 규칙 — iOS render()는 zoomState를 초기화하고 이전
        // zoom(toXRange:)을 기억하지 않는다. 관찰: 갱신 후 중앙 탭이 전체 구간 중앙 x=2.5("5'30\"")
        // 값을 통지해야 한다(스테일 [3,4] 재적용 시 창 중앙 x=3.5 → "5'27\"").
        val scrubbed = mutableListOf<Map<String, String>>()
        var gen by mutableStateOf(0)
        val controller = LineChartZoomController()
        var densityV = 1f
        rule.setContent {
            densityV = androidx.compose.ui.platform.LocalDensity.current.density
            RDLineChart(
                data = if (gen == 0) TestFixtures.paceOnly else TestFixtures.paceAndHeartRate,
                modifier = Modifier.size(width = 360.dp, height = 280.dp),
                invertedAxes = setOf(Axis.PRIMARY),
                labelFormatter = TestFixtures::format,
                animateEntrance = false,
                isZoomEnabled = true,
                zoomController = controller,
                onScrub = { scrubbed.add(it) },
            )
        }
        rule.waitForIdle()
        controller.zoomTo(3.0..4.0)
        rule.waitForIdle()
        gen = 1 // 데이터 갱신 → 줌 초기화(iOS render() parity), 스테일 요청 재적용 금지.
        rule.waitForIdle()
        rule.onRoot().performTouchInput {
            val plotWidth = width - 88f * densityV
            down(Offset(44f * densityV + plotWidth / 2f, height * 0.5f))
            advanceEventTime(700) // 더블탭 대기 창 경과 → 단일탭 스크럽 확정
            up()
        }
        rule.waitForIdle()
        assertEquals("5'30\"", scrubbed.lastOrNull()?.get("pace"), "데이터 갱신 후 창은 전체 구간(중앙 x=2.5)이어야 함")
    }

    @Test
    fun markerControllerShowOutsideZoomWindowIsIgnored() {
        // iOS showTouchMarker parity: 확대 창 밖 x는 무시(마커·콜백 없음), 창 안 x는 표시.
        val scrubbed = mutableListOf<Map<String, String>>()
        val controller = LineChartMarkerController()
        rule.setContent {
            RDLineChart(
                data = TestFixtures.paceOnly,
                modifier = Modifier.size(width = 360.dp, height = 280.dp),
                invertedAxes = setOf(Axis.PRIMARY),
                labelFormatter = TestFixtures::format,
                animateEntrance = false,
                isZoomEnabled = true,
                zoomXRange = 2.0..3.0,
                markerController = controller,
                onScrub = { scrubbed.add(it) },
            )
        }
        rule.waitForIdle()
        controller.show(4.5) // 창 [2,3] 밖.
        rule.waitForIdle()
        assertTrue(scrubbed.isEmpty(), "확대 창 밖 show는 무시되어야 함")
        controller.show(3.0) // 창 상한(경계) — epsilon 클램프로 표시되어야 함.
        rule.waitForIdle()
        assertEquals("5'21\"", scrubbed.lastOrNull()?.get("pace"), "창 경계 show는 클램프되어 표시(x=3.0, pace 5.35)")
    }

    @Test
    fun zoomControllerResetRestoresFullWindow() {
        // iOS resetZoom parity: [3,4] 줌 후 reset() → 전체 구간 복귀. 복귀 검증은 플롯 중앙 탭
        // 스크럽 값으로 관찰 — 줌 유지 시 창 중앙 x=3.5("5'27\""), 복귀 시 전체 중앙 x=2.5("5'30\"").
        val scrubbed = mutableListOf<Map<String, String>>()
        val controller = LineChartZoomController()
        var densityV = 1f
        rule.setContent {
            densityV = androidx.compose.ui.platform.LocalDensity.current.density
            RDLineChart(
                data = TestFixtures.paceOnly,
                modifier = Modifier.size(width = 360.dp, height = 280.dp),
                invertedAxes = setOf(Axis.PRIMARY),
                labelFormatter = TestFixtures::format,
                animateEntrance = false,
                isZoomEnabled = true,
                zoomController = controller,
                onScrub = { scrubbed.add(it) },
            )
        }
        rule.waitForIdle()
        controller.zoomTo(3.0..4.0)
        rule.waitForIdle()
        controller.reset()
        rule.waitForIdle()
        rule.onRoot().performTouchInput {
            val plotWidth = width - 88f * densityV
            down(Offset(44f * densityV + plotWidth / 2f, height * 0.5f))
            advanceEventTime(700) // 더블탭 대기 창 경과 → 단일탭 스크럽 확정
            up()
        }
        rule.waitForIdle()
        assertEquals("5'30\"", scrubbed.lastOrNull()?.get("pace"), "reset 후 플롯 중앙은 전체 구간 중앙 x=2.5여야 함")
    }
}
