plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.binaryCompatibilityValidator)
}

// API 표면 스냅샷(50-guardrails §4) — apiDump를 커밋하고 apiCheck를 게이트에 포함한다.
// iOS 표면은 체크인된 LumipolGraph.h의 diff가 곧 API diff(릴리스 체크리스트).
apiValidation {
    ignoredProjects.addAll(listOf("android")) // 샘플 앱은 API 표면 아님
}
