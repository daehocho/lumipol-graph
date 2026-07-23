import XCTest
import LumipolGraph
@testable import LumipolGraphUI

final class PaceColormapTests: XCTestCase {

    private func input(_ value: Double, fastest: Double, slowest: Double, average: Double) -> BarPaceColorInput {
        BarPaceColorInput(value: value, fastest: fastest, slowest: slowest, average: average,
                          isPartial: false, index: 0, colorRole: .onTarget)
    }

    private func rgba(_ c: UIColor) -> (CGFloat, CGFloat, CGFloat, CGFloat) {
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        c.getRed(&r, green: &g, blue: &b, alpha: &a)
        return (r, g, b, a)
    }

    private func assertRGB(_ c: UIColor, _ er: CGFloat, _ eg: CGFloat, _ eb: CGFloat,
                           _ msg: String, file: StaticString = #filePath, line: UInt = #line) {
        let (r, g, b, _) = rgba(c)
        XCTAssertEqual(r, er, accuracy: 0.01, "\(msg) red", file: file, line: line)
        XCTAssertEqual(g, eg, accuracy: 0.01, "\(msg) green", file: file, line: line)
        XCTAssertEqual(b, eb, accuracy: 0.01, "\(msg) blue", file: file, line: line)
    }

    // 값 [300,330,360] → fastest=300, slowest=360, average=330
    // pace1=330-(330-300)*0.70=309, pace2=330+(360-330)*0.25=337.5
    // length1=9, length2=28.5, length3=22.5
    func testFastestIsBlueEnd() {
        // p=300<pace1: cv=(309-300)/9=1 → RGB(0, 1-0.4, 1)
        assertRGB(ChartStyle.defaultPaceColor(input(300, fastest: 300, slowest: 360, average: 330)),
                  0, 0.6, 1, "fastest")
    }
    func testSlowestIsRed() {
        // p=360>=pace2: cv=(360-360)/22.5=0 → RGB(1,0,0)
        assertRGB(ChartStyle.defaultPaceColor(input(360, fastest: 300, slowest: 360, average: 330)),
                  1, 0, 0, "slowest")
    }
    func testAverageIsGreenYellowMix() {
        // p=330 in [pace1,pace2): cv=(337.5-330)/28.5=0.2632 → RGB(1-0.2632,1,0)
        assertRGB(ChartStyle.defaultPaceColor(input(330, fastest: 300, slowest: 360, average: 330)),
                  0.7368, 1, 0, "average")
    }
    func testPace1BoundaryIsGreen() {
        // p=309==pace1: 중간 구간 cv=(337.5-309)/28.5=1 → RGB(0,1,0)
        assertRGB(ChartStyle.defaultPaceColor(input(309, fastest: 300, slowest: 360, average: 330)),
                  0, 1, 0, "pace1")
    }
    func testPace2BoundaryIsYellow() {
        // p=337.5==pace2: 느린 구간 cv=(360-337.5)/22.5=1 → RGB(1,1,0)
        assertRGB(ChartStyle.defaultPaceColor(input(337.5, fastest: 300, slowest: 360, average: 330)),
                  1, 1, 0, "pace2")
    }
    func testDegenerateSinglePaceIsGreen() {
        // fastest==slowest → 중간 초록
        assertRGB(ChartStyle.defaultPaceColor(input(330, fastest: 330, slowest: 330, average: 330)),
                  0, 1, 0, "degenerate")
    }
}
