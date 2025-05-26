pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

include(":core", ":inappmessaging", ":notifications", ":variables", ":visualtracking", ":inbox", ":inappframe", ":debugger")
include(":sample_java", ":sample_kotlin", ":test_lib")
rootProject.name = "Karte"
includeBuild("gradle-plugin") {
    dependencySubstitution {
        substitute(module("io.karte.android:local-gradle-plugin")).using(project(":"))
    }
}

project(":test_lib").projectDir = File(settingsDir, "core/test_lib")
project(":sample_java").projectDir = File(settingsDir, "examples/sample_java")
project(":sample_kotlin").projectDir = File(settingsDir, "examples/sample_kotlin")
