plugins {
    id("com.android.application")
    kotlin("android")
}
kotlin {
    jvmToolchain(21)
}
android {
    // 使用 buildSrc 中的 Versions 对象
    compileSdk = Versions.compileSdk

    defaultConfig {
        applicationId = "com.stardust.auojs.inrt"
        minSdk = Versions.minSdk
        targetSdk = Versions.targetSdk
        versionCode = (Versions.appVersionCode - 200)
        versionName = Versions.appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("x86", "armeabi-v7a")
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

    lint {
        abortOnError = false
        disable.add("MissingTranslation")
        disable.add("ExtraTranslation")
    }
    buildFeatures {
        buildConfig = true
    }
}

fun buildApkPluginForAbi(pluginProjectDir: File, abi: String) {

    copy {
        from(file("..\\app\\release\\"))
        into(File(pluginProjectDir, "app\\src\\main\\assets"))
        val fileName = "inrt-$abi-release.apk"
        include(fileName)
        rename(fileName, "template.apk")
    }
    val execOps = project.extensions.getByType(ExecOperations::class.java)
    execOps.exec {
        workingDir = pluginProjectDir
        commandLine("gradlew.bat", "assembleRelease")
    }
    copy {
        from(File(pluginProjectDir, "app\\build\\outputs\\apk\\release"))
        into(file("..\\common\\release"))
        val fileName = "打包插件-${Versions.appVersionName}-release.apk"
        include(fileName)
        rename(fileName, "打包插件-$abi-${Versions.appVersionName}-release.apk")
    }
}

tasks.register("buildApkPlugin") {
    doLast {
        val pluginProjectDirPath = "..\\..\\AutoJsApkBuilderPlugin"
        val pluginProjectDir = file(pluginProjectDirPath)
        if (!pluginProjectDir.exists() || !pluginProjectDir.isDirectory) {
            println("pluginProjectDir not exists")
            return@doLast
        }
        buildApkPluginForAbi(pluginProjectDir, "armeabi-v7a")
        buildApkPluginForAbi(pluginProjectDir, "x86")
    }
}

tasks.whenTaskAdded {
    if (name == "assembleRelease") {
        finalizedBy("buildApkPlugin")
    }
}

repositories {
    google()
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    androidTestImplementation(libs.espresso.core) {
        exclude(group = "com.android.support", module = "support-annotations")
    }
    testImplementation(libs.junit)

    // Glide
    implementation(libs.glide) {
        exclude(group = "com.android.support")
    }

    implementation(project(":autojs"))
}