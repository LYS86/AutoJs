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


    namespace = "com.stardust.autojs"
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
    testImplementation(libs.junit)
    api(fileTree("libs") { include("*.jar") })
    api("org.greenrobot:eventbus:3.0.0")
    implementation(libs.r8)
    api("com.afollestad.material-dialogs:core:0.9.2.3") {
        exclude(group = "com.android.support")
    }
    api("com.google.android.material:material:1.1.0-alpha01")
    api("com.github.hyb1996:EnhancedFloaty:0.31")
    api("com.makeramen:roundedimageview:2.3.0")
    api("local:opencv:3.4.3")
    // OkHttp
    api("com.squareup.okhttp3:okhttp:3.10.0")
    // JDeferred
    api("org.jdeferred:jdeferred-android-aar:1.2.6")
    // Gson
    api("com.google.code.gson:gson:2.8.2")
    // log4j
    api("de.mindpipe.android:android-logging-log4j:1.0.3")
    api("log4j:log4j:1.2.17")
    api("local:libtermexec:1.0")
    api("local:emulatorview:1.0")
    api("local:term:1.0")
    implementation("org.mozilla:rhino:1.7.15.1")
    implementation(libs.timber)
    api(project(":common"))
    api(project(":automator"))
    implementation("androidx.browser:browser:1.3.0")
}