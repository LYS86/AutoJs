plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
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
    }
}

dependencies {
    androidTestImplementation(libs.androidx.espresso) {
        exclude(group = "com.android.support", module = "support-annotations")
    }
    testImplementation(libs.junit)

    api(libs.kotlin.stdlib)
    api(libs.androidx.annotation)
    api(libs.settingscompat)
    implementation(libs.timber)
}
