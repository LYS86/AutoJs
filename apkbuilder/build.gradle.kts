plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    compileSdkVersion(Versions.compile)

    defaultConfig {
        minSdkVersion(Versions.mini)
        targetSdkVersion(Versions.target)
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    lintOptions {
        isAbortOnError = false
    }
    namespace = "com.stardust.autojs.apkbuilder"
}

dependencies {
}
