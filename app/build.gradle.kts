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

    // 정식판(full)과 14일 무료 체험판(trial)을 한 번에 빌드 (같은 앱 ID → 체험판 위에 정식판을 깔면 학습 기록 그대로)
    buildFeatures { buildConfig = true }
    flavorDimensions += "edition"
    productFlavors {
        create("full") {
            dimension = "edition"
            buildConfigField("int", "TRIAL_DAYS", "0")
        }
        create("trial") {
            dimension = "edition"
            buildConfigField("int", "TRIAL_DAYS", "14")
            versionNameSuffix = "-trial"
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
