include(":app", ":automator", ":common", ":autojs", ":inrt",
        ":emulatorview",
        ":libtermexec",
        ":term")

project(":emulatorview").projectDir = file("common/libs/emulatorview")
project(":libtermexec").projectDir = file("common/libs/libtermexec")
project(":term").projectDir = file("common/libs/term")