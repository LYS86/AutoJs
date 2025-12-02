plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
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
    buildConfigField("String", "VERSION_NAME", "\"${Versions.appVersionName}\"")
    buildConfigField("int", "VERSION_CODE", "${Versions.appVersionCode}")
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
        aidl = true
    }
}

dependencies {
    androidTestImplementation(libs.espresso.core) {
        exclude(group = "com.android.support", module = "support-annotations")
    }
    testImplementation(libs.junit)
    implementation(libs.eventbus)
    api(libs.material.dialogs.core) {
        exclude(group = "com.android.support")
    }
    implementation(libs.material)
    implementation(libs.enhancedfloaty)
    implementation(libs.roundedimageview)
    // OkHttp
    implementation(libs.okhttp)
    // Gson
    implementation(libs.gson)

    // Rhino JavaScript Engine
    api(files("libs/rhino-1.7.15.jar"))
    api(project(":common"))
    api(project(":automator"))
    implementation(libs.bundles.shizuku)
    implementation(libs.bundles.litert.all)

    implementation(libs.androidx.core.ktx)

    // https://mvnrepository.com/artifact/com.jakewharton.timber/timber
    implementation(libs.timber)
    implementation(libs.opencv)
    implementation(libs.androidx.browser)
    implementation(libs.r8)


}

// ML Kit dependencies
dependencies {
    implementation(libs.text.recognition)
    implementation(libs.text.recognition.chinese)
}