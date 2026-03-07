plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    androidTestImplementation(libs.androidx.espresso) {
        exclude(group = "com.android.support", module = "support-annotations")
    }
    testImplementation(libs.junit)

    api(libs.rhino)
    implementation(files("libs/dx.jar"))

    api(libs.eventbus)
    api(libs.zip4j)

    api(libs.material.dialogs.core) {
        exclude(group = "com.android.support")
    }

    api(libs.material)
    api(libs.enhancedfloaty)
    api(libs.roundedimageview)

    implementation("local:opencv:3.4.3")

    api(libs.okhttp)
    api(libs.jdeferred)

    api("local:RootShell:1.6")

    api(libs.gson)

    api(libs.glide) {
        exclude(group = "com.android.support")
    }

    api(libs.bundles.log4j)

    implementation("local:libtermexec:1.0")
    implementation("local:emulatorview:1.0")
    implementation("local:term-debug:1.0")

    api(project(":common"))
    api(project(":automator"))

    api(libs.androidx.core.ktx)
    api(libs.androidx.browser)
}
