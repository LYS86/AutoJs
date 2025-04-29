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


    namespace = "com.stardust.autojs"

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
    api(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    api(libs.eventbus)
    api(libs.zip4j)
    api(libs.material.dialogs.core) {
        exclude(group = "com.android.support")
    }
    api(libs.material)
    api(libs.enhancedfloaty)
    api(libs.roundedimageview)
    // OkHttp
    api(libs.okhttp)
    // JDeferred
    api(libs.jdeferred.android)
    // RootShell
    api(libs.rootshell)
    // Gson
    api(libs.gson)
    // log4j
    api(libs.android.logging.log4j)
    api(libs.log4j)
    // Rhino JavaScript Engine
    api(files("libs/rhino-1.7.15.jar"))
    api(project(":common"))
    api(project(":automator"))
}

// LiteRT dependencies
dependencies {
    implementation(libs.litert)
    implementation(libs.litert.gpu)
    implementation(libs.litert.metadata)
    implementation(libs.litert.support)
}

// ML Kit dependencies
dependencies {
    implementation(libs.text.recognition)
    implementation(libs.text.recognition.chinese)
}