#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class LumipolGraphAxis, LumipolGraphAxisDomain, LumipolGraphAxisTick, LumipolGraphAxisTicksLayout, LumipolGraphBarChartData, LumipolGraphBarChartEngine, LumipolGraphBarChartLayout, LumipolGraphBarColorRole, LumipolGraphBarLayout, LumipolGraphChartAxis, LumipolGraphChartConfig, LumipolGraphDonutChartData, LumipolGraphDonutChartLayout, LumipolGraphDonutColorRole, LumipolGraphDonutEngine, LumipolGraphDonutSegment, LumipolGraphDonutSegmentLayout, LumipolGraphKotlinArray<T>, LumipolGraphKotlinEnum<E>, LumipolGraphKotlinEnumCompanion, LumipolGraphLineChartData, LumipolGraphLineChartEngine, LumipolGraphLineChartLayout, LumipolGraphMarker, LumipolGraphMarkerLayout, LumipolGraphNearestResult, LumipolGraphNiceScale, LumipolGraphNormalizedPoint, LumipolGraphPoint, LumipolGraphRefBand, LumipolGraphRefBandLayout, LumipolGraphRefLine, LumipolGraphRefLineLayout, LumipolGraphSegmentStat, LumipolGraphSeries, LumipolGraphSeriesLayout, LumipolGraphSeriesRole, LumipolGraphSeriesStat, LumipolGraphSplitSample, LumipolGraphStats;

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
- (LumipolGraphBarChartLayout *)layoutData:(LumipolGraphBarChartData *)data __attribute__((swift_name("layout(data:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DonutEngine")))
@interface LumipolGraphDonutEngine : LumipolGraphBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)donutEngine __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LumipolGraphDonutEngine *shared __attribute__((swift_name("shared")));
- (LumipolGraphDonutChartLayout *)layoutData:(LumipolGraphDonutChartData *)data __attribute__((swift_name("layout(data:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LineChartEngine")))
@interface LumipolGraphLineChartEngine : LumipolGraphBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)lineChartEngine __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LumipolGraphLineChartEngine *shared __attribute__((swift_name("shared")));
- (LumipolGraphLineChartLayout *)layoutData:(LumipolGraphLineChartData *)data __attribute__((swift_name("layout(data:)")));
- (LumipolGraphLineChartLayout *)layoutData:(LumipolGraphLineChartData *)data xMin:(double)xMin xMax:(double)xMax __attribute__((swift_name("layout(data:xMin:xMax:)")));
- (NSArray<LumipolGraphNearestResult *> *)nearestData:(LumipolGraphLineChartData *)data x:(double)x __attribute__((swift_name("nearest(data:x:)")));
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
- (instancetype)initWithSamples:(NSArray<LumipolGraphSplitSample *> *)samples splitDistanceMeters:(double)splitDistanceMeters targetPaceSecPerUnit:(LumipolGraphDouble * _Nullable)targetPaceSecPerUnit toleranceSecPerUnit:(double)toleranceSecPerUnit maxTicks:(int32_t)maxTicks __attribute__((swift_name("init(samples:splitDistanceMeters:targetPaceSecPerUnit:toleranceSecPerUnit:maxTicks:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphBarChartData *)doCopySamples:(NSArray<LumipolGraphSplitSample *> *)samples splitDistanceMeters:(double)splitDistanceMeters targetPaceSecPerUnit:(LumipolGraphDouble * _Nullable)targetPaceSecPerUnit toleranceSecPerUnit:(double)toleranceSecPerUnit maxTicks:(int32_t)maxTicks __attribute__((swift_name("doCopy(samples:splitDistanceMeters:targetPaceSecPerUnit:toleranceSecPerUnit:maxTicks:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t maxTicks __attribute__((swift_name("maxTicks")));
@property (readonly) NSArray<LumipolGraphSplitSample *> *samples __attribute__((swift_name("samples")));
@property (readonly) double splitDistanceMeters __attribute__((swift_name("splitDistanceMeters")));
@property (readonly) LumipolGraphDouble * _Nullable targetPaceSecPerUnit __attribute__((swift_name("targetPaceSecPerUnit")));
@property (readonly) double toleranceSecPerUnit __attribute__((swift_name("toleranceSecPerUnit")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BarChartLayout")))
@interface LumipolGraphBarChartLayout : LumipolGraphBase
- (instancetype)initWithBars:(NSArray<LumipolGraphBarLayout *> *)bars yTicks:(NSArray<LumipolGraphAxisTick *> *)yTicks referenceLinePosition:(LumipolGraphDouble * _Nullable)referenceLinePosition __attribute__((swift_name("init(bars:yTicks:referenceLinePosition:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphBarChartLayout *)doCopyBars:(NSArray<LumipolGraphBarLayout *> *)bars yTicks:(NSArray<LumipolGraphAxisTick *> *)yTicks referenceLinePosition:(LumipolGraphDouble * _Nullable)referenceLinePosition __attribute__((swift_name("doCopy(bars:yTicks:referenceLinePosition:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<LumipolGraphBarLayout *> *bars __attribute__((swift_name("bars")));
@property (readonly) LumipolGraphDouble * _Nullable referenceLinePosition __attribute__((swift_name("referenceLinePosition")));
@property (readonly) NSArray<LumipolGraphAxisTick *> *yTicks __attribute__((swift_name("yTicks")));
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
- (instancetype)initWithIndex:(int32_t)index value:(double)value heightFraction:(double)heightFraction colorRole:(LumipolGraphBarColorRole *)colorRole isPartial:(BOOL)isPartial __attribute__((swift_name("init(index:value:heightFraction:colorRole:isPartial:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphBarLayout *)doCopyIndex:(int32_t)index value:(double)value heightFraction:(double)heightFraction colorRole:(LumipolGraphBarColorRole *)colorRole isPartial:(BOOL)isPartial __attribute__((swift_name("doCopy(index:value:heightFraction:colorRole:isPartial:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) LumipolGraphBarColorRole *colorRole __attribute__((swift_name("colorRole")));
@property (readonly) double heightFraction __attribute__((swift_name("heightFraction")));
@property (readonly) int32_t index __attribute__((swift_name("index")));
@property (readonly) BOOL isPartial __attribute__((swift_name("isPartial")));
@property (readonly) double value __attribute__((swift_name("value")));
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
+ (LumipolGraphKotlinArray<LumipolGraphChartAxis *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<LumipolGraphChartAxis *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChartConfig")))
@interface LumipolGraphChartConfig : LumipolGraphBase
- (instancetype)initWithSegmentCount:(int32_t)segmentCount maxTicks:(int32_t)maxTicks __attribute__((swift_name("init(segmentCount:maxTicks:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphChartConfig *)doCopySegmentCount:(int32_t)segmentCount maxTicks:(int32_t)maxTicks __attribute__((swift_name("doCopy(segmentCount:maxTicks:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t maxTicks __attribute__((swift_name("maxTicks")));
@property (readonly) int32_t segmentCount __attribute__((swift_name("segmentCount")));
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
- (LumipolGraphDonutSegment *)doCopyValue:(double)value colorRole:(LumipolGraphDonutColorRole *)colorRole __attribute__((swift_name("doCopy(value:colorRole:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) LumipolGraphDonutColorRole *colorRole __attribute__((swift_name("colorRole")));
@property (readonly) double value __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DonutSegmentLayout")))
@interface LumipolGraphDonutSegmentLayout : LumipolGraphBase
- (instancetype)initWithStartFraction:(double)startFraction sweepFraction:(double)sweepFraction value:(double)value colorRole:(LumipolGraphDonutColorRole *)colorRole __attribute__((swift_name("init(startFraction:sweepFraction:value:colorRole:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphDonutSegmentLayout *)doCopyStartFraction:(double)startFraction sweepFraction:(double)sweepFraction value:(double)value colorRole:(LumipolGraphDonutColorRole *)colorRole __attribute__((swift_name("doCopy(startFraction:sweepFraction:value:colorRole:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) LumipolGraphDonutColorRole *colorRole __attribute__((swift_name("colorRole")));
@property (readonly) double startFraction __attribute__((swift_name("startFraction")));
@property (readonly) double sweepFraction __attribute__((swift_name("sweepFraction")));
@property (readonly) double value __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LineChartData")))
@interface LumipolGraphLineChartData : LumipolGraphBase
- (instancetype)initWithSeries:(NSArray<LumipolGraphSeries *> *)series referenceLines:(NSArray<LumipolGraphRefLine *> *)referenceLines referenceBands:(NSArray<LumipolGraphRefBand *> *)referenceBands segmentMarkers:(NSArray<LumipolGraphMarker *> *)segmentMarkers config:(LumipolGraphChartConfig *)config __attribute__((swift_name("init(series:referenceLines:referenceBands:segmentMarkers:config:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphLineChartData *)doCopySeries:(NSArray<LumipolGraphSeries *> *)series referenceLines:(NSArray<LumipolGraphRefLine *> *)referenceLines referenceBands:(NSArray<LumipolGraphRefBand *> *)referenceBands segmentMarkers:(NSArray<LumipolGraphMarker *> *)segmentMarkers config:(LumipolGraphChartConfig *)config __attribute__((swift_name("doCopy(series:referenceLines:referenceBands:segmentMarkers:config:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) LumipolGraphChartConfig *config __attribute__((swift_name("config")));
@property (readonly) NSArray<LumipolGraphRefBand *> *referenceBands __attribute__((swift_name("referenceBands")));
@property (readonly) NSArray<LumipolGraphRefLine *> *referenceLines __attribute__((swift_name("referenceLines")));
@property (readonly) NSArray<LumipolGraphMarker *> *segmentMarkers __attribute__((swift_name("segmentMarkers")));
@property (readonly) NSArray<LumipolGraphSeries *> *series __attribute__((swift_name("series")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LineChartLayout")))
@interface LumipolGraphLineChartLayout : LumipolGraphBase
- (instancetype)initWithSeries:(NSArray<LumipolGraphSeriesLayout *> *)series axisTicks:(NSArray<LumipolGraphAxisTicksLayout *> *)axisTicks refLines:(NSArray<LumipolGraphRefLineLayout *> *)refLines refBands:(NSArray<LumipolGraphRefBandLayout *> *)refBands markers:(NSArray<LumipolGraphMarkerLayout *> *)markers stats:(LumipolGraphStats *)stats __attribute__((swift_name("init(series:axisTicks:refLines:refBands:markers:stats:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphLineChartLayout *)doCopySeries:(NSArray<LumipolGraphSeriesLayout *> *)series axisTicks:(NSArray<LumipolGraphAxisTicksLayout *> *)axisTicks refLines:(NSArray<LumipolGraphRefLineLayout *> *)refLines refBands:(NSArray<LumipolGraphRefBandLayout *> *)refBands markers:(NSArray<LumipolGraphMarkerLayout *> *)markers stats:(LumipolGraphStats *)stats __attribute__((swift_name("doCopy(series:axisTicks:refLines:refBands:markers:stats:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<LumipolGraphAxisTicksLayout *> *axisTicks __attribute__((swift_name("axisTicks")));
@property (readonly) NSArray<LumipolGraphMarkerLayout *> *markers __attribute__((swift_name("markers")));
@property (readonly) NSArray<LumipolGraphRefBandLayout *> *refBands __attribute__((swift_name("refBands")));
@property (readonly) NSArray<LumipolGraphRefLineLayout *> *refLines __attribute__((swift_name("refLines")));
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
__attribute__((swift_name("RefLine")))
@interface LumipolGraphRefLine : LumipolGraphBase
- (instancetype)initWithValue:(double)value axis:(LumipolGraphAxis *)axis label:(NSString * _Nullable)label __attribute__((swift_name("init(value:axis:label:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphRefLine *)doCopyValue:(double)value axis:(LumipolGraphAxis *)axis label:(NSString * _Nullable)label __attribute__((swift_name("doCopy(value:axis:label:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) LumipolGraphAxis *axis __attribute__((swift_name("axis")));
@property (readonly) NSString * _Nullable label __attribute__((swift_name("label")));
@property (readonly) double value __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RefLineLayout")))
@interface LumipolGraphRefLineLayout : LumipolGraphBase
- (instancetype)initWithAxis:(LumipolGraphAxis *)axis position:(double)position label:(NSString * _Nullable)label __attribute__((swift_name("init(axis:position:label:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphRefLineLayout *)doCopyAxis:(LumipolGraphAxis *)axis position:(double)position label:(NSString * _Nullable)label __attribute__((swift_name("doCopy(axis:position:label:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) LumipolGraphAxis *axis __attribute__((swift_name("axis")));
@property (readonly) NSString * _Nullable label __attribute__((swift_name("label")));
@property (readonly) double position __attribute__((swift_name("position")));
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
- (LumipolGraphSeriesLayout *)doCopyId:(NSString *)id role:(LumipolGraphSeriesRole *)role points:(NSArray<LumipolGraphNormalizedPoint *> *)points __attribute__((swift_name("doCopy(id:role:points:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
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
@property (class, readonly) LumipolGraphSeriesRole *ghost __attribute__((swift_name("ghost")));
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
__attribute__((swift_name("AxisDomain")))
@interface LumipolGraphAxisDomain : LumipolGraphBase
- (instancetype)initWithMin:(double)min max:(double)max __attribute__((swift_name("init(min:max:)"))) __attribute__((objc_designated_initializer));
- (LumipolGraphAxisDomain *)doCopyMin:(double)min max:(double)max __attribute__((swift_name("doCopy(min:max:)")));
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
__attribute__((swift_name("AxisDomainKt")))
@interface LumipolGraphAxisDomainKt : LumipolGraphBase
+ (NSArray<LumipolGraphDouble *> *)yValuesData:(LumipolGraphLineChartData *)data axis:(LumipolGraphAxis *)axis xWindow:(LumipolGraphAxisDomain * _Nullable)xWindow __attribute__((swift_name("yValues(data:axis:xWindow:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NearestKt")))
@interface LumipolGraphNearestKt : LumipolGraphBase
+ (NSArray<LumipolGraphNearestResult *> *)nearestData:(LumipolGraphLineChartData *)data x:(double)x __attribute__((swift_name("nearest(data:x:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NiceScaleKt")))
@interface LumipolGraphNiceScaleKt : LumipolGraphBase
+ (LumipolGraphNiceScale *)niceScaleMin:(double)min max:(double)max maxTicks:(int32_t)maxTicks __attribute__((swift_name("niceScale(min:max:maxTicks:)")));
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
