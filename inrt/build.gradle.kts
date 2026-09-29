plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val versions = rootProject.extra["versions"] as Map<*, *>

android {
    compileSdk = versions["compile"] as Int

    defaultConfig {
        applicationId = "com.stardust.auojs.inrt"
        minSdk = versions["mini"] as Int
        targetSdk = versions["target"] as Int
        versionCode = (versions["appVersionCode"] as Int) - 200
        versionName = versions["appVersionName"] as String
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
            include("x86_64", "arm64-v8a")
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
    namespace = "com.stardust.auojs.inrt"
    buildFeatures {
        buildConfig = true
    }
    lint {
        abortOnError = false
        baseline = file("lint-baseline.xml")
        disable.addAll(listOf("MissingTranslation", "ExtraTranslation"))
    }
}

fun buildApkPluginForAbi(pluginProjectDir: File, abi: String) {
    val appVersionName = versions["appVersionName"] as String
    copy {
        from("../app/release/")
        into(File(pluginProjectDir, "app/src/main/assets"))
        val fileName = "inrt-$abi-release.apk"
        include(fileName)
        rename(fileName, "template.apk")
    }
    providers.exec {
        workingDir(pluginProjectDir)
        commandLine("gradlew.bat", "assembleRelease")
    }.result.get()
    copy {
        from(File(pluginProjectDir, "app/build/outputs/apk/release"))
        into("../common/release")
        val fileName = "打包插件-$appVersionName-release.apk"
        include(fileName)
        rename(fileName, "打包插件-$abi-$appVersionName-release.apk")
    }
}

tasks.register("buildApkPlugin") {
    doLast {
        val pluginProjectDir = file("../../AutoJsApkBuilderPlugin")
        if (!pluginProjectDir.exists() || !pluginProjectDir.isDirectory) {
            println("pluginProjectDir not exists")
            return@doLast
        }
        buildApkPluginForAbi(pluginProjectDir, "arm64-v8a")
        buildApkPluginForAbi(pluginProjectDir, "x86_64")
    }
}

tasks.whenTaskAdded {
    if (name == "assembleRelease") {
        finalizedBy("buildApkPlugin")
    }
}


dependencies {
    implementation(fileTree("libs") { include("*.jar") })
    implementation("androidx.preference:preference:1.1.1")
    implementation("com.google.android.material:material:1.4.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.1.1-alpha01") {
        exclude(group = "com.android.support", module = "support-annotations")
    }
    //Glide
    implementation("com.github.bumptech.glide:glide:4.2.0") {
        exclude(group = "com.android.support")
    }
    testImplementation("junit:junit:4.12")
    implementation(project(":automator"))
    implementation(project(":common"))
    implementation(project(":autojs"))
    implementation(libs.timber)
}