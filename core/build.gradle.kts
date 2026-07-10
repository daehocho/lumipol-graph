import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    id("maven-publish")
}

group = "com.lumipol.graph"
version = "0.10.0"

kotlin {
    jvmToolchain(17)
    jvm() // 호스트에서 빠른 commonTest 실행용

    androidTarget {
        publishLibraryVariants("release")
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    val xcf = XCFramework("LumipolGraph")
    listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach { target ->
        target.binaries.framework {
            baseName = "LumipolGraph"
            xcf.add(this)
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
