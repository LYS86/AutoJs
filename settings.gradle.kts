include(
        ":app", ":automator", ":common", ":autojs", ":inrt",
        ":common:libs:emulatorview",
        ":common:libs:libtermexec",
        ":common:libs:term"
)

pluginManagement {
        repositories {
                maven("https://maven.aliyun.com/repository/gradle-plugin")
                google()
                gradlePluginPortal()
        }
}

dependencyResolutionManagement {
        repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
        repositories {
                maven("https://maven.aliyun.com/repository/google")
                maven("https://maven.aliyun.com/repository/public")
                google()
                mavenCentral()
                maven("https://jitpack.io")
        }
}

rootProject.name = "AutoJs"