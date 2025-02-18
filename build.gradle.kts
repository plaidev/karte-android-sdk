buildscript {
    extra["kotlin_version"] = "1.4.32"
    extra["coroutine_version"] = "1.4.3"
    dependencies {
        // for apply karte plugin to example project
        classpath("io.karte.android:local-gradle-plugin")
        classpath("com.google.gms:google-services:4.3.3")
    }
}

plugins {
    id("com.android.application") version "7.0.4" apply false
    id("com.android.library") version "7.0.4" apply false
    id("org.jetbrains.kotlin.android") version "1.7.20" apply false
    id("org.jetbrains.dokka") version "1.9.20"
    
    // for upload maven repo
    id("com.github.dcendents.android-maven") version "2.1" apply false
    id("io.codearte.nexus-staging") version "0.22.0"
    
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.13.2"
}

apiValidation {
    ignoredProjects.addAll(listOf("test_lib", "sample_kotlin", "sample_java"))
}

configure(subprojects.filter { !it.name.startsWith("sample_") && !it.name.startsWith("test") }) {
    println("configure libraries: ${project.name}")
    
    apply(from = "../buildscripts/projectDokka.gradle")
    apply(from = "../buildscripts/projectJacoco.gradle")
    
    apply(from = "../buildscripts/projectMaven.gradle")
    apply(from = "../buildscripts/projectMavenAndroid.gradle")
}

extra["ossrhUsername"] = System.getenv().getOrDefault("MAVEN_USER_NAME", "")
extra["ossrhPassword"] = System.getenv().getOrDefault("MAVEN_PASSWORD", "")
extra["afterConfigure"] = { project: Project ->
    project.apply(from = "../buildscripts/projectSonatype.gradle")
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

apply(from = "buildscripts/ktlint.gradle")
apply(from = "buildscripts/dokka.gradle")
apply(from = "buildscripts/jacoco.gradle")
apply(from = "buildscripts/sonatype.gradle")
