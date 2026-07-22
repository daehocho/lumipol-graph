import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    id("maven-publish")
}

// JitPack(Linux 빌더)은 Kotlin/Native iOS 타겟을 빌드할 수 없다 — macOS 호스트에서만 등록.
// iOS 소비는 Maven이 아니라 xcframework(SPM)라서 발행 아티팩트에 iOS 타겟이 없어도 무영향.
group = "com.github.daehocho.lumipol-graph"
version = "0.12.0"

kotlin {
    jvmToolchain(17)
    jvm() // 호스트에서 빠른 commonTest 실행용

    androidTarget {
        publishLibraryVariants("release")
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    if (System.getProperty("os.name").startsWith("Mac")) {
        val xcf = XCFramework("LumipolGraph")
        listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach { target ->
            target.binaries.framework {
                baseName = "LumipolGraph"
                xcf.add(this)
            }
        }
    }

    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

android {
    namespace = "com.lumipol.graph.core"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }
}
