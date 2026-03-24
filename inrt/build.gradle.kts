plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()
    namespace = "com.stardust.auojs.inrt"

    defaultConfig {
        applicationId = "com.stardust.auojs.inrt"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.appVersionCode.get().toInt() - 200
        versionName = libs.versions.appVersionName.get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
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
    }
}

dependencies {
    androidTestImplementation(libs.androidx.espresso) {
        exclude(group = "com.android.support", module = "support-annotations")
    }

    testImplementation(libs.junit)

    implementation(project(":automator"))
    implementation(project(":common"))
    implementation(project(":autojs"))
    implementation(libs.splashscreen)
    implementation(libs.timber)
}
