plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}
kotlin {
    jvmToolchain(21)
}
android {
    compileSdk = Versions.compileSdk

    defaultConfig {
        applicationId = "io.github.autojs"
        minSdk = Versions.minSdk
        targetSdk = Versions.targetSdk
        versionCode = Versions.appVersionCode
        versionName = Versions.appVersionName
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


    flavorDimensions += "channel"

    splits {
        abi {
            isEnable = true
            reset()
            include("x86", "armeabi-v7a", "arm64-v8a", "x86_64")
            isUniversalApk = true
        }
    }

    productFlavors {
        create("common") {
            buildConfigField("String", "CHANNEL", "\"common\"")
        }
        create("coolapk") {
            buildConfigField("String", "CHANNEL", "\"coolapk\"")
        }
    }

    namespace = "org.autojs.autojs"

    lint {
        abortOnError = false
        disable.add("MissingTranslation")
        disable.add("ExtraTranslation")
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    androidTestImplementation(libs.espresso.core) {
        exclude(group = "com.android.support", module = "support-annotations")
    }
    testImplementation(libs.junit)
    debugImplementation(libs.leakcanary.android)

    // Kotlin
    implementation(libs.kotlinx.coroutines.android)
    // Android supports
    implementation(libs.appcompat)
    implementation(libs.androidx.cardview)
    implementation(libs.material)
    implementation(libs.androidx.multidex)
    // Personal libraries
    implementation(libs.mutabletheme)
    // Material Dialogs
    implementation(libs.material.dialogs.core) {
        exclude(group = "com.android.support")
    }

    // Common Markdown
    implementation(libs.commonmark.java)
    // MultiLevelListView
    implementation(libs.android.multi.level.listview)
    // Licenses Dialog
    implementation(libs.licensesdialog)
    // Expandable RecyclerView
    implementation(libs.expandablerecyclerview)
    // FlexibleDivider
    implementation(libs.recyclerview.flexibledivider)
    implementation(libs.library)
    // Commons-lang
    implementation(libs.commons.lang3)
    // Expandable RecyclerView
    implementation(libs.thoughtbot.expandablerecyclerview)
    // RxJava
    implementation(libs.rxjava)
    implementation(libs.rxandroid)
    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.retrofit2.rxjava2.adapter)
    implementation(libs.retrofit2.kotlin.coroutines.adapter)
    // Glide
    implementation(libs.glide) {
        exclude(group = "com.android.support")
    }
    // Joda time
    implementation(libs.joda.time)
    // Tasker Plugin
    implementation(libs.android.plugin.client.sdk.for1.locale)
    // Bugly
    implementation(libs.crashreport)
    // MaterialDialogCommon
    implementation(libs.commons) {
        exclude(group = "com.android.support")
    }
    // WorkManager
    implementation(libs.bundles.work)
    // Project modules
    implementation(project(":autojs"))
    implementation(libs.commons.io)

    // https://mvnrepository.com/artifact/androidx.lifecycle/lifecycle-runtime-ktx
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.timber)
    implementation(libs.eventbus)
    implementation(libs.enhancedfloaty)
    implementation(libs.roundedimageview)
    implementation(libs.jdeferred.android)
    implementation(libs.rootshell)
    implementation(libs.androidx.preference)
    implementation(libs.expandable.layout)
}