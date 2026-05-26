pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                // Android Build Tools and related libraries (Android/Firebase only)
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google\\.android.*")
                includeGroupByRegex("com\\.google\\.firebase.*")
            }
        }
        mavenCentral {
            content {
                // Kotlin
                includeGroup("org.jetbrains.kotlin")
                includeGroup("org.jetbrains.kotlinx")

                // Third-party libraries
                includeGroup("io.coil-kt")
                includeGroup("com.squareup.okhttp3")

                // Testing libraries
                includeGroup("junit")
                includeGroup("io.mockk")
                includeGroup("org.robolectric")
                includeGroup("net.bytebuddy")
                includeGroup("net.javacrumbs.json-unit")
                includeGroup("org.json")
                includeGroup("org.objenesis")
                includeGroup("com.google.truth")

                // Code quality
                includeGroup("com.pinterest.ktlint")
            }
        }
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

