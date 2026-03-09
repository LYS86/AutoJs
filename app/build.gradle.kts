plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()
    namespace = "org.autojs.autojs"

    defaultConfig {
        applicationId = "org.autojs.autojs"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.appVersionCode.get().toInt()
        versionName = libs.versions.appVersionName.get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        encoding = "utf-8"
    }

    buildFeatures {
        viewBinding = true
    }

    configurations.all {
        resolutionStrategy.force("com.google.code.findbugs:jsr305:3.0.1")
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("x86", "armeabi-v7a")
            isUniversalApk = false
        }
    }

    lint {
        abortOnError = false
        disable += listOf("MissingTranslation", "ExtraTranslation")
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines)
    implementation(libs.commons.io)
    androidTestImplementation(libs.androidx.espresso) {
        exclude(group = "com.android.support", module = "support-annotations")
    }
    testImplementation(libs.junit)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.cardview)
    implementation(libs.material)
    implementation(libs.androidx.multidex)

    implementation(libs.mutabletheme)

    implementation(libs.material.dialogs.core) {
        exclude(group = "com.android.support")
    }

    implementation(libs.bundles.commonmark)

    implementation(libs.multi.level.listview)
    implementation(libs.licensesdialog)
    implementation(libs.expandablerecyclerview)
    implementation(libs.expandable.layout)
    implementation(libs.flexibledivider)

    implementation(project(":apkbuilder"))
    implementation(libs.apksig)
    implementation(libs.spongycastle)

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
    releaseImplementation(libs.leakcanary.no.op)
    debugImplementation(libs.leakcanary.fragment)

    implementation(libs.timber)

    implementation(project(":automator"))
    implementation(project(":common"))
    implementation(project(":autojs"))
}
