plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(17)
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()
    namespace = "com.stardust.autojs"

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    lint {
        abortOnError = false
    }
    buildFeatures {
        aidl = true
        buildConfig = true
        compose = true
    }
}

dependencies {
    androidTestImplementation(libs.androidx.espresso) {
        exclude(group = "com.android.support", module = "support-annotations")
    }
    testImplementation(libs.junit)

    api(libs.rhino)
    implementation(files("libs/dx.jar"))
    implementation(libs.kotlinx.coroutines)

    api(libs.eventbus)
    api(libs.zip4j)

    api(libs.material.dialogs.core) {
        exclude(group = "com.android.support")
    }

    implementation(libs.material)
    api(libs.enhancedfloaty)
    api(libs.roundedimageview)

    implementation(libs.opencv)

    api(libs.okhttp)
    api(libs.jdeferred)

    api("local:RootShell:1.6")

    api(libs.gson)

    api(libs.glide) {
        exclude(group = "com.android.support")
    }

    implementation("local:libtermexec:1.0")
    implementation("local:emulatorview:1.0")
    implementation("local:term-debug:1.0")

    api(project(":common"))
    api(project(":automator"))

    api(libs.androidx.core.ktx)
    api(libs.androidx.browser)
    implementation(libs.timber)
    implementation(libs.bundles.shizuku)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
}
