plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(17)
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()
    namespace = "org.autojs.autojs"

    defaultConfig {
        applicationId = "org.github.autojs"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.appVersionCode.get().toInt()
        versionName = libs.versions.appVersionName.get()
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isShrinkResources = false
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
        release {
            isShrinkResources = false
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }

    configurations.all {
        resolutionStrategy.force("com.google.code.findbugs:jsr305:3.0.1")
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "x86_64")
            isUniversalApk = false
        }
    }

    lint {
        abortOnError = false
        disable += listOf("MissingTranslation", "ExtraTranslation")
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines)
    implementation(libs.commons.io)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.webkit)
    implementation(libs.material)
    implementation(libs.androidx.multidex)

    implementation(libs.mutabletheme)

    implementation(libs.material.dialogs.core) {
        exclude(group = "com.android.support")
    }

    implementation(libs.multi.level.listview)
    implementation(libs.licensesdialog)
    implementation(libs.expandablerecyclerview)
    implementation(libs.expandable.layout)
    implementation(libs.flexibledivider)

    implementation(project(":apkbuilder"))
    implementation(libs.apksig)
    implementation(libs.spongycastle) {
        exclude(group = "junit", module = "junit")
    }

    implementation(libs.bundles.rxjava)
    implementation(libs.bundles.retrofit)

    implementation(libs.glide) {
        exclude(group = "com.android.support")
    }
    kapt(libs.glide.compiler)

    implementation(libs.joda.time)
    implementation(libs.tasker.plugin)
    implementation(libs.bugly)

    implementation(libs.material.dialogs.commons) {
        exclude(group = "com.android.support")
    }

    implementation(libs.android.job)

    debugImplementation(libs.leakcanary)

    implementation(libs.timber)
    implementation(libs.splashscreen)

    implementation(project(":automator"))
    implementation(project(":common"))
    implementation(project(":autojs"))
    implementation(libs.bundles.shizuku)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
