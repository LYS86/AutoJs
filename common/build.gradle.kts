plugins {
    id("com.android.library")
    kotlin("android")
}
kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
android {
    val versions = rootProject.extra["versions"] as Map<*, *>

    compileSdk = versions["compile"].toString().toInt()

    defaultConfig {
        minSdk = versions["mini"].toString().toInt()
        targetSdk = versions["target"].toString().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    namespace = "com.stardust"

    lint {
        abortOnError = false
    }
    buildFeatures {
        buildConfig = true
    }
}

// 导入 libs 下面的 aar 文件,作为模块使用
subprojects {
    val libsDir = projectDir.parentFile
    if (libsDir.name == "libs" && libsDir.parentFile.name == "common") {
        configurations.maybeCreate("default")
        val aarFiles = projectDir.listFiles()?.filter { it.extension == "aar" } ?: emptyList()
        val aar = when {
            aarFiles.size == 1 -> aarFiles[0]
            aarFiles.isEmpty() -> throw GradleException("${projectDir.name} 模块下未找到 AAR 文件")
            else -> throw GradleException("${projectDir.name} 模块下找到多个 AAR 文件: ${aarFiles.map { it.name }}")
        }
        artifacts {
            add("default", aar)
        }
    }
}

dependencies {
    androidTestImplementation(libs.espresso.core) {
        exclude(group = "com.android.support", module = "support-annotations")
    }
    testImplementation(libs.junit)
    api(libs.kotlin.stdlib)
    api(libs.androidx.annotation)
    api(libs.settingscompat)
    api(libs.opencv)
    // https://mvnrepository.com/artifact/androidx.core/core-ktx
    api(libs.androidx.core.ktx)

    // https://mvnrepository.com/artifact/com.jakewharton.timber/timber
    api(libs.timber)

    api(project(":common:libs:emulatorview"))
    api(project(":common:libs:libtermexec"))
    api(project(":common:libs:term"))

}