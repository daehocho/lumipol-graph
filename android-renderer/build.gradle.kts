plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
    id("maven-publish")
}

group = "com.lumipol.graph"
version = "0.10.0"

android {
    namespace = "com.lumipol.graph.renderer"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }
    buildFeatures {
        compose = true
    }
    // 단일 release variant만 발행(maven-publish 컴포넌트 소스).
    publishing {
        singleVariant("release")
    }
    // Robolectric로 컴포저블·제스처를 JVM 단위테스트에서 구동하려면 Android 리소스가 필요.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    jvmToolchain(17)
}

// AGP release 컴포넌트는 afterEvaluate 후에야 생성된다.
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifactId = "renderer"
            }
        }
    }
}

dependencies {
    // 공개 컴포저블 시그니처가 core 타입(LineChartData 등)을 노출하므로 api 필수 —
    // implementation이면 POM에서 runtime 스코프가 되어 외부 소비자가 컴파일 불가.
    api(project(":core"))

    implementation(libs.compose.foundation)
    implementation(libs.compose.animation.core)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.text)
    implementation(libs.compose.ui.unit)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)

    // Robolectric + compose-ui-test로 컴포저블·제스처 배선을 JVM에서 검증(에뮬 불필요).
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
