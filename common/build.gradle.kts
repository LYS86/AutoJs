plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

val versions = rootProject.extra["versions"] as Map<*, *>

android {
    compileSdk = versions["compile"] as Int

    defaultConfig {
        minSdk = versions["mini"] as Int
        targetSdk = versions["target"] as Int

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    namespace = "com.stardust"
    buildFeatures {
        buildConfig = true
    }
    lint {
        abortOnError = false
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    androidTestImplementation("androidx.test.espresso:espresso-core:3.1.1-alpha01") {
        exclude(group = "com.android.support", module = "support-annotations")
    }
    testImplementation("junit:junit:4.12")
    api("androidx.annotation:annotation:1.0.0")
    api("com.github.hyb1996:settingscompat:1.1.5")
}