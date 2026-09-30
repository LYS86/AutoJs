plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = libs.versions.compile.get().toInt()

    defaultConfig {
        minSdk = libs.versions.mini.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    namespace = "com.stardust.automator"
    buildFeatures {
        buildConfig = true
    }
    lint {
        abortOnError = false
        baseline = file("lint-baseline.xml")
        targetSdk = libs.versions.target.get().toInt()
    }
}


dependencies {
    androidTestImplementation("androidx.test.espresso:espresso-core:3.1.1-alpha01") {
        exclude(group = "com.android.support", module = "support-annotations")
    }
    testImplementation("junit:junit:4.12")
    api("androidx.appcompat:appcompat:1.1.0")
    api(project(":common"))
}