#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class LumipolGraphBarChartEngine, LumipolGraphBarChartLayout, LumipolGraphBarChartData, LumipolGraphChartA11y, LumipolGraphDonutChartLayout, LumipolGraphChartDefaults, LumipolGraphChartDefaultsDarkPalette, LumipolGraphChartDefaultsLightPalette, LumipolGraphChartFormat, LumipolGraphDonutEngine, LumipolGraphDonutChartData, LumipolGraphKotlinEnumCompanion, LumipolGraphKotlinEnum<E>, LumipolGraphGender, LumipolGraphKotlinArray<T>, LumipolGraphHeartRateZoneEngine, LumipolGraphHeartRateZoneSample, LumipolGraphZoneBpmRange, LumipolGraphLineChartEngine, LumipolGraphPoint, LumipolGraphLineChartLayout, LumipolGraphLineChartData, LumipolGraphNearestResult, LumipolGraphScrubResult, LumipolGraphPaceColormap, LumipolGraphBarColorAnchors, LumipolGraphPaceSeriesEngine, LumipolGraphPaceSeriesResult, LumipolGraphPaceSeriesInput, LumipolGraphPaceSeriesId, LumipolGraphSeriesSelection, LumipolGraphAxis, LumipolGraphTrackChartBuilder, LumipolGraphRawTrackSample, LumipolGraphRunTotals, LumipolGraphBuildOptions, LumipolGraphSplitSample, LumipolGraphZoomWindow, LumipolGraphAxisTick, LumipolGraphChartAxis, LumipolGraphAxisTicksLayout, LumipolGraphBarLayout, LumipolGraphBarColorRole, LumipolGraphDistanceUnit, LumipolGraphXMode, LumipolGraphChartConfigCompanion, LumipolGraphChartConfig, LumipolGraphAxisDomain, LumipolGraphChartDomains, LumipolGraphDistanceUnitCompanion, LumipolGraphDonutSegment, LumipolGraphDonutSegmentLayout, LumipolGraphDonutColorRole, LumipolGraphSeries, LumipolGraphRefBand, LumipolGraphMarker, LumipolGraphSeriesLayout, LumipolGraphRefBandLayout, LumipolGraphMarkerLayout, LumipolGraphStats, LumipolGraphNormalizedPoint, LumipolGraphPaceSamplePoint, LumipolGraphRawTrackSampleCompanion, LumipolGraphSeriesRole, LumipolGraphScrubPoint, LumipolGraphSegmentStat, LumipolGraphSeriesStat, LumipolGraphNiceScale;

@protocol LumipolGraphKotlinComparable, LumipolGraphKotlinIterator;

NS_ASSUME_NONNULL_BEGIN
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunknown-warning-option"
#pragma clang diagnostic ignored "-Wincompatible-property-type"
#pragma clang diagnostic ignored "-Wnullability"

#pragma push_macro("_Nullable_result")
#if !__has_feature(nullability_nullable_result)
#undef _Nullable_result
#define _Nullable_result _Nullable
#endif

__attribute__((swift_name("KotlinBase")))
@interface LumipolGraphBase : NSObject
- (instancetype)init __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
+ (void)initialize __attribute__((objc_requires_super));
@end

@interface LumipolGraphBase (LumipolGraphBaseCopying) <NSCopying>
@end

__attribute__((swift_name("KotlinMutableSet")))
@interface LumipolGraphMutableSet<ObjectType> : NSMutableSet<ObjectType>
@end

__attribute__((swift_name("KotlinMutableDictionary")))
@interface LumipolGraphMutableDictionary<KeyType, ObjectType> : NSMutableDictionary<KeyType, ObjectType>
@end

@interface NSError (NSErrorLumipolGraphKotlinException)
@property (readonly) id _Nullable kotlinException;
@end

__attribute__((swift_name("KotlinNumber")))
@interface LumipolGraphNumber : NSNumber
- (instancetype)initWithChar:(char)value __attribute__((unavailable));
- (instancetype)initWithUnsignedChar:(unsigned char)value __attribute__((unavailable));
- (instancetype)initWithShort:(short)value __attribute__((unavailable));
- (instancetype)initWithUnsignedShort:(unsigned short)value __attribute__((unavailable));
- (instancetype)initWithInt:(int)value __attribute__((unavailable));
- (instancetype)initWithUnsignedInt:(unsigned int)value __attribute__((unavailable));
- (instancetype)initWithLong:(long)value __attribute__((unavailable));
- (instancetype)initWithUnsignedLong:(unsigned long)value __attribute__((unavailable));
- (instancetype)initWithLongLong:(long long)value __attribute__((unavailable));
- (instancetype)initWithUnsignedLongLong:(unsigned long long)value __attribute__((unavailable));
- (instancetype)initWithFloat:(float)value __attribute__((unavailable));
- (instancetype)initWithDouble:(double)value __attribute__((unavailable));
- (instancetype)initWithBool:(BOOL)value __attribute__((unavailable));
- (instancetype)initWithInteger:(NSInteger)value __attribute__((unavailable));
- (instancetype)initWithUnsignedInteger:(NSUInteger)value __attribute__((unavailable));
+ (instancetype)numberWithChar:(char)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedChar:(unsigned char)value __attribute__((unavailable));
+ (instancetype)numberWithShort:(short)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedShort:(unsigned short)value __attribute__((unavailable));
+ (instancetype)numberWithInt:(int)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedInt:(unsigned int)value __attribute__((unavailable));
+ (instancetype)numberWithLong:(long)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedLong:(unsigned long)value __attribute__((unavailable));
+ (instancetype)numberWithLongLong:(long long)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedLongLong:(unsigned long long)value __attribute__((unavailable));
+ (instancetype)numberWithFloat:(float)value __attribute__((unavailable));
+ (instancetype)numberWithDouble:(double)value __attribute__((unavailable));
+ (instancetype)numberWithBool:(BOOL)value __attribute__((unavailable));
+ (instancetype)numberWithInteger:(NSInteger)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedInteger:(NSUInteger)value __attribute__((unavailable));
@end

__attribute__((swift_name("KotlinByte")))
@interface LumipolGraphByte : LumipolGraphNumber
- (instancetype)initWithChar:(char)value;
+ (instancetype)numberWithChar:(char)value;
@end

__attribute__((swift_name("KotlinUByte")))
@interface LumipolGraphUByte : LumipolGraphNumber
- (instancetype)initWithUnsignedChar:(unsigned char)value;
+ (instancetype)numberWithUnsignedChar:(unsigned char)value;
@end

__attribute__((swift_name("KotlinShort")))
@interface LumipolGraphShort : LumipolGraphNumber
- (instancetype)initWithShort:(short)value;
+ (instancetype)numberWithShort:(short)value;
@end

__attribute__((swift_name("KotlinUShort")))
@interface LumipolGraphUShort : LumipolGraphNumber
- (instancetype)initWithUnsignedShort:(unsigned short)value;
+ (instancetype)numberWithUnsignedShort:(unsigned short)value;
@end

__attribute__((swift_name("KotlinInt")))
@interface LumipolGraphInt : LumipolGraphNumber
- (instancetype)initWithInt:(int)value;
+ (instancetype)numberWithInt:(int)value;
@end

__attribute__((swift_name("KotlinUInt")))
@interface LumipolGraphUInt : LumipolGraphNumber
- (instancetype)initWithUnsignedInt:(unsigned int)value;
+ (instancetype)numberWithUnsignedInt:(unsigned int)value;
@end

__attribute__((swift_name("KotlinLong")))
@interface LumipolGraphLong : LumipolGraphNumber
- (instancetype)initWithLongLong:(long long)value;
+ (instancetype)numberWithLongLong:(long long)value;
@end

__attribute__((swift_name("KotlinULong")))
@interface LumipolGraphULong : LumipolGraphNumber
- (instancetype)initWithUnsignedLongLong:(unsigned long long)value;
+ (instancetype)numberWithUnsignedLongLong:(unsigned long long)value;
@end

__attribute__((swift_name("KotlinFloat")))
@interface LumipolGraphFloat : LumipolGraphNumber
- (instancetype)initWithFloat:(float)value;
+ (instancetype)numberWithFloat:(float)value;
@end

__attribute__((swift_name("KotlinDouble")))
@interface LumipolGraphDouble : LumipolGraphNumber
- (instancetype)initWithDouble:(double)value;
+ (instancetype)numberWithDouble:(double)value;
@end

__attribute__((swift_name("KotlinBoolean")))
@interface LumipolGraphBoolean : LumipolGraphNumber
- (instancetype)initWithBool:(BOOL)value;
+ (instancetype)numberWithBool:(BOOL)value;
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BarChartEngine")))
@interface LumipolGraphBarChartEngine : LumipolGraphBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)barChartEngine __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LumipolGraphBarChartEngine *shared __attribute__((swift_name("shared")));
- (double)chooseDistanceBucketMetersTotalDistanceMeters:(double)totalDistanceMeters unitMeters:(double)unitMeters __attribute__((swift_name("chooseDistanceBucketMeters(totalDistanceMeters:unitMeters:)")));
- (double)chooseTimeBucketSecondsRunningSeconds:(double)runningSeconds __attribute__((swift_name("chooseTimeBucketSeconds(runningSeconds:)")));
- (LumipolGraphBarChartLayout *)layoutData:(LumipolGraphBarChartData *)data __attribute__((swift_name("layout(data:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChartA11y")))
@interface LumipolGraphChartA11y : LumipolGraphBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)chartA11y __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LumipolGraphChartA11y *shared __attribute__((swift_name("shared")));
- (NSString *)barChartBarCount:(int32_t)barCount barLabels:(NSArray<NSString *> *)barLabels __attribute__((swift_name("barChart(barCount:barLabels:)")));
- (NSString *)donutLayout:(LumipolGraphDonutChartLayout *)layout __attribute__((swift_name("donut(layout:)")));
- (NSString *)donutSelectionLabel:(NSString * _Nullable)label sweepFraction:(double)sweepFraction __attribute__((swift_name("donutSelection(label:sweepFraction:)")));
- (NSString *)lineChartSeriesCount:(int32_t)seriesCount hasBackgroundArea:(BOOL)hasBackgroundArea __attribute__((swift_name("lineChart(seriesCount:hasBackgroundArea:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChartDefaults")))
@interface LumipolGraphChartDefaults : LumipolGraphBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)chartDefaults __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LumipolGraphChartDefaults *shared __attribute__((swift_name("shared")));
@property (readonly) double AREA_FILL_ALPHA __attribute__((swift_name("AREA_FILL_ALPHA")));
@property (readonly) double AREA_HEIGHT_FRACTION __attribute__((swift_name("AREA_HEIGHT_FRACTION")));
@property (readonly) double AREA_MIN_VALUE_SPAN __attribute__((swift_name("AREA_MIN_VALUE_SPAN")));
@property (readonly) double AXIS_LABEL_FONT_SIZE __attribute__((swift_name("AXIS_LABEL_FONT_SIZE")));
@property (readonly) double AXIS_LABEL_GAP __attribute__((swift_name("AXIS_LABEL_GAP")));
@property (readonly) double BAR_CALLOUT_FONT_SIZE __attribute__((swift_name("BAR_CALLOUT_FONT_SIZE")));
@property (readonly) double BAR_CORNER_RADIUS __attribute__((swift_name("BAR_CORNER_RADIUS")));
@property (readonly) double BAR_DIM_OPACITY __attribute__((swift_name("BAR_DIM_OPACITY")));
@property (readonly) double BAR_GROWTH_DURATION_SECONDS __attribute__((swift_name("BAR_GROWTH_DURATION_SECONDS")));
@property (readonly) double BAR_LABEL_GAP __attribute__((swift_name("BAR_LABEL_GAP")));
@property (readonly) double BAR_LABEL_MIN_GAP __attribute__((swift_name("BAR_LABEL_MIN_GAP")));
@property (readonly) double BAR_MIN_HEIGHT __attribute__((swift_name("BAR_MIN_HEIGHT")));
@property (readonly) double BAR_REFERENCE_LINE_ALPHA __attribute__((swift_name("BAR_REFERENCE_LINE_ALPHA")));
@property (readonly) double BAR_SELECTION_LINE_ALPHA __attribute__((swift_name("BAR_SELECTION_LINE_ALPHA")));
@property (readonly) double BAR_WIDTH_RATIO __attribute__((swift_name("BAR_WIDTH_RATIO")));
@property (readonly) double BAR_X_LABEL_GAP __attribute__((swift_name("BAR_X_LABEL_GAP")));
@property (readonly) double CALLOUT_CORNER_RADIUS __attribute__((swift_name("CALLOUT_CORNER_RADIUS")));
@property (readonly) double CALLOUT_PAD_H __attribute__((swift_name("CALLOUT_PAD_H")));
@property (readonly) double CALLOUT_PAD_V __attribute__((swift_name("CALLOUT_PAD_V")));
@property (readonly) double DONUT_AUTO_DESELECT_SECONDS __attribute__((swift_name("DONUT_AUTO_DESELECT_SECONDS")));
@property (readonly) double DONUT_CENTER_LABEL_FONT_SIZE __attribute__((swift_name("DONUT_CENTER_LABEL_FONT_SIZE")));
@property (readonly) double DONUT_CENTER_PERCENT_FONT_SIZE __attribute__((swift_name("DONUT_CENTER_PERCENT_FONT_SIZE")));
@property (readonly) double DONUT_CENTER_WIDTH_RATIO __attribute__((swift_name("DONUT_CENTER_WIDTH_RATIO")));
@property (readonly) double DONUT_DIMMED_ALPHA __attribute__((swift_name("DONUT_DIMMED_ALPHA")));
@property (readonly) double DONUT_EMPTY_ALPHA __attribute__((swift_name("DONUT_EMPTY_ALPHA")));
@property (readonly) double DONUT_RING_WIDTH __attribute__((swift_name("DONUT_RING_WIDTH")));
@property (readonly) double DONUT_START_DEGREES __attribute__((swift_name("DONUT_START_DEGREES")));
@property (readonly) double DONUT_SWEEP_DURATION_SECONDS __attribute__((swift_name("DONUT_SWEEP_DURATION_SECONDS")));
@property (readonly) double DONUT_ZONE2_ALPHA __attribute__((swift_name("DONUT_ZONE2_ALPHA")));
@property (readonly) double ENTRANCE_DURATION_SECONDS __attribute__((swift_name("ENTRANCE_DURATION_SECONDS")));
@property (readonly) double ENTRANCE_EASING_X1 __attribute__((swift_name("ENTRANCE_EASING_X1")));
@property (readonly) double ENTRANCE_EASING_X2 __attribute__((swift_name("ENTRANCE_EASING_X2")));
@property (readonly) double ENTRANCE_EASING_Y1 __attribute__((swift_name("ENTRANCE_EASING_Y1")));
@property (readonly) double ENTRANCE_EASING_Y2 __attribute__((swift_name("ENTRANCE_EASING_Y2")));
@property (readonly) BOOL ENTRANCE_ENABLED_DEFAULT __attribute__((swift_name("ENTRANCE_ENABLED_DEFAULT")));
@property (readonly) int64_t FALLBACK_DATA_COLOR __attribute__((swift_name("FALLBACK_DATA_COLOR")));
@property (readonly) double GRADIENT_MAX_ALPHA __attribute__((swift_name("GRADIENT_MAX_ALPHA")));
@property (readonly) double GRID_DASH_OFF __attribute__((swift_name("GRID_DASH_OFF")));
@property (readonly) double GRID_DASH_ON __attribute__((swift_name("GRID_DASH_ON")));
@property (readonly) double GRID_LINE_ALPHA __attribute__((swift_name("GRID_LINE_ALPHA")));
@property (readonly) double GRID_LINE_WIDTH __attribute__((swift_name("GRID_LINE_WIDTH")));
@property (readonly) double LABEL_GAP __attribute__((swift_name("LABEL_GAP")));
@property (readonly) double LINE_WIDTH __attribute__((swift_name("LINE_WIDTH")));
@property (readonly) double MARKER_EMPHASIS_LINE_WIDTH __attribute__((swift_name("MARKER_EMPHASIS_LINE_WIDTH")));
@property (readonly) double MARKER_LINE_WIDTH __attribute__((swift_name("MARKER_LINE_WIDTH")));
@property (readonly) double MAX_ZOOM_SCALE __attribute__((swift_name("MAX_ZOOM_SCALE")));
@property (readonly) double OVERLAY_LINE_ALPHA __attribute__((swift_name("OVERLAY_LINE_ALPHA")));
@property (readonly) double OVERLAY_LINE_WIDTH __attribute__((swift_name("OVERLAY_LINE_WIDTH")));
@property (readonly) double PARTIAL_BAR_ALPHA __attribute__((swift_name("PARTIAL_BAR_ALPHA")));
@property (readonly) double PLOT_INSET_BOTTOM __attribute__((swift_name("PLOT_INSET_BOTTOM")));
@property (readonly) double PLOT_INSET_LEFT __attribute__((swift_name("PLOT_INSET_LEFT")));
@property (readonly) double PLOT_INSET_RIGHT __attribute__((swift_name("PLOT_INSET_RIGHT")));
@property (readonly) double PLOT_INSET_TOP __attribute__((swift_name("PLOT_INSET_TOP")));
@property (readonly) double REF_BAND_ALPHA __attribute__((swift_name("REF_BAND_ALPHA")));
@property (readonly) double REF_DASH_OFF __attribute__((swift_name("REF_DASH_OFF")));
@property (readonly) double REF_DASH_ON __attribute__((swift_name("REF_DASH_ON")));
@property (readonly) double SECONDARY_LABEL_ALPHA __attribute__((swift_name("SECONDARY_LABEL_ALPHA")));
@property (readonly) double TOUCH_DOT_RADIUS __attribute__((swift_name("TOUCH_DOT_RADIUS")));
@property (readonly) double TOUCH_LINE_WIDTH __attribute__((swift_name("TOUCH_LINE_WIDTH")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChartDefaults.DarkPalette")))
@interface LumipolGraphChartDefaultsDarkPalette : LumipolGraphBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)darkPalette __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LumipolGraphChartDefaultsDarkPalette *shared __attribute__((swift_name("shared")));
@property (readonly) int64_t AREA_FILL __attribute__((swift_name("AREA_FILL")));
@property (readonly) int64_t AXIS_LABEL __attribute__((swift_name("AXIS_LABEL")));
@property (readonly) int64_t BAR_CALLOUT_BACKGROUND __attribute__((swift_name("BAR_CALLOUT_BACKGROUND")));
@property (readonly) int64_t BAR_CALLOUT_TEXT __attribute__((swift_name("BAR_CALLOUT_TEXT")));
@property (readonly) int64_t BAR_REFERENCE_LINE __attribute__((swift_name("BAR_REFERENCE_LINE")));
@property (readonly) int64_t BAR_SELECTION_LINE __attribute__((swift_name("BAR_SELECTION_LINE")));
@property (readonly) int64_t DONUT_CENTER_LABEL __attribute__((swift_name("DONUT_CENTER_LABEL")));
@property (readonly) int64_t DONUT_CENTER_PERCENT __attribute__((swift_name("DONUT_CENTER_PERCENT")));
@property (readonly) int64_t DONUT_EMPTY __attribute__((swift_name("DONUT_EMPTY")));
@property (readonly) int64_t DONUT_ZONE1 __attribute__((swift_name("DONUT_ZONE1")));
@property (readonly) int64_t DONUT_ZONE2 __attribute__((swift_name("DONUT_ZONE2")));
@property (readonly) int64_t DONUT_ZONE3 __attribute__((swift_name("DONUT_ZONE3")));
@property (readonly) int64_t DONUT_ZONE4 __attribute__((swift_name("DONUT_ZONE4")));
@property (readonly) int64_t DONUT_ZONE5 __attribute__((swift_name("DONUT_ZONE5")));
@property (readonly) int64_t GRID_LINE __attribute__((swift_name("GRID_LINE")));
@property (readonly) int64_t MARKER_EMPHASIS_LINE __attribute__((swift_name("MARKER_EMPHASIS_LINE")));
@property (readonly) int64_t MARKER_LINE __attribute__((swift_name("MARKER_LINE")));
@property (readonly) int64_t OVERLAY_LINE __attribute__((swift_name("OVERLAY_LINE")));
@property (readonly) int64_t PRIMARY_LINE __attribute__((swift_name("PRIMARY_LINE")));
@property (readonly) int64_t REF_BAND __attribute__((swift_name("REF_BAND")));
@property (readonly) int64_t SECONDARY_LINE __attribute__((swift_name("SECONDARY_LINE")));
@property (readonly) int64_t TOUCH_LINE __attribute__((swift_name("TOUCH_LINE")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChartDefaults.LightPalette")))
@interface LumipolGraphChartDefaultsLightPalette : LumipolGraphBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)lightPalette __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LumipolGraphChartDefaultsLightPalette *shared __attribute__((swift_name("shared")));
@property (readonly) int64_t AREA_FILL __attribute__((swift_name("AREA_FILL")));
@property (readonly) int64_t AXIS_LABEL __attribute__((swift_name("AXIS_LABEL")));
@property (readonly) int64_t BAR_CALLOUT_BACKGROUND __attribute__((swift_name("BAR_CALLOUT_BACKGROUND")));
@property (readonly) int64_t BAR_CALLOUT_TEXT __attribute__((swift_name("BAR_CALLOUT_TEXT")));
@property (readonly) int64_t BAR_REFERENCE_LINE __attribute__((swift_name("BAR_REFERENCE_LINE")));
@property (readonly) int64_t BAR_SELECTION_LINE __attribute__((swift_name("BAR_SELECTION_LINE")));
@property (readonly) int64_t DONUT_CENTER_LABEL __attribute__((swift_name("DONUT_CENTER_LABEL")));
@property (readonly) int64_t DONUT_CENTER_PERCENT __attribute__((swift_name("DONUT_CENTER_PERCENT")));
@property (readonly) int64_t DONUT_EMPTY __attribute__((swift_name("DONUT_EMPTY")));
@property (readonly) int64_t DONUT_ZONE1 __attribute__((swift_name("DONUT_ZONE1")));
@property (readonly) int64_t DONUT_ZONE2 __attribute__((swift_name("DONUT_ZONE2")));
@property (readonly) int64_t DONUT_ZONE3 __attribute__((swift_name("DONUT_ZONE3")));
@property (readonly) int64_t DONUT_ZONE4 __attribute__((swift_name("DONUT_ZONE4")));
@property (readonly) int64_t DONUT_ZONE5 __attribute__((swift_name("DONUT_ZONE5")));
@property (readonly) int64_t GRID_LINE __attribute__((swift_name("GRID_LINE")));
@property (readonly) int64_t MARKER_EMPHASIS_LINE __attribute__((swift_name("MARKER_EMPHASIS_LINE")));
@property (readonly) int64_t MARKER_LINE __attribute__((swift_name("MARKER_LINE")));
@property (readonly) int64_t OVERLAY_LINE __attribute__((swift_name("OVERLAY_LINE")));
@property (readonly) int64_t PRIMARY_LINE __attribute__((swift_name("PRIMARY_LINE")));
@property (readonly) int64_t REF_BAND __attribute__((swift_name("REF_BAND")));
@property (readonly) int64_t SECONDARY_LINE __attribute__((swift_name("SECONDARY_LINE")));
@property (readonly) int64_t TOUCH_LINE __attribute__((swift_name("TOUCH_LINE")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChartFormat")))
@interface LumipolGraphChartFormat : LumipolGraphBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)chartFormat __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LumipolGraphChartFormat *shared __attribute__((swift_name("shared")));
- (NSString *)distanceTickValue:(double)value __attribute__((swift_name("distanceTick(value:)")));
- (NSString *)durationSeconds:(double)seconds __attribute__((swift_name("duration(seconds:)")));
- (NSString *)intTickValue:(double)value __attribute__((swift_name("intTick(value:)")));
- (NSString *)paceSeconds:(double)seconds __attribute__((swift_name("pace(seconds:)")));
- (NSString *)paceInvalid __attribute__((swift_name("paceInvalid()")));
- (NSString *)percentFraction:(double)fraction __attribute__((swift_name("percent(fraction:)")));
- (NSString *)timeTickMinutes:(double)minutes __attribute__((swift_name("timeTick(minutes:)")));
@property (readonly) double PACE_MAX_SECONDS __attribute__((swift_name("PACE_MAX_SECONDS")));
@property (readonly) double PACE_MIN_SECONDS __attribute__((swift_name("PACE_MIN_SECONDS")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DonutEngine")))
@interface LumipolGraphDonutEngine : LumipolGraphBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)donutEngine __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LumipolGraphDonutEngine *shared __attribute__((swift_name("shared")));
- (LumipolGraphInt * _Nullable)hitTestDxRatio:(double)dxRatio dyRatio:(double)dyRatio hitBandRatio:(double)hitBandRatio layout:(LumipolGraphDonutChartLayout *)layout __attribute__((swift_name("hitTest(dxRatio:dyRatio:hitBandRatio:layout:)")));
- (LumipolGraphDonutChartLayout *)layoutData:(LumipolGraphDonutChartData *)data __attribute__((swift_name("layout(data:)")));
- (LumipolGraphInt * _Nullable)toggleSelectionCurrent:(LumipolGraphInt * _Nullable)current tapped:(LumipolGraphInt * _Nullable)tapped __attribute__((swift_name("toggleSelection(current:tapped:)")));
@property (readonly) double MIN_HIT_TARGET_DP __attribute__((swift_name("MIN_HIT_TARGET_DP")));
@end

__attribute__((swift_name("KotlinComparable")))
@protocol LumipolGraphKotlinComparable
@required
- (int32_t)compareToOther:(id _Nullable)other __attribute__((swift_name("compareTo(other:)")));
@end

__attribute__((swift_name("KotlinEnum")))
@interface LumipolGraphKotlinEnum<E> : LumipolGraphBase <LumipolGraphKotlinComparable>
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) LumipolGraphKotlinEnumCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(E)other __attribute__((swift_name("compareTo(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) int32_t ordinal __attribute__((swift_name("ordinal")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Gender")))
@interface LumipolGraphGender : LumipolGraphKotlinEnum<LumipolGraphGender *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) LumipolGraphGender *male __attribute__((swift_name("male")));
@property (class, readonly) LumipolGraphGender *female __attribute__((swift_name("female")));
@property (class, readonly) LumipolGraphGender *unknown __attribute__((swift_name("unknown")));
+ (LumipolGraphKotlinArray<LumipolGraphGender *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<LumipolGraphGender *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("HeartRateZoneEngine")))
@interface LumipolGraphHeartRateZoneEngine : LumipolGraphBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)heartRateZoneEngine __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LumipolGraphHeartRateZoneEngine *shared __attribute__((swift_name("shared")));
- (NSArray<LumipolGraphDouble *> *)calculateSamples:(NSArray<LumipolGraphHeartRateZoneSample *> *)samples maxHeartRate:(int32_t)maxHeartRate __attribute__((swift_name("calculate(samples:maxHeartRate:)")));
- (LumipolGraphDonutChartData * _Nullable)donutDataZoneSeconds:(NSArray<LumipolGraphDouble *> *)zoneSeconds labels:(NSArray<NSString *> *)labels __attribute__((swift_name("donutData(zoneSeconds:labels:)")));
- (int32_t)maxHeartRateAge:(int32_t)age gender:(LumipolGraphGender *)gender __attribute__((swift_name("maxHeartRate(age:gender:)")));
- (NSArray<LumipolGraphZoneBpmRange *> *)zoneBpmRangesMaxHeartRate:(int32_t)maxHeartRate __attribute__((swift_name("zoneBpmRanges(maxHeartRate:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LineChartEngine")))
@interface LumipolGraphLineChartEngine : LumipolGraphBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)lineChartEngine __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LumipolGraphLineChartEngine *shared __attribute__((swift_name("shared")));
- (LumipolGraphDouble * _Nullable)interpolatedYPoints:(NSArray<LumipolGraphPoint *> *)points x:(double)x __attribute__((swift_name("interpolatedY(points:x:)")));
- (LumipolGraphLineChartLayout *)layoutData:(LumipolGraphLineChartData *)data __attribute__((swift_name("layout(data:)")));
- (LumipolGraphLineChartLayout *)layoutData:(LumipolGraphLineChartData *)data backgroundArea:(NSArray<LumipolGraphPoint *> * _Nullable)backgroundArea __attribute__((swift_name("layout(data:backgroundArea:)")));
- (LumipolGraphLineChartLayout *)layoutData:(LumipolGraphLineChartData *)data xMin:(double)xMin xMax:(double)xMax __attribute__((swift_name("layout(data:xMin:xMax:)")));
- (LumipolGraphLineChartLayout *)layoutData:(LumipolGraphLineChartData *)data xMin:(double)xMin xMax:(double)xMax backgroundArea:(NSArray<LumipolGraphPoint *> * _Nullable)backgroundArea __attribute__((swift_name("layout(data:xMin:xMax:backgroundArea:)")));
- (NSArray<LumipolGraphNearestResult *> *)nearestData:(LumipolGraphLineChartData *)data x:(double)x __attribute__((swift_name("nearest(data:x:)"))) __attribute__((deprecated("스냅 소스 선택·창 필터·정규화 좌표를 렌더러가 재구성해야 한다 — 코어가 확정하는 nearestScrub로 대체(B2)")));
- (NSArray<LumipolGraphNearestResult *> *)nearestData:(LumipolGraphLineChartData *)data x:(double)x xMin:(double)xMin xMax:(double)xMax __attribute__((swift_name("nearest(data:x:xMin:xMax:)"))) __attribute__((deprecated("스냅 소스 선택·창 필터·정규화 좌표를 렌더러가 재구성해야 한다 — 코어가 확정하는 nearestScrub로 대체(B2)")));
- (LumipolGraphScrubResult * _Nullable)nearestScrubData:(LumipolGraphLineChartData *)data layout:(LumipolGraphLineChartLayout *)layout x:(double)x __attribute__((swift_name("nearestScrub(data:layout:x:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PaceColormap")))
@interface LumipolGraphPaceColormap : LumipolGraphBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)paceColormap __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LumipolGraphPaceColormap *shared __attribute__((swift_name("shared")));
- (NSArray<LumipolGraphLong *> *)legendStopsAnchors:(LumipolGraphBarColorAnchors *)anchors count:(int32_t)count colorBlind:(BOOL)colorBlind __attribute__((swift_name("legendStops(anchors:count:colorBlind:)")));
- (int64_t)rgbaValue:(double)value anchors:(LumipolGraphBarColorAnchors *)anchors colorBlind:(BOOL)colorBlind __attribute__((swift_name("rgba(value:anchors:colorBlind:)")));
@property (readonly) int64_t COLOR_BLIND_BLUE __attribute__((swift_name("COLOR_BLIND_BLUE")));
@property (readonly) int64_t COLOR_BLIND_GREEN __attribute__((swift_name("COLOR_BLIND_GREEN")));
@property (readonly) int64_t COLOR_BLIND_RED __attribute__((swift_name("COLOR_BLIND_RED")));
@property (readonly) int64_t COLOR_BLIND_YELLOW __attribute__((swift_name("COLOR_BLIND_YELLOW")));
@property (readonly) int32_t LEGEND_STOP_COUNT __attribute__((swift_name("LEGEND_STOP_COUNT")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PaceSeriesEngine")))
@interface LumipolGraphPaceSeriesEngine : LumipolGraphBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)paceSeriesEngine __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LumipolGraphPaceSeriesEngine *shared __attribute__((swift_name("shared")));
- (LumipolGraphPaceSeriesResult *)preprocessInput:(LumipolGraphPaceSeriesInput *)input __attribute__((swift_name("preprocess(input:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PaceSeriesId")))
@interface LumipolGraphPaceSeriesId : LumipolGraphBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)paceSeriesId __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LumipolGraphPaceSeriesId *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t ALTITUDE __attribute__((swift_name("ALTITUDE")));
@property (readonly) int32_t CADENCE __attribute__((swift_name("CADENCE")));
@property (readonly) NSArray<LumipolGraphInt *> *DISPLAY_PRIORITY __attribute__((swift_name("DISPLAY_PRIORITY")));
@property (readonly) int32_t HEART __attribute__((swift_name("HEART")));
@property (readonly) NSArray<LumipolGraphInt *> *LINE_PRIORITY __attribute__((swift_name("LINE_PRIORITY")));
@property (readonly) int32_t PACE __attribute__((swift_name("PACE")));
@property (readonly) NSSet<LumipolGraphInt *> *SHARED_SCALE_IDS __attribute__((swift_name("SHARED_SCALE_IDS")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SeriesSelection")))
@interface LumipolGraphSeriesSelection : LumipolGraphBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)seriesSelection __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LumipolGraphSeriesSelection *shared __attribute__((swift_name("shared")));
- (NSArray<LumipolGraphInt *> *)assignSlotsPriority:(NSArray<LumipolGraphInt *> *)priority selected:(NSSet<LumipolGraphInt *> *)selected withData:(NSSet<LumipolGraphInt *> *)withData __attribute__((swift_name("assignSlots(priority:selected:withData:)")));
- (NSSet<LumipolGraphAxis *> *)invertedAxesForPaceSlot:(int32_t)paceSlot __attribute__((swift_name("invertedAxesFor(paceSlot:)"))) __attribute__((deprecated("slotAxis 기반 매핑 — slotAxes 결과를 받는 오버로드로 대체(0.40.0)")));
- (NSSet<LumipolGraphAxis *> *)invertedAxesForPaceSlot:(int32_t)paceSlot axes:(NSArray<LumipolGraphAxis *> *)axes __attribute__((swift_name("invertedAxesFor(paceSlot:axes:)")));
- (NSArray<LumipolGraphInt *> *)normalizedCurrent:(NSArray<LumipolGraphInt *> *)current available:(NSSet<LumipolGraphInt *> *)available priority:(NSArray<LumipolGraphInt *> *)priority __attribute__((swift_name("normalized(current:available:priority:)")));
- (NSArray<LumipolGraphAxis *> *)slotAxesSlots:(NSArray<LumipolGraphInt *> *)slots sharedScaleIds:(NSSet<LumipolGraphInt *> *)sharedScaleIds __attribute__((swift_name("slotAxes(slots:sharedScaleIds:)")));
- (LumipolGraphAxis *)slotAxisIndex:(int32_t)index __attribute__((swift_name("slotAxis(index:)"))) __attribute__((deprecated("슬롯 index만으로는 스케일 그룹 병합(심박+케이던스 한 축)을 표현할 수 없다 — slotAxes로 대체(0.40.0)")));
- (NSArray<LumipolGraphInt *> *)toggledCurrent:(NSArray<LumipolGraphInt *> *)current toggling:(int32_t)toggling __attribute__((swift_name("toggled(current:toggling:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TrackChartBuilder")))
@interface LumipolGraphTrackChartBuilder : LumipolGraphBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)trackChartBuilder __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LumipolGraphTrackChartBuilder *shared __attribute__((swift_name("shared")));
- (LumipolGraphPaceSeriesInput *)paceInputSamples:(NSArray<LumipolGraphRawTrackSample *> *)samples totals:(LumipolGraphRunTotals *)totals options:(LumipolGraphBuildOptions *)options __attribute__((swift_name("paceInput(samples:totals:options:)")));
- (NSArray<LumipolGraphSplitSample *> *)splitSamplesSamples:(NSArray<LumipolGraphRawTrackSample *> *)samples __attribute__((swift_name("splitSamples(samples:)")));
- (NSArray<LumipolGraphHeartRateZoneSample *> *)zoneSamplesSamples:(NSArray<LumipolGraphRawTrackSample *> *)samples __attribute__((swift_name("zoneSamples(samples:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ZoomWindow")))
@interface LumipolGraphZoomWindow : LumipolGraphBase
- (instancetype)initWithFullMin:(double)fullMin fullMax:(double)fullMax __attribute__((swift_name("init(fullMin:fullMax:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithFullMin:(double)fullMin fullMax:(double)fullMax windowMin:(double)windowMin windowMax:(double)windowMax __attribute__((swift_name("init(fullMin:fullMax:windowMin:windowMax:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphZoomWindow *)doCopyFullMin:(double)fullMin fullMax:(double)fullMax windowMin:(double)windowMin windowMax:(double)windowMax __attribute__((swift_name("doCopy(fullMin:fullMax:windowMin:windowMax:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (LumipolGraphZoomWindow *)panStartMin:(double)startMin startMax:(double)startMax fraction:(double)fraction __attribute__((swift_name("pan(startMin:startMax:fraction:)")));
- (LumipolGraphZoomWindow *)pinchStartMin:(double)startMin startMax:(double)startMax cumulativeScale:(double)cumulativeScale anchor:(double)anchor maxScale:(double)maxScale __attribute__((swift_name("pinch(startMin:startMax:cumulativeScale:anchor:maxScale:)")));
- (LumipolGraphZoomWindow *)reset __attribute__((swift_name("reset()")));
- (LumipolGraphZoomWindow *)setWindowTargetMin:(double)targetMin targetMax:(double)targetMax __attribute__((swift_name("setWindow(targetMin:targetMax:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) double fullMax __attribute__((swift_name("fullMax")));
@property (readonly) double fullMin __attribute__((swift_name("fullMin")));
@property (readonly) BOOL isZoomed __attribute__((swift_name("isZoomed")));
@property (readonly) double scale __attribute__((swift_name("scale")));
@property (readonly) double windowMax __attribute__((swift_name("windowMax")));
@property (readonly) double windowMin __attribute__((swift_name("windowMin")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Axis")))
@interface LumipolGraphAxis : LumipolGraphKotlinEnum<LumipolGraphAxis *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) LumipolGraphAxis *primary __attribute__((swift_name("primary")));
@property (class, readonly) LumipolGraphAxis *secondary __attribute__((swift_name("secondary")));
+ (LumipolGraphKotlinArray<LumipolGraphAxis *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<LumipolGraphAxis *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AxisTick")))
@interface LumipolGraphAxisTick : LumipolGraphBase
- (instancetype)initWithValue:(double)value position:(double)position __attribute__((swift_name("init(value:position:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphAxisTick *)doCopyValue:(double)value position:(double)position __attribute__((swift_name("doCopy(value:position:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) double position __attribute__((swift_name("position")));
@property (readonly) double value __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AxisTicksLayout")))
@interface LumipolGraphAxisTicksLayout : LumipolGraphBase
- (instancetype)initWithAxis:(LumipolGraphChartAxis *)axis ticks:(NSArray<LumipolGraphAxisTick *> *)ticks __attribute__((swift_name("init(axis:ticks:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphAxisTicksLayout *)doCopyAxis:(LumipolGraphChartAxis *)axis ticks:(NSArray<LumipolGraphAxisTick *> *)ticks __attribute__((swift_name("doCopy(axis:ticks:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) LumipolGraphChartAxis *axis __attribute__((swift_name("axis")));
@property (readonly) NSArray<LumipolGraphAxisTick *> *ticks __attribute__((swift_name("ticks")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BarChartData")))
@interface LumipolGraphBarChartData : LumipolGraphBase
- (instancetype)initWithSamples:(NSArray<LumipolGraphSplitSample *> *)samples splitDistanceMeters:(double)splitDistanceMeters __attribute__((swift_name("init(samples:splitDistanceMeters:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithSamples:(NSArray<LumipolGraphSplitSample *> *)samples splitDistanceMeters:(double)splitDistanceMeters targetPaceSecPerUnit:(LumipolGraphDouble * _Nullable)targetPaceSecPerUnit toleranceSecPerUnit:(double)toleranceSecPerUnit __attribute__((swift_name("init(samples:splitDistanceMeters:targetPaceSecPerUnit:toleranceSecPerUnit:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithSamples:(NSArray<LumipolGraphSplitSample *> *)samples splitDistanceMeters:(double)splitDistanceMeters targetPaceSecPerUnit:(LumipolGraphDouble * _Nullable)targetPaceSecPerUnit toleranceSecPerUnit:(double)toleranceSecPerUnit maxTicks:(int32_t)maxTicks splitTimeSeconds:(LumipolGraphDouble * _Nullable)splitTimeSeconds totalDurationSeconds:(LumipolGraphDouble * _Nullable)totalDurationSeconds totalDistanceMeters:(LumipolGraphDouble * _Nullable)totalDistanceMeters __attribute__((swift_name("init(samples:splitDistanceMeters:targetPaceSecPerUnit:toleranceSecPerUnit:maxTicks:splitTimeSeconds:totalDurationSeconds:totalDistanceMeters:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphBarChartData *)doCopySamples:(NSArray<LumipolGraphSplitSample *> *)samples splitDistanceMeters:(double)splitDistanceMeters targetPaceSecPerUnit:(LumipolGraphDouble * _Nullable)targetPaceSecPerUnit toleranceSecPerUnit:(double)toleranceSecPerUnit maxTicks:(int32_t)maxTicks splitTimeSeconds:(LumipolGraphDouble * _Nullable)splitTimeSeconds totalDurationSeconds:(LumipolGraphDouble * _Nullable)totalDurationSeconds totalDistanceMeters:(LumipolGraphDouble * _Nullable)totalDistanceMeters __attribute__((swift_name("doCopy(samples:splitDistanceMeters:targetPaceSecPerUnit:toleranceSecPerUnit:maxTicks:splitTimeSeconds:totalDurationSeconds:totalDistanceMeters:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t maxTicks __attribute__((swift_name("maxTicks")));
@property (readonly) NSArray<LumipolGraphSplitSample *> *samples __attribute__((swift_name("samples")));
@property (readonly) double splitDistanceMeters __attribute__((swift_name("splitDistanceMeters")));
@property (readonly) LumipolGraphDouble * _Nullable splitTimeSeconds __attribute__((swift_name("splitTimeSeconds")));
@property (readonly) LumipolGraphDouble * _Nullable targetPaceSecPerUnit __attribute__((swift_name("targetPaceSecPerUnit")));
@property (readonly) double toleranceSecPerUnit __attribute__((swift_name("toleranceSecPerUnit")));
@property (readonly) LumipolGraphDouble * _Nullable totalDistanceMeters __attribute__((swift_name("totalDistanceMeters")));
@property (readonly) LumipolGraphDouble * _Nullable totalDurationSeconds __attribute__((swift_name("totalDurationSeconds")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BarChartLayout")))
@interface LumipolGraphBarChartLayout : LumipolGraphBase
- (instancetype)initWithBars:(NSArray<LumipolGraphBarLayout *> *)bars yTicks:(NSArray<LumipolGraphAxisTick *> *)yTicks referenceLinePosition:(LumipolGraphDouble * _Nullable)referenceLinePosition colorAnchors:(LumipolGraphBarColorAnchors * _Nullable)colorAnchors __attribute__((swift_name("init(bars:yTicks:referenceLinePosition:colorAnchors:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphBarChartLayout *)doCopyBars:(NSArray<LumipolGraphBarLayout *> *)bars yTicks:(NSArray<LumipolGraphAxisTick *> *)yTicks referenceLinePosition:(LumipolGraphDouble * _Nullable)referenceLinePosition colorAnchors:(LumipolGraphBarColorAnchors * _Nullable)colorAnchors __attribute__((swift_name("doCopy(bars:yTicks:referenceLinePosition:colorAnchors:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<LumipolGraphBarLayout *> *bars __attribute__((swift_name("bars")));
@property (readonly) LumipolGraphBarColorAnchors * _Nullable colorAnchors __attribute__((swift_name("colorAnchors")));
@property (readonly) LumipolGraphDouble * _Nullable referenceLinePosition __attribute__((swift_name("referenceLinePosition")));
@property (readonly) NSArray<LumipolGraphAxisTick *> *yTicks __attribute__((swift_name("yTicks")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BarColorAnchors")))
@interface LumipolGraphBarColorAnchors : LumipolGraphBase
- (instancetype)initWithFastest:(double)fastest slowest:(double)slowest average:(double)average __attribute__((swift_name("init(fastest:slowest:average:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphBarColorAnchors *)doCopyFastest:(double)fastest slowest:(double)slowest average:(double)average __attribute__((swift_name("doCopy(fastest:slowest:average:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) double average __attribute__((swift_name("average")));
@property (readonly) double fastest __attribute__((swift_name("fastest")));
@property (readonly) double slowest __attribute__((swift_name("slowest")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BarColorRole")))
@interface LumipolGraphBarColorRole : LumipolGraphKotlinEnum<LumipolGraphBarColorRole *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) LumipolGraphBarColorRole *faster __attribute__((swift_name("faster")));
@property (class, readonly) LumipolGraphBarColorRole *onTarget __attribute__((swift_name("onTarget")));
@property (class, readonly) LumipolGraphBarColorRole *slower __attribute__((swift_name("slower")));
+ (LumipolGraphKotlinArray<LumipolGraphBarColorRole *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<LumipolGraphBarColorRole *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BarLayout")))
@interface LumipolGraphBarLayout : LumipolGraphBase
- (instancetype)initWithIndex:(int32_t)index value:(double)value heightFraction:(double)heightFraction colorRole:(LumipolGraphBarColorRole *)colorRole isPartial:(BOOL)isPartial endMinutes:(LumipolGraphInt * _Nullable)endMinutes endDistanceMeters:(LumipolGraphDouble * _Nullable)endDistanceMeters endSeconds:(LumipolGraphDouble * _Nullable)endSeconds __attribute__((swift_name("init(index:value:heightFraction:colorRole:isPartial:endMinutes:endDistanceMeters:endSeconds:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphBarLayout *)doCopyIndex:(int32_t)index value:(double)value heightFraction:(double)heightFraction colorRole:(LumipolGraphBarColorRole *)colorRole isPartial:(BOOL)isPartial endMinutes:(LumipolGraphInt * _Nullable)endMinutes endDistanceMeters:(LumipolGraphDouble * _Nullable)endDistanceMeters endSeconds:(LumipolGraphDouble * _Nullable)endSeconds __attribute__((swift_name("doCopy(index:value:heightFraction:colorRole:isPartial:endMinutes:endDistanceMeters:endSeconds:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) LumipolGraphBarColorRole *colorRole __attribute__((swift_name("colorRole")));
@property (readonly) LumipolGraphDouble * _Nullable endDistanceMeters __attribute__((swift_name("endDistanceMeters")));
@property (readonly) LumipolGraphInt * _Nullable endMinutes __attribute__((swift_name("endMinutes")));
@property (readonly) LumipolGraphDouble * _Nullable endSeconds __attribute__((swift_name("endSeconds")));
@property (readonly) double heightFraction __attribute__((swift_name("heightFraction")));
@property (readonly) int32_t index __attribute__((swift_name("index")));
@property (readonly) BOOL isPartial __attribute__((swift_name("isPartial")));
@property (readonly) double value __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BuildOptions")))
@interface LumipolGraphBuildOptions : LumipolGraphBase
- (instancetype)initWithUnit:(LumipolGraphDistanceUnit *)unit xMode:(LumipolGraphXMode *)xMode __attribute__((swift_name("init(unit:xMode:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithUnit:(LumipolGraphDistanceUnit *)unit xMode:(LumipolGraphXMode *)xMode useWatchSpeed:(BOOL)useWatchSpeed __attribute__((swift_name("init(unit:xMode:useWatchSpeed:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphBuildOptions *)doCopyUnit:(LumipolGraphDistanceUnit *)unit xMode:(LumipolGraphXMode *)xMode useWatchSpeed:(BOOL)useWatchSpeed __attribute__((swift_name("doCopy(unit:xMode:useWatchSpeed:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) LumipolGraphDistanceUnit *unit __attribute__((swift_name("unit")));
@property (readonly) BOOL useWatchSpeed __attribute__((swift_name("useWatchSpeed")));
@property (readonly) LumipolGraphXMode *xMode __attribute__((swift_name("xMode")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChartAxis")))
@interface LumipolGraphChartAxis : LumipolGraphKotlinEnum<LumipolGraphChartAxis *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) LumipolGraphChartAxis *x __attribute__((swift_name("x")));
@property (class, readonly) LumipolGraphChartAxis *yPrimary __attribute__((swift_name("yPrimary")));
@property (class, readonly) LumipolGraphChartAxis *ySecondary __attribute__((swift_name("ySecondary")));
@property (class, readonly) LumipolGraphChartAxis *yOverlay __attribute__((swift_name("yOverlay")));
+ (LumipolGraphKotlinArray<LumipolGraphChartAxis *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<LumipolGraphChartAxis *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChartConfig")))
@interface LumipolGraphChartConfig : LumipolGraphBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithSegmentCount:(int32_t)segmentCount maxTicks:(int32_t)maxTicks __attribute__((swift_name("init(segmentCount:maxTicks:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) LumipolGraphChartConfigCompanion *companion __attribute__((swift_name("companion")));
- (LumipolGraphChartConfig *)doCopySegmentCount:(int32_t)segmentCount maxTicks:(int32_t)maxTicks __attribute__((swift_name("doCopy(segmentCount:maxTicks:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t maxTicks __attribute__((swift_name("maxTicks")));
@property (readonly) int32_t segmentCount __attribute__((swift_name("segmentCount")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChartConfig.Companion")))
@interface LumipolGraphChartConfigCompanion : LumipolGraphBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LumipolGraphChartConfigCompanion *shared __attribute__((swift_name("shared")));
- (int32_t)segmentCountForTotalDistanceUnits:(double)totalDistanceUnits xMode:(LumipolGraphXMode *)xMode __attribute__((swift_name("segmentCountFor(totalDistanceUnits:xMode:)")));
@property (readonly) int32_t MAX_SEGMENT_COUNT __attribute__((swift_name("MAX_SEGMENT_COUNT")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChartDomains")))
@interface LumipolGraphChartDomains : LumipolGraphBase
- (instancetype)initWithX:(LumipolGraphAxisDomain *)x yPrimary:(LumipolGraphAxisDomain * _Nullable)yPrimary ySecondary:(LumipolGraphAxisDomain * _Nullable)ySecondary __attribute__((swift_name("init(x:yPrimary:ySecondary:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphChartDomains *)doCopyX:(LumipolGraphAxisDomain *)x yPrimary:(LumipolGraphAxisDomain * _Nullable)yPrimary ySecondary:(LumipolGraphAxisDomain * _Nullable)ySecondary __attribute__((swift_name("doCopy(x:yPrimary:ySecondary:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) LumipolGraphAxisDomain *x __attribute__((swift_name("x")));
@property (readonly) LumipolGraphAxisDomain * _Nullable yPrimary __attribute__((swift_name("yPrimary")));
@property (readonly) LumipolGraphAxisDomain * _Nullable ySecondary __attribute__((swift_name("ySecondary")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DistanceUnit")))
@interface LumipolGraphDistanceUnit : LumipolGraphKotlinEnum<LumipolGraphDistanceUnit *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) LumipolGraphDistanceUnitCompanion *companion __attribute__((swift_name("companion")));
@property (class, readonly) LumipolGraphDistanceUnit *kilometers __attribute__((swift_name("kilometers")));
@property (class, readonly) LumipolGraphDistanceUnit *miles __attribute__((swift_name("miles")));
+ (LumipolGraphKotlinArray<LumipolGraphDistanceUnit *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<LumipolGraphDistanceUnit *> *entries __attribute__((swift_name("entries")));
@property (readonly) double unitMeters __attribute__((swift_name("unitMeters")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DistanceUnit.Companion")))
@interface LumipolGraphDistanceUnitCompanion : LumipolGraphBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LumipolGraphDistanceUnitCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) double METERS_PER_KM __attribute__((swift_name("METERS_PER_KM")));
@property (readonly) double METERS_PER_MILE __attribute__((swift_name("METERS_PER_MILE")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DonutChartData")))
@interface LumipolGraphDonutChartData : LumipolGraphBase
- (instancetype)initWithSegments:(NSArray<LumipolGraphDonutSegment *> *)segments __attribute__((swift_name("init(segments:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphDonutChartData *)doCopySegments:(NSArray<LumipolGraphDonutSegment *> *)segments __attribute__((swift_name("doCopy(segments:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<LumipolGraphDonutSegment *> *segments __attribute__((swift_name("segments")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DonutChartLayout")))
@interface LumipolGraphDonutChartLayout : LumipolGraphBase
- (instancetype)initWithSegments:(NSArray<LumipolGraphDonutSegmentLayout *> *)segments total:(double)total __attribute__((swift_name("init(segments:total:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphDonutChartLayout *)doCopySegments:(NSArray<LumipolGraphDonutSegmentLayout *> *)segments total:(double)total __attribute__((swift_name("doCopy(segments:total:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<LumipolGraphDonutSegmentLayout *> *segments __attribute__((swift_name("segments")));
@property (readonly) double total __attribute__((swift_name("total")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DonutColorRole")))
@interface LumipolGraphDonutColorRole : LumipolGraphKotlinEnum<LumipolGraphDonutColorRole *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) LumipolGraphDonutColorRole *zone1 __attribute__((swift_name("zone1")));
@property (class, readonly) LumipolGraphDonutColorRole *zone2 __attribute__((swift_name("zone2")));
@property (class, readonly) LumipolGraphDonutColorRole *zone3 __attribute__((swift_name("zone3")));
@property (class, readonly) LumipolGraphDonutColorRole *zone4 __attribute__((swift_name("zone4")));
@property (class, readonly) LumipolGraphDonutColorRole *zone5 __attribute__((swift_name("zone5")));
+ (LumipolGraphKotlinArray<LumipolGraphDonutColorRole *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<LumipolGraphDonutColorRole *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DonutSegment")))
@interface LumipolGraphDonutSegment : LumipolGraphBase
- (instancetype)initWithValue:(double)value colorRole:(LumipolGraphDonutColorRole *)colorRole __attribute__((swift_name("init(value:colorRole:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithValue:(double)value colorRole:(LumipolGraphDonutColorRole *)colorRole label:(NSString * _Nullable)label __attribute__((swift_name("init(value:colorRole:label:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphDonutSegment *)doCopyValue:(double)value colorRole:(LumipolGraphDonutColorRole *)colorRole label:(NSString * _Nullable)label __attribute__((swift_name("doCopy(value:colorRole:label:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) LumipolGraphDonutColorRole *colorRole __attribute__((swift_name("colorRole")));
@property (readonly) NSString * _Nullable label __attribute__((swift_name("label")));
@property (readonly) double value __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DonutSegmentLayout")))
@interface LumipolGraphDonutSegmentLayout : LumipolGraphBase
- (instancetype)initWithStartFraction:(double)startFraction sweepFraction:(double)sweepFraction value:(double)value colorRole:(LumipolGraphDonutColorRole *)colorRole sourceIndex:(int32_t)sourceIndex label:(NSString * _Nullable)label __attribute__((swift_name("init(startFraction:sweepFraction:value:colorRole:sourceIndex:label:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphDonutSegmentLayout *)doCopyStartFraction:(double)startFraction sweepFraction:(double)sweepFraction value:(double)value colorRole:(LumipolGraphDonutColorRole *)colorRole sourceIndex:(int32_t)sourceIndex label:(NSString * _Nullable)label __attribute__((swift_name("doCopy(startFraction:sweepFraction:value:colorRole:sourceIndex:label:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) LumipolGraphDonutColorRole *colorRole __attribute__((swift_name("colorRole")));
@property (readonly) NSString * _Nullable label __attribute__((swift_name("label")));
@property (readonly) int32_t sourceIndex __attribute__((swift_name("sourceIndex")));
@property (readonly) double startFraction __attribute__((swift_name("startFraction")));
@property (readonly) double sweepFraction __attribute__((swift_name("sweepFraction")));
@property (readonly) double value __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("HeartRateZoneSample")))
@interface LumipolGraphHeartRateZoneSample : LumipolGraphBase
- (instancetype)initWithHeartRate:(double)heartRate timeInterval:(double)timeInterval __attribute__((swift_name("init(heartRate:timeInterval:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphHeartRateZoneSample *)doCopyHeartRate:(double)heartRate timeInterval:(double)timeInterval __attribute__((swift_name("doCopy(heartRate:timeInterval:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) double heartRate __attribute__((swift_name("heartRate")));
@property (readonly) double timeInterval __attribute__((swift_name("timeInterval")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LineChartData")))
@interface LumipolGraphLineChartData : LumipolGraphBase
- (instancetype)initWithSeries:(NSArray<LumipolGraphSeries *> *)series referenceBands:(NSArray<LumipolGraphRefBand *> *)referenceBands segmentMarkers:(NSArray<LumipolGraphMarker *> *)segmentMarkers config:(LumipolGraphChartConfig *)config __attribute__((swift_name("init(series:referenceBands:segmentMarkers:config:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphLineChartData *)doCopySeries:(NSArray<LumipolGraphSeries *> *)series referenceBands:(NSArray<LumipolGraphRefBand *> *)referenceBands segmentMarkers:(NSArray<LumipolGraphMarker *> *)segmentMarkers config:(LumipolGraphChartConfig *)config __attribute__((swift_name("doCopy(series:referenceBands:segmentMarkers:config:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) LumipolGraphChartConfig *config __attribute__((swift_name("config")));
@property (readonly) NSArray<LumipolGraphRefBand *> *referenceBands __attribute__((swift_name("referenceBands")));
@property (readonly) NSArray<LumipolGraphMarker *> *segmentMarkers __attribute__((swift_name("segmentMarkers")));
@property (readonly) NSArray<LumipolGraphSeries *> *series __attribute__((swift_name("series")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LineChartLayout")))
@interface LumipolGraphLineChartLayout : LumipolGraphBase
- (instancetype)initWithSeries:(NSArray<LumipolGraphSeriesLayout *> *)series axisTicks:(NSArray<LumipolGraphAxisTicksLayout *> *)axisTicks refBands:(NSArray<LumipolGraphRefBandLayout *> *)refBands markers:(NSArray<LumipolGraphMarkerLayout *> *)markers stats:(LumipolGraphStats *)stats domains:(LumipolGraphChartDomains *)domains __attribute__((swift_name("init(series:axisTicks:refBands:markers:stats:domains:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphLineChartLayout *)doCopySeries:(NSArray<LumipolGraphSeriesLayout *> *)series axisTicks:(NSArray<LumipolGraphAxisTicksLayout *> *)axisTicks refBands:(NSArray<LumipolGraphRefBandLayout *> *)refBands markers:(NSArray<LumipolGraphMarkerLayout *> *)markers stats:(LumipolGraphStats *)stats domains:(LumipolGraphChartDomains *)domains __attribute__((swift_name("doCopy(series:axisTicks:refBands:markers:stats:domains:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<LumipolGraphAxisTicksLayout *> *axisTicks __attribute__((swift_name("axisTicks")));
@property (readonly) LumipolGraphChartDomains *domains __attribute__((swift_name("domains")));
@property (readonly) NSArray<LumipolGraphMarkerLayout *> *markers __attribute__((swift_name("markers")));
@property (readonly) NSArray<LumipolGraphRefBandLayout *> *refBands __attribute__((swift_name("refBands")));
@property (readonly) NSArray<LumipolGraphSeriesLayout *> *series __attribute__((swift_name("series")));
@property (readonly) LumipolGraphStats *stats __attribute__((swift_name("stats")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Marker")))
@interface LumipolGraphMarker : LumipolGraphBase
- (instancetype)initWithX:(double)x label:(NSString * _Nullable)label emphasis:(BOOL)emphasis __attribute__((swift_name("init(x:label:emphasis:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphMarker *)doCopyX:(double)x label:(NSString * _Nullable)label emphasis:(BOOL)emphasis __attribute__((swift_name("doCopy(x:label:emphasis:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL emphasis __attribute__((swift_name("emphasis")));
@property (readonly) NSString * _Nullable label __attribute__((swift_name("label")));
@property (readonly) double x __attribute__((swift_name("x")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MarkerLayout")))
@interface LumipolGraphMarkerLayout : LumipolGraphBase
- (instancetype)initWithPosition:(double)position label:(NSString * _Nullable)label emphasis:(BOOL)emphasis __attribute__((swift_name("init(position:label:emphasis:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphMarkerLayout *)doCopyPosition:(double)position label:(NSString * _Nullable)label emphasis:(BOOL)emphasis __attribute__((swift_name("doCopy(position:label:emphasis:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL emphasis __attribute__((swift_name("emphasis")));
@property (readonly) NSString * _Nullable label __attribute__((swift_name("label")));
@property (readonly) double position __attribute__((swift_name("position")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NearestResult")))
@interface LumipolGraphNearestResult : LumipolGraphBase
- (instancetype)initWithSeriesId:(NSString *)seriesId x:(double)x y:(double)y __attribute__((swift_name("init(seriesId:x:y:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphNearestResult *)doCopySeriesId:(NSString *)seriesId x:(double)x y:(double)y __attribute__((swift_name("doCopy(seriesId:x:y:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *seriesId __attribute__((swift_name("seriesId")));
@property (readonly) double x __attribute__((swift_name("x")));
@property (readonly) double y __attribute__((swift_name("y")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NormalizedPoint")))
@interface LumipolGraphNormalizedPoint : LumipolGraphBase
- (instancetype)initWithX:(double)x y:(double)y __attribute__((swift_name("init(x:y:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphNormalizedPoint *)doCopyX:(double)x y:(double)y __attribute__((swift_name("doCopy(x:y:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) double x __attribute__((swift_name("x")));
@property (readonly) double y __attribute__((swift_name("y")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PaceSamplePoint")))
@interface LumipolGraphPaceSamplePoint : LumipolGraphBase
- (instancetype)initWithX:(double)x paceSeconds:(double)paceSeconds heartRate:(LumipolGraphDouble * _Nullable)heartRate cadence:(LumipolGraphDouble * _Nullable)cadence altitude:(LumipolGraphDouble * _Nullable)altitude __attribute__((swift_name("init(x:paceSeconds:heartRate:cadence:altitude:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphPaceSamplePoint *)doCopyX:(double)x paceSeconds:(double)paceSeconds heartRate:(LumipolGraphDouble * _Nullable)heartRate cadence:(LumipolGraphDouble * _Nullable)cadence altitude:(LumipolGraphDouble * _Nullable)altitude __attribute__((swift_name("doCopy(x:paceSeconds:heartRate:cadence:altitude:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) LumipolGraphDouble * _Nullable altitude __attribute__((swift_name("altitude")));
@property (readonly) LumipolGraphDouble * _Nullable cadence __attribute__((swift_name("cadence")));
@property (readonly) LumipolGraphDouble * _Nullable heartRate __attribute__((swift_name("heartRate")));
@property (readonly) double paceSeconds __attribute__((swift_name("paceSeconds")));
@property (readonly) double x __attribute__((swift_name("x")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PaceSeriesInput")))
@interface LumipolGraphPaceSeriesInput : LumipolGraphBase
- (instancetype)initWithPoints:(NSArray<LumipolGraphPaceSamplePoint *> *)points runningSeconds:(double)runningSeconds sumDistanceMeters:(double)sumDistanceMeters __attribute__((swift_name("init(points:runningSeconds:sumDistanceMeters:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphPaceSeriesInput *)doCopyPoints:(NSArray<LumipolGraphPaceSamplePoint *> *)points runningSeconds:(double)runningSeconds sumDistanceMeters:(double)sumDistanceMeters __attribute__((swift_name("doCopy(points:runningSeconds:sumDistanceMeters:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<LumipolGraphPaceSamplePoint *> *points __attribute__((swift_name("points")));
@property (readonly) double runningSeconds __attribute__((swift_name("runningSeconds")));
@property (readonly) double sumDistanceMeters __attribute__((swift_name("sumDistanceMeters")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PaceSeriesResult")))
@interface LumipolGraphPaceSeriesResult : LumipolGraphBase
- (instancetype)initWithPace:(NSArray<LumipolGraphPoint *> *)pace heart:(NSArray<LumipolGraphPoint *> *)heart cadence:(NSArray<LumipolGraphPoint *> *)cadence altitudeArea:(NSArray<LumipolGraphPoint *> * _Nullable)altitudeArea bestPaceSeconds:(double)bestPaceSeconds validPaceCount:(int32_t)validPaceCount availableSeries:(NSSet<LumipolGraphInt *> *)availableSeries __attribute__((swift_name("init(pace:heart:cadence:altitudeArea:bestPaceSeconds:validPaceCount:availableSeries:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphPaceSeriesResult *)doCopyPace:(NSArray<LumipolGraphPoint *> *)pace heart:(NSArray<LumipolGraphPoint *> *)heart cadence:(NSArray<LumipolGraphPoint *> *)cadence altitudeArea:(NSArray<LumipolGraphPoint *> * _Nullable)altitudeArea bestPaceSeconds:(double)bestPaceSeconds validPaceCount:(int32_t)validPaceCount availableSeries:(NSSet<LumipolGraphInt *> *)availableSeries __attribute__((swift_name("doCopy(pace:heart:cadence:altitudeArea:bestPaceSeconds:validPaceCount:availableSeries:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<LumipolGraphPoint *> * _Nullable altitudeArea __attribute__((swift_name("altitudeArea")));
@property (readonly) NSSet<LumipolGraphInt *> *availableSeries __attribute__((swift_name("availableSeries")));
@property (readonly) double bestPaceSeconds __attribute__((swift_name("bestPaceSeconds")));
@property (readonly) NSArray<LumipolGraphPoint *> *cadence __attribute__((swift_name("cadence")));
@property (readonly) NSArray<LumipolGraphPoint *> *heart __attribute__((swift_name("heart")));
@property (readonly) NSArray<LumipolGraphPoint *> *pace __attribute__((swift_name("pace")));
@property (readonly) int32_t validPaceCount __attribute__((swift_name("validPaceCount")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Point")))
@interface LumipolGraphPoint : LumipolGraphBase
- (instancetype)initWithX:(double)x y:(double)y __attribute__((swift_name("init(x:y:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphPoint *)doCopyX:(double)x y:(double)y __attribute__((swift_name("doCopy(x:y:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) double x __attribute__((swift_name("x")));
@property (readonly) double y __attribute__((swift_name("y")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RawTrackSample")))
@interface LumipolGraphRawTrackSample : LumipolGraphBase
- (instancetype)initWithCumulativeDistanceMeters:(LumipolGraphDouble * _Nullable)cumulativeDistanceMeters deltaDistanceMeters:(LumipolGraphDouble * _Nullable)deltaDistanceMeters cumulativeSeconds:(LumipolGraphDouble * _Nullable)cumulativeSeconds deltaSeconds:(LumipolGraphDouble * _Nullable)deltaSeconds speedMps:(LumipolGraphDouble * _Nullable)speedMps latitude:(LumipolGraphDouble * _Nullable)latitude longitude:(LumipolGraphDouble * _Nullable)longitude heartRate:(LumipolGraphDouble * _Nullable)heartRate cadence:(LumipolGraphDouble * _Nullable)cadence altitude:(LumipolGraphDouble * _Nullable)altitude __attribute__((swift_name("init(cumulativeDistanceMeters:deltaDistanceMeters:cumulativeSeconds:deltaSeconds:speedMps:latitude:longitude:heartRate:cadence:altitude:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) LumipolGraphRawTrackSampleCompanion *companion __attribute__((swift_name("companion")));
- (LumipolGraphRawTrackSample *)doCopyCumulativeDistanceMeters:(LumipolGraphDouble * _Nullable)cumulativeDistanceMeters deltaDistanceMeters:(LumipolGraphDouble * _Nullable)deltaDistanceMeters cumulativeSeconds:(LumipolGraphDouble * _Nullable)cumulativeSeconds deltaSeconds:(LumipolGraphDouble * _Nullable)deltaSeconds speedMps:(LumipolGraphDouble * _Nullable)speedMps latitude:(LumipolGraphDouble * _Nullable)latitude longitude:(LumipolGraphDouble * _Nullable)longitude heartRate:(LumipolGraphDouble * _Nullable)heartRate cadence:(LumipolGraphDouble * _Nullable)cadence altitude:(LumipolGraphDouble * _Nullable)altitude __attribute__((swift_name("doCopy(cumulativeDistanceMeters:deltaDistanceMeters:cumulativeSeconds:deltaSeconds:speedMps:latitude:longitude:heartRate:cadence:altitude:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) LumipolGraphDouble * _Nullable altitude __attribute__((swift_name("altitude")));
@property (readonly) LumipolGraphDouble * _Nullable cadence __attribute__((swift_name("cadence")));
@property (readonly) LumipolGraphDouble * _Nullable cumulativeDistanceMeters __attribute__((swift_name("cumulativeDistanceMeters")));
@property (readonly) LumipolGraphDouble * _Nullable cumulativeSeconds __attribute__((swift_name("cumulativeSeconds")));
@property (readonly) LumipolGraphDouble * _Nullable deltaDistanceMeters __attribute__((swift_name("deltaDistanceMeters")));
@property (readonly) LumipolGraphDouble * _Nullable deltaSeconds __attribute__((swift_name("deltaSeconds")));
@property (readonly) LumipolGraphDouble * _Nullable heartRate __attribute__((swift_name("heartRate")));
@property (readonly) LumipolGraphDouble * _Nullable latitude __attribute__((swift_name("latitude")));
@property (readonly) LumipolGraphDouble * _Nullable longitude __attribute__((swift_name("longitude")));
@property (readonly) LumipolGraphDouble * _Nullable speedMps __attribute__((swift_name("speedMps")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RawTrackSample.Companion")))
@interface LumipolGraphRawTrackSampleCompanion : LumipolGraphBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LumipolGraphRawTrackSampleCompanion *shared __attribute__((swift_name("shared")));
- (LumipolGraphRawTrackSample *)sanitizedCumulativeDistanceMeters:(LumipolGraphDouble * _Nullable)cumulativeDistanceMeters deltaDistanceMeters:(LumipolGraphDouble * _Nullable)deltaDistanceMeters cumulativeSeconds:(LumipolGraphDouble * _Nullable)cumulativeSeconds deltaSeconds:(LumipolGraphDouble * _Nullable)deltaSeconds speedMps:(LumipolGraphDouble * _Nullable)speedMps latitude:(LumipolGraphDouble * _Nullable)latitude longitude:(LumipolGraphDouble * _Nullable)longitude rawHeartRate:(LumipolGraphDouble * _Nullable)rawHeartRate rawCadence:(LumipolGraphDouble * _Nullable)rawCadence rawAltitude:(LumipolGraphDouble * _Nullable)rawAltitude __attribute__((swift_name("sanitized(cumulativeDistanceMeters:deltaDistanceMeters:cumulativeSeconds:deltaSeconds:speedMps:latitude:longitude:rawHeartRate:rawCadence:rawAltitude:)")));
@property (readonly) double INVALID_ALTITUDE __attribute__((swift_name("INVALID_ALTITUDE")));
@property (readonly) double MAX_CADENCE __attribute__((swift_name("MAX_CADENCE")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RefBand")))
@interface LumipolGraphRefBand : LumipolGraphBase
- (instancetype)initWithLower:(double)lower upper:(double)upper axis:(LumipolGraphAxis *)axis __attribute__((swift_name("init(lower:upper:axis:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphRefBand *)doCopyLower:(double)lower upper:(double)upper axis:(LumipolGraphAxis *)axis __attribute__((swift_name("doCopy(lower:upper:axis:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) LumipolGraphAxis *axis __attribute__((swift_name("axis")));
@property (readonly) double lower __attribute__((swift_name("lower")));
@property (readonly) double upper __attribute__((swift_name("upper")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RefBandLayout")))
@interface LumipolGraphRefBandLayout : LumipolGraphBase
- (instancetype)initWithAxis:(LumipolGraphAxis *)axis lower:(double)lower upper:(double)upper __attribute__((swift_name("init(axis:lower:upper:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphRefBandLayout *)doCopyAxis:(LumipolGraphAxis *)axis lower:(double)lower upper:(double)upper __attribute__((swift_name("doCopy(axis:lower:upper:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) LumipolGraphAxis *axis __attribute__((swift_name("axis")));
@property (readonly) double lower __attribute__((swift_name("lower")));
@property (readonly) double upper __attribute__((swift_name("upper")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RunTotals")))
@interface LumipolGraphRunTotals : LumipolGraphBase
- (instancetype)initWithSumDistanceMeters:(double)sumDistanceMeters runningSeconds:(double)runningSeconds __attribute__((swift_name("init(sumDistanceMeters:runningSeconds:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphRunTotals *)doCopySumDistanceMeters:(double)sumDistanceMeters runningSeconds:(double)runningSeconds __attribute__((swift_name("doCopy(sumDistanceMeters:runningSeconds:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) double runningSeconds __attribute__((swift_name("runningSeconds")));
@property (readonly) double sumDistanceMeters __attribute__((swift_name("sumDistanceMeters")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ScrubPoint")))
@interface LumipolGraphScrubPoint : LumipolGraphBase
- (instancetype)initWithSeriesId:(NSString *)seriesId x:(double)x y:(double)y nx:(double)nx ny:(LumipolGraphDouble * _Nullable)ny role:(LumipolGraphSeriesRole *)role axis:(LumipolGraphAxis *)axis chartAxis:(LumipolGraphChartAxis *)chartAxis __attribute__((swift_name("init(seriesId:x:y:nx:ny:role:axis:chartAxis:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphScrubPoint *)doCopySeriesId:(NSString *)seriesId x:(double)x y:(double)y nx:(double)nx ny:(LumipolGraphDouble * _Nullable)ny role:(LumipolGraphSeriesRole *)role axis:(LumipolGraphAxis *)axis chartAxis:(LumipolGraphChartAxis *)chartAxis __attribute__((swift_name("doCopy(seriesId:x:y:nx:ny:role:axis:chartAxis:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) LumipolGraphAxis *axis __attribute__((swift_name("axis")));
@property (readonly) LumipolGraphChartAxis *chartAxis __attribute__((swift_name("chartAxis")));
@property (readonly) double nx __attribute__((swift_name("nx")));
@property (readonly) LumipolGraphDouble * _Nullable ny __attribute__((swift_name("ny")));
@property (readonly) LumipolGraphSeriesRole *role __attribute__((swift_name("role")));
@property (readonly) NSString *seriesId __attribute__((swift_name("seriesId")));
@property (readonly) double x __attribute__((swift_name("x")));
@property (readonly) double y __attribute__((swift_name("y")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ScrubResult")))
@interface LumipolGraphScrubResult : LumipolGraphBase
- (instancetype)initWithSnappedX:(double)snappedX snappedNx:(double)snappedNx snapSourceId:(NSString *)snapSourceId perSeries:(NSArray<LumipolGraphScrubPoint *> *)perSeries __attribute__((swift_name("init(snappedX:snappedNx:snapSourceId:perSeries:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphScrubResult *)doCopySnappedX:(double)snappedX snappedNx:(double)snappedNx snapSourceId:(NSString *)snapSourceId perSeries:(NSArray<LumipolGraphScrubPoint *> *)perSeries __attribute__((swift_name("doCopy(snappedX:snappedNx:snapSourceId:perSeries:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<LumipolGraphScrubPoint *> *perSeries __attribute__((swift_name("perSeries")));
@property (readonly) NSString *snapSourceId __attribute__((swift_name("snapSourceId")));
@property (readonly) double snappedNx __attribute__((swift_name("snappedNx")));
@property (readonly) double snappedX __attribute__((swift_name("snappedX")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SegmentStat")))
@interface LumipolGraphSegmentStat : LumipolGraphBase
- (instancetype)initWithMin:(double)min max:(double)max avg:(double)avg count:(int32_t)count __attribute__((swift_name("init(min:max:avg:count:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphSegmentStat *)doCopyMin:(double)min max:(double)max avg:(double)avg count:(int32_t)count __attribute__((swift_name("doCopy(min:max:avg:count:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) double avg __attribute__((swift_name("avg")));
@property (readonly) int32_t count __attribute__((swift_name("count")));
@property (readonly) double max __attribute__((swift_name("max")));
@property (readonly) double min __attribute__((swift_name("min")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Series")))
@interface LumipolGraphSeries : LumipolGraphBase
- (instancetype)initWithId:(NSString *)id points:(NSArray<LumipolGraphPoint *> *)points __attribute__((swift_name("init(id:points:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithId:(NSString *)id points:(NSArray<LumipolGraphPoint *> *)points axis:(LumipolGraphAxis *)axis role:(LumipolGraphSeriesRole *)role __attribute__((swift_name("init(id:points:axis:role:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphSeries *)doCopyId:(NSString *)id points:(NSArray<LumipolGraphPoint *> *)points axis:(LumipolGraphAxis *)axis role:(LumipolGraphSeriesRole *)role __attribute__((swift_name("doCopy(id:points:axis:role:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) LumipolGraphAxis *axis __attribute__((swift_name("axis")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSArray<LumipolGraphPoint *> *points __attribute__((swift_name("points")));
@property (readonly) LumipolGraphSeriesRole *role __attribute__((swift_name("role")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SeriesLayout")))
@interface LumipolGraphSeriesLayout : LumipolGraphBase
- (instancetype)initWithId:(NSString *)id role:(LumipolGraphSeriesRole *)role points:(NSArray<LumipolGraphNormalizedPoint *> *)points __attribute__((swift_name("init(id:role:points:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithId:(NSString *)id role:(LumipolGraphSeriesRole *)role points:(NSArray<LumipolGraphNormalizedPoint *> *)points axis:(LumipolGraphAxis *)axis __attribute__((swift_name("init(id:role:points:axis:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphSeriesLayout *)doCopyId:(NSString *)id role:(LumipolGraphSeriesRole *)role points:(NSArray<LumipolGraphNormalizedPoint *> *)points axis:(LumipolGraphAxis *)axis __attribute__((swift_name("doCopy(id:role:points:axis:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) LumipolGraphAxis *axis __attribute__((swift_name("axis")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSArray<LumipolGraphNormalizedPoint *> *points __attribute__((swift_name("points")));
@property (readonly) LumipolGraphSeriesRole *role __attribute__((swift_name("role")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SeriesRole")))
@interface LumipolGraphSeriesRole : LumipolGraphKotlinEnum<LumipolGraphSeriesRole *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) LumipolGraphSeriesRole *main __attribute__((swift_name("main")));
@property (class, readonly) LumipolGraphSeriesRole *overlay __attribute__((swift_name("overlay")));
+ (LumipolGraphKotlinArray<LumipolGraphSeriesRole *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<LumipolGraphSeriesRole *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SeriesStat")))
@interface LumipolGraphSeriesStat : LumipolGraphBase
- (instancetype)initWithId:(NSString *)id min:(double)min max:(double)max avg:(double)avg __attribute__((swift_name("init(id:min:max:avg:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphSeriesStat *)doCopyId:(NSString *)id min:(double)min max:(double)max avg:(double)avg __attribute__((swift_name("doCopy(id:min:max:avg:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) double avg __attribute__((swift_name("avg")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) double max __attribute__((swift_name("max")));
@property (readonly) double min __attribute__((swift_name("min")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SplitSample")))
@interface LumipolGraphSplitSample : LumipolGraphBase
- (instancetype)initWithDistanceMeters:(double)distanceMeters timeSeconds:(double)timeSeconds __attribute__((swift_name("init(distanceMeters:timeSeconds:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphSplitSample *)doCopyDistanceMeters:(double)distanceMeters timeSeconds:(double)timeSeconds __attribute__((swift_name("doCopy(distanceMeters:timeSeconds:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) double distanceMeters __attribute__((swift_name("distanceMeters")));
@property (readonly) double timeSeconds __attribute__((swift_name("timeSeconds")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Stats")))
@interface LumipolGraphStats : LumipolGraphBase
- (instancetype)initWithPerSeries:(NSArray<LumipolGraphSeriesStat *> *)perSeries segments:(NSArray<LumipolGraphSegmentStat *> *)segments segmentSeriesId:(NSString * _Nullable)segmentSeriesId __attribute__((swift_name("init(perSeries:segments:segmentSeriesId:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphStats *)doCopyPerSeries:(NSArray<LumipolGraphSeriesStat *> *)perSeries segments:(NSArray<LumipolGraphSegmentStat *> *)segments segmentSeriesId:(NSString * _Nullable)segmentSeriesId __attribute__((swift_name("doCopy(perSeries:segments:segmentSeriesId:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<LumipolGraphSeriesStat *> *perSeries __attribute__((swift_name("perSeries")));
@property (readonly) NSString * _Nullable segmentSeriesId __attribute__((swift_name("segmentSeriesId")));
@property (readonly) NSArray<LumipolGraphSegmentStat *> *segments __attribute__((swift_name("segments")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("XMode")))
@interface LumipolGraphXMode : LumipolGraphKotlinEnum<LumipolGraphXMode *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) LumipolGraphXMode *distance __attribute__((swift_name("distance")));
@property (class, readonly) LumipolGraphXMode *time __attribute__((swift_name("time")));
+ (LumipolGraphKotlinArray<LumipolGraphXMode *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<LumipolGraphXMode *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ZoneBpmRange")))
@interface LumipolGraphZoneBpmRange : LumipolGraphBase
- (instancetype)initWithLower:(int32_t)lower upper:(LumipolGraphInt * _Nullable)upper __attribute__((swift_name("init(lower:upper:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphZoneBpmRange *)doCopyLower:(int32_t)lower upper:(LumipolGraphInt * _Nullable)upper __attribute__((swift_name("doCopy(lower:upper:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t lower __attribute__((swift_name("lower")));
@property (readonly) LumipolGraphInt * _Nullable upper __attribute__((swift_name("upper")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AxisDomain")))
@interface LumipolGraphAxisDomain : LumipolGraphBase
- (instancetype)initWithMin:(double)min max:(double)max __attribute__((swift_name("init(min:max:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphAxisDomain *)doCopyMin:(double)min max:(double)max __attribute__((swift_name("doCopy(min:max:)")));
- (double)denormalizeT:(double)t __attribute__((swift_name("denormalize(t:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (double)normalizeV:(double)v __attribute__((swift_name("normalize(v:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) double max __attribute__((swift_name("max")));
@property (readonly) double min __attribute__((swift_name("min")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NiceScale")))
@interface LumipolGraphNiceScale : LumipolGraphBase
- (instancetype)initWithNiceMin:(double)niceMin niceMax:(double)niceMax step:(double)step ticks:(NSArray<LumipolGraphDouble *> *)ticks __attribute__((swift_name("init(niceMin:niceMax:step:ticks:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphNiceScale *)doCopyNiceMin:(double)niceMin niceMax:(double)niceMax step:(double)step ticks:(NSArray<LumipolGraphDouble *> *)ticks __attribute__((swift_name("doCopy(niceMin:niceMax:step:ticks:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) double niceMax __attribute__((swift_name("niceMax")));
@property (readonly) double niceMin __attribute__((swift_name("niceMin")));
@property (readonly) double step __attribute__((swift_name("step")));
@property (readonly) NSArray<LumipolGraphDouble *> *ticks __attribute__((swift_name("ticks")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AreaInterpolationKt")))
@interface LumipolGraphAreaInterpolationKt : LumipolGraphBase
+ (LumipolGraphDouble * _Nullable)interpolatedYPoints:(NSArray<LumipolGraphPoint *> *)points x:(double)x __attribute__((swift_name("interpolatedY(points:x:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AxisDomainKt")))
@interface LumipolGraphAxisDomainKt : LumipolGraphBase
+ (NSArray<LumipolGraphDouble *> *)yValuesData:(LumipolGraphLineChartData *)data axis:(LumipolGraphAxis *)axis xWindow:(LumipolGraphAxisDomain * _Nullable)xWindow __attribute__((swift_name("yValues(data:axis:xWindow:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BarHitTestKt")))
@interface LumipolGraphBarHitTestKt : LumipolGraphBase
+ (LumipolGraphInt * _Nullable)barIndexAtXX:(double)x plotMinX:(double)plotMinX plotWidth:(double)plotWidth count:(int32_t)count __attribute__((swift_name("barIndexAtX(x:plotMinX:plotWidth:count:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("HeightFractionsKt")))
@interface LumipolGraphHeightFractionsKt : LumipolGraphBase
+ (NSArray<LumipolGraphDouble *> *)heightFractionsValues:(NSArray<LumipolGraphDouble *> *)values minSpan:(double)minSpan __attribute__((swift_name("heightFractions(values:minSpan:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LabelThinningKt")))
@interface LumipolGraphLabelThinningKt : LumipolGraphBase
+ (BOOL)isLabelVisibleIndex:(int32_t)index count:(int32_t)count stride:(int32_t)stride __attribute__((swift_name("isLabelVisible(index:count:stride:)")));
+ (int32_t)labelStrideCount:(int32_t)count plotWidthPx:(double)plotWidthPx labelWidthPx:(double)labelWidthPx gapPx:(double)gapPx __attribute__((swift_name("labelStride(count:plotWidthPx:labelWidthPx:gapPx:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NearestKt")))
@interface LumipolGraphNearestKt : LumipolGraphBase
+ (NSArray<LumipolGraphNearestResult *> *)nearestData:(LumipolGraphLineChartData *)data x:(double)x __attribute__((swift_name("nearest(data:x:)")));
+ (NSArray<LumipolGraphNearestResult *> *)nearestData:(LumipolGraphLineChartData *)data x:(double)x xMin:(double)xMin xMax:(double)xMax __attribute__((swift_name("nearest(data:x:xMin:xMax:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NiceScaleKt")))
@interface LumipolGraphNiceScaleKt : LumipolGraphBase
+ (LumipolGraphNiceScale *)niceScaleMin:(double)min max:(double)max maxTicks:(int32_t)maxTicks headroomFraction:(double)headroomFraction __attribute__((swift_name("niceScale(min:max:maxTicks:headroomFraction:)")));
@property (class, readonly) double Y_AXIS_HEADROOM_FRACTION __attribute__((swift_name("Y_AXIS_HEADROOM_FRACTION")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ScrubKt")))
@interface LumipolGraphScrubKt : LumipolGraphBase
+ (LumipolGraphScrubResult * _Nullable)nearestScrubData:(LumipolGraphLineChartData *)data layout:(LumipolGraphLineChartLayout *)layout x:(double)x __attribute__((swift_name("nearestScrub(data:layout:x:)")));
@property (class, readonly) double SCRUB_WINDOW_EPSILON __attribute__((swift_name("SCRUB_WINDOW_EPSILON")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StatsKt")))
@interface LumipolGraphStatsKt : LumipolGraphBase
+ (NSArray<LumipolGraphSegmentStat *> *)segmentStatsSeries:(LumipolGraphSeries *)series count:(int32_t)count __attribute__((swift_name("segmentStats(series:count:)")));
+ (LumipolGraphSeriesStat *)seriesStatSeries:(LumipolGraphSeries *)series __attribute__((swift_name("seriesStat(series:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinEnumCompanion")))
@interface LumipolGraphKotlinEnumCompanion : LumipolGraphBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LumipolGraphKotlinEnumCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinArray")))
@interface LumipolGraphKotlinArray<T> : LumipolGraphBase
+ (instancetype)arrayWithSize:(int32_t)size init:(T _Nullable (^)(LumipolGraphInt *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (T _Nullable)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (id<LumipolGraphKotlinIterator>)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(T _Nullable)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((swift_name("KotlinIterator")))
@protocol LumipolGraphKotlinIterator
@required
- (BOOL)hasNext __attribute__((swift_name("hasNext()")));
- (id _Nullable)next __attribute__((swift_name("next()")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
