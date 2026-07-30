package com.lumipol.graph.model

import com.lumipol.graph.scale.AxisDomain

/**
 * 출력 전용 축 식별(X + 두 Y축 + 오버레이). Y_OVERLAY는 스크럽 포맷 태그이자
 * 0.40.0부터 고도 실루엣 눈금 채널 — Y_SECONDARY가 빌 때만 방출된다(렌더러는 오른쪽에 배치).
 */
enum class ChartAxis { X, Y_PRIMARY, Y_SECONDARY, Y_OVERLAY }

/**
 * 축 도메인 출력 — 렌더러가 tick 두 점에서 선형관계를 역산(구 AxisScale)하지 않도록
 * 코어가 계산에 쓴 도메인을 그대로 내보낸다(경계 정책 §4-1 "역산 금지").
 * 값이 없는 Y축은 null. 픽셀→도메인 변환은 [AxisDomain.denormalize]로 한다.
 */
data class ChartDomains(
    val x: AxisDomain,
    val yPrimary: AxisDomain?,
    val ySecondary: AxisDomain?,
)

data class NormalizedPoint(val x: Double, val y: Double) // 각 0.0~1.0

/**
 * 시리즈 1개의 정규화 출력. [axis]는 이 항목의 y 정규화에 쓴 축(B10) — 렌더러가 data에서
 * id→축 맵(첫 우선 규칙)을 재구성하지 않도록 코어가 항목별로 확정해 실어 보낸다.
 * 오버레이는 자체 정규화라 축 미사용(PRIMARY 고정).
 */
data class SeriesLayout(
    val id: String,
    val role: SeriesRole,
    val points: List<NormalizedPoint>,
    val axis: Axis,
) {
    // ObjC export 기본 인자 소실 대응 + 기존 직접 생성 호출부(테스트) 보존용.
    constructor(id: String, role: SeriesRole, points: List<NormalizedPoint>) :
        this(id, role, points, Axis.PRIMARY)
}

// position 0.0~1.0. 단 Y_OVERLAY(고도 눈금)의 position은 실루엣 밴드 내 fraction
// (0=플롯 바닥, 1=밴드 상단 = areaHeightFraction 높이) — 플롯 전체 0~1이 아니다.
data class AxisTick(val value: Double, val position: Double)
data class AxisTicksLayout(val axis: ChartAxis, val ticks: List<AxisTick>)

data class RefBandLayout(val axis: Axis, val lower: Double, val upper: Double)      // 0.0~1.0
data class MarkerLayout(val position: Double, val label: String?, val emphasis: Boolean) // X 위치 0.0~1.0

data class SeriesStat(val id: String, val min: Double, val max: Double, val avg: Double)
data class SegmentStat(val min: Double, val max: Double, val avg: Double, val count: Int)
data class Stats(val perSeries: List<SeriesStat>, val segments: List<SegmentStat>, val segmentSeriesId: String?)

data class LineChartLayout(
    val series: List<SeriesLayout>,
    val axisTicks: List<AxisTicksLayout>,
    val refBands: List<RefBandLayout>,
    val markers: List<MarkerLayout>,
    val stats: Stats,
    val domains: ChartDomains,
)

data class NearestResult(val seriesId: String, val x: Double, val y: Double)

/**
 * 스크럽 마커의 시리즈 1건 — 렌더러가 재탐색·정규화 재계산 없이 도트를 놓을 수 있는 완전한 출력
 * (경계 정책 §4-1 "역산 금지"의 스크럽 적용, B2).
 *
 * @property x,y 원본 도메인 근접점 — 콜백·포맷 값. 원본 포인트의 복사(보간 없음)라 동등성 T1.
 * @property nx 도트 x 위치(수직선과 동일, 0..1 클램프) — 모든 도트는 스냅 수직선 위에 놓인다.
 * @property ny 도트 y 위치. 축 시리즈는 축 도메인 정규화, 오버레이는 layout 자체 정규화 y
 *   (렌더러는 반전 무시 매핑을 써야 한다 — 라인과 동일 규칙). null이면 도트 생략(값만 전달) —
 *   오버레이가 layout에 없는 경우.
 * @property axis 도트 매핑 축. 오버레이는 PRIMARY(반전 무시 매핑이라 미사용).
 * @property chartAxis 포맷 축(Y_PRIMARY/Y_SECONDARY/Y_OVERLAY).
 */
data class ScrubPoint(
    val seriesId: String,
    val x: Double,
    val y: Double,
    val nx: Double,
    val ny: Double?,
    val role: SeriesRole,
    val axis: Axis,
    val chartAxis: ChartAxis,
)

/**
 * 스크럽(근접 질의) 결과 — 스냅 소스 선택(main 우선)·창 필터·정규화 좌표까지 코어가 확정한다.
 * 렌더러의 TouchMarker는 이 값을 플랫폼 좌표로 옮겨 그리기만 한다.
 *
 * @property snappedX 수직선 기준 원본 도메인 x(스냅 소스 근접점) — 배경 area 보간에 사용.
 * @property snappedNx 수직선 위치(0..1 클램프).
 * @property snapSourceId 스냅 소스 시리즈 id(main 우선, 없으면 첫 시리즈).
 */
data class ScrubResult(
    val snappedX: Double,
    val snappedNx: Double,
    val snapSourceId: String,
    val perSeries: List<ScrubPoint>,
)

/** 막대 1칸. value=스플릿 평균 페이스(sec/unit, 시간가중).
 *  heightFraction·position은 0.0~1.0, 반전 축 — 값이 작을수록(빠를수록) 크다.
 *  끝 위치 3종은 x축 라벨용이며 모드에 따라 채워지는 것이 다르다(0.41.0). */
data class BarLayout(
    val index: Int,
    val value: Double,
    val heightFraction: Double,
    val colorRole: BarColorRole,
    val isPartial: Boolean,
    // 시간모드 버킷 끝 표시용 정수 분 — 경과초/60 **절삭**·최소1(0.54.0, 반올림 아님).
    // 항상 endMinutes*60 <= endSeconds라 실측 라벨과 역전되지 않는다. 초까지 필요하면
    // endSeconds를 쓸 것(부분 버킷·sub-minute 축 라벨은 코어도 endSeconds를 쓴다). 거리모드 null
    val endMinutes: Int? = null,
    val endDistanceMeters: Double? = null, // 거리모드 버킷 끝 누적 거리(m, 부분은 총거리). 시간모드 null
    val endSeconds: Double? = null, // 시간모드 버킷 끝 누적 초(반올림 없음). 거리모드 null
)

/**
 * 막대 색 앵커 — 컬러맵의 기준 3값. 렌더러 2곳+소비 앱 2곳이 각자 재계산하던 축약
 * (docs/refactor/21-constants-diff.md §4, 4벌 복제·실사고 2회)의 단일 원본.
 * 규칙: 온전(비부분) 스플릿이 2개 이상이고 범위가 있으면 온전 스플릿만, 아니면 전체.
 * average는 런 총합 평균(있고 >0이면)을 우선하되 [fastest, slowest]로 클램프.
 */
data class BarColorAnchors(val fastest: Double, val slowest: Double, val average: Double)

data class BarChartLayout(
    val bars: List<BarLayout>,
    val yTicks: List<AxisTick>,
    val referenceLinePosition: Double?,
    val colorAnchors: BarColorAnchors?,
)

/**
 * 도넛 한 조각. fraction은 0.0~1.0(12시 0, 시계방향). value=원본 크기.
 * [sourceIndex]는 **원본 `DonutChartData.segments` 인덱스** — 엔진이 value<=0을 필터하므로
 * 레이아웃 인덱스와 어긋날 수 있고, 렌더러 히트테스트는 이 값을 그대로 보고해야 한다.
 */
data class DonutSegmentLayout(
    val startFraction: Double,
    val sweepFraction: Double,
    val value: Double,
    val colorRole: DonutColorRole,
    val sourceIndex: Int = -1,
    val label: String? = null,
)

/** total=세그먼트 value 합(0이면 무데이터). */
data class DonutChartLayout(
    val segments: List<DonutSegmentLayout>,
    val total: Double,
)

/**
 * 페이스 전처리 결과 — 정제된 라인 포인트 + 통계 + 고도 실루엣.
 *
 * 네 시리즈 필드는 [availableSeries]와 항상 일치한다: 지표가 가용하지 않으면 필드도
 * 비어 있고(고도는 null), 가용하면 비어 있지 않다. 둘 중 어느 쪽을 봐도 같은 답이 나온다.
 */
data class PaceSeriesResult(
    val pace: List<Point>,          // y = paceSeconds/60(분), 아웃라이어 컷·평활·다운샘플. 미가용이면 emptyList
    val heart: List<Point>,         // 전 포인트, 결측 승계(앞쪽 결측은 첫 유효값 소급). 미가용이면 emptyList
    val cadence: List<Point>,       // 전 포인트, 결측 승계(앞쪽 결측은 첫 유효값 소급). 미가용이면 emptyList
    val altitudeArea: List<Point>?, // 다운샘플. 고도 미측정이거나 <2점이면 null. 평지여도 측정됐으면 반환
    val bestPaceSeconds: Double,    // 평활·다운샘플된(표시되는) 페이스의 최소(초), 없으면 0 — 선과 일치. 미가용이어도 낸다
    val validPaceCount: Int,        // 필터·아웃라이어 통과 표본 수(다운샘플 이전). 위와 동일
    /**
     * 실제로 표시 가능한 지표 id 집합([com.lumipol.graph.PaceSeriesId]). 소비 앱의 지표 칩·범례
     * 노출과 [com.lumipol.graph.SeriesSelection] 입력의 단일 소스 — 앱이 개별 필드를 보고
     * 각자 판정하면 플랫폼마다 규칙이 갈리므로 코어가 확정해 내보낸다.
     */
    val availableSeries: Set<Int>,
)

/** 존 표시용 bpm 경계. upper=null이면 상한 없음(최대존). */
data class ZoneBpmRange(val lower: Int, val upper: Int?)
