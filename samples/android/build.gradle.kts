plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "com.lumipol.graph.sample"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.lumipol.graph.sample"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.9.0"
    }
    buildFeatures {
        compose = true
    }
    // 데모라 릴리스 서명은 다루지 않는다 — assembleDebug 컴파일 검증만 목표.
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":android-renderer"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.text)
    implementation(libs.compose.ui.unit)
}
