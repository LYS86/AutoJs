plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

kotlin {
    jvmToolchain(17)
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()
    namespace = "com.stardust"

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
        baseline = file("lint-baseline.xml")
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    androidTestImplementation(libs.androidx.espresso) {
        exclude(group = "com.android.support", module = "support-annotations")
    }
    testImplementation(libs.junit)

    api(libs.kotlin.stdlib)
    api(libs.androidx.annotation)
    implementation(libs.androidx.fragment)
    implementation(libs.timber)
}
