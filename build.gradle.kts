// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    val kotlin_version by extra("1.8.10")
    val coroutine_version by extra("1.7.3")
    dependencies {
        // for apply karte plugin to example project
        classpath("io.karte.android:local-gradle-plugin")
        classpath("com.google.gms:google-services:4.3.3")
    }
}

plugins {
    id("com.android.application") version "8.1.1" apply false
    id("com.android.library") version "8.1.1" apply false
    id("org.jetbrains.kotlin.android") version "1.8.10" apply false
    id("org.jetbrains.dokka") version "1.9.20"
    // for upload maven repo
    id("com.github.dcendents.android-maven") version "2.1" apply false
    id("io.codearte.nexus-staging") version "0.22.0"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.13.2"
}

apiValidation {
    ignoredProjects.add("test_lib")
    ignoredProjects.add("sample_kotlin")
    ignoredProjects.add("sample_java")
}

configure(subprojects.filter { !it.name.startsWith("sample_") && !it.name.startsWith("test") }) {
    println("configure libraries: ${project.name}")
    apply(from = "../buildscripts/projectDokka.gradle")
    apply(from = "../buildscripts/projectJacoco.gradle")
    apply(from = "../buildscripts/projectMaven.gradle")
    apply(from = "../buildscripts/projectMavenAndroid.gradle")
}

ext {
    set("ossrhUsername", System.getenv().getOrDefault("MAVEN_USER_NAME", ""))
    set("ossrhPassword", System.getenv().getOrDefault("MAVEN_PASSWORD", ""))
    set("afterConfigurate", { project: Project ->
        project.apply(from = "../buildscripts/projectSonatype.gradle")
    })
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

apply(from = "buildscripts/ktlint.gradle")
apply(from = "buildscripts/dokka.gradle")
apply(from = "buildscripts/jacoco.gradle")
apply(from = "buildscripts/sonatype.gradle")