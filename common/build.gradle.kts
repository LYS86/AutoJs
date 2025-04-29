plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    val versions = rootProject.extra["versions"] as Map<*, *>
    
    compileSdk = versions["compile"].toString().toInt()

    defaultConfig {
        minSdk = versions["mini"].toString().toInt()
        targetSdk = versions["target"].toString().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    namespace = "com.stardust"
    
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
    api(libs.kotlin.stdlib)
    api(libs.androidx.annotation)
    api(libs.settingscompat)
    api(libs.opencv)

    api(project(":emulatorview"))
    api(project(":libtermexec"))
    api(project(":term"))
}