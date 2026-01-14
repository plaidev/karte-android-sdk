// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        // for apply karte plugin to example project
        classpath("io.karte.android:local-gradle-plugin")
        classpath(libs.gms.google.services)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.dokka)
    // for upload maven repo via Central Portal
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.binary.compatibility.validator)
    alias(libs.plugins.dependency.guard) apply false
}

apiValidation {
    ignoredProjects.add("test_lib")
    ignoredProjects.add("sample_kotlin")
    ignoredProjects.add("sample_java")
}

// Configure Java toolchain for all Android projects
subprojects {
    pluginManager.withPlugin("org.jetbrains.kotlin.android") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension> {
            jvmToolchain(21)
        }
    }
}

configure(subprojects.filter { !it.name.startsWith("sample_") && !it.name.startsWith("test") }) {
    println("configure libraries: ${project.name}")
    apply(from = "../buildscripts/projectDokka.gradle")
    apply(from = "../buildscripts/projectJacoco.gradle")
    apply(from = "../buildscripts/projectMaven.gradle")
    apply(from = "../buildscripts/projectMavenAndroid.gradle")
}


tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

apply(from = "buildscripts/ktlint.gradle")
apply(from = "buildscripts/dokka.gradle")
apply(from = "buildscripts/jacoco.gradle")