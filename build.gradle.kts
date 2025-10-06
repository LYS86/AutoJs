// ---------- 根 build.gradle.kts ----------
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android)      apply false
}
val buildToolsVersion by extra("34.0.0")


tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}