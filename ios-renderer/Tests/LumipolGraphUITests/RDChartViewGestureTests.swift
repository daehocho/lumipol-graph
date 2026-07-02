import XCTest

@testable import LumipolGraphUI

final class RDChartViewGestureTests: XCTestCase {
    func testGesturesAllowSimultaneousRecognitionWithScrollView() {
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        let recognizers = view.gestureRecognizers ?? []
        XCTAssertFalse(recognizers.isEmpty)
        let other = UIPanGestureRecognizer()
        for recognizer in recognizers {
            let delegate = recognizer.delegate
            XCTAssertTrue(delegate === view, "제스처 delegate는 뷰 자신이어야 함")
            XCTAssertTrue(
                delegate?.gestureRecognizer?(
                    recognizer, shouldRecognizeSimultaneouslyWith: other
                ) == true,
                "스크롤뷰 pan과 동시 인식 허용"
            )
        }
    }
}
