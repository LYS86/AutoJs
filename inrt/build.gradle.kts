plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(17)
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()
    namespace = "com.stardust.auojs.inrt"

    defaultConfig {
        applicationId = "org.github.autojs.inrt"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.appVersionCode.get().toInt() - 200
        versionName = libs.versions.appVersionName.get()
        multiDexEnabled = true
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "x86_64")
            isUniversalApk = false
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    lint {
        abortOnError = false
        disable += listOf("MissingTranslation", "ExtraTranslation")
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    debugImplementation(libs.compose.ui.tooling)

    implementation(project(":automator"))
    implementation(project(":common"))
    implementation(project(":autojs"))
    implementation(libs.splashscreen)
    implementation(libs.timber)
    implementation(libs.material)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.material.icons.extended)
}
