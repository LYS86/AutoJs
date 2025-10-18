plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}
kotlin {
    jvmToolchain(21)
}
android {
    compileSdk = Versions.compileSdk

    defaultConfig {
    minSdk = Versions.minSdk
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
    implementation(libs.kotlin.stdlib)
    implementation(libs.androidx.drawerlayout)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.fragment.ktx)
    // https://mvnrepository.com/artifact/com.jakewharton.timber/timber
    implementation(libs.timber)

    api(project(":common:libs:emulatorview"))
    api(project(":common:libs:libtermexec"))
    api(project(":common:libs:term"))

}