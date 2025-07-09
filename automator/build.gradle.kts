plugins {
    id("com.android.library")
    kotlin("android")
}
kotlin {
    jvmToolchain(21)
}
android {
    val versions = rootProject.extra["versions"] as Map<*, *>

    compileSdk = versions["compile"].toString().toInt()

    defaultConfig {
        minSdk = versions["mini"].toString().toInt()
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

repositories {
    google()
}

dependencies {
    androidTestImplementation(libs.espresso.core) {
        exclude(group = "com.android.support", module = "support-annotations")
    }
    testImplementation(libs.junit)
    api(libs.appcompat)
    api(project(":common"))
}