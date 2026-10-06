plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.jmc.fingererror"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.jmc.fingererror"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // 항상 같은 서명 → 새 버전을 덮어 설치해도 학습 기록 유지
    signingConfigs {
        create("fe") {
            storeFile = file("fingererror.jks")
            storePassword = "fingererror"
            keyAlias = "fingererror"
            keyPassword = "fingererror"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fe")
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
