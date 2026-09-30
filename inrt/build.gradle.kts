plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = libs.versions.compile.get().toInt()

    defaultConfig {
        applicationId = "com.stardust.auojs.inrt"
        minSdk = libs.versions.mini.get().toInt()
        targetSdk = libs.versions.target.get().toInt()
        versionCode = libs.versions.appVersionCode.get().toInt() - 200
        versionName = libs.versions.appVersionName.get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
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
    val appVersionName = libs.versions.appVersionName.get()
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