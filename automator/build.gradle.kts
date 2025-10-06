plugins {
    id("com.android.library")
    kotlin("android")
}
kotlin {
    jvmToolchain(21)
}
android {
    // 使用 buildSrc 中的 Versions 对象
    compileSdk = Versions.compileSdk

    defaultConfig {
    minSdk = Versions.minSdk
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    namespace = "com.stardust.automator"

    lint {
        abortOnError = false
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    androidTestImplementation(libs.espresso.core) {
        exclude(group = "com.android.support", module = "support-annotations")
    }
    testImplementation(libs.junit)
    api(libs.appcompat)
    api(project(":common"))
}