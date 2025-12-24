plugins {
    alias(libs.plugins.kotlin.jvm)
    // Keep 0.25.3 until Kotlin is upgraded (0.35.0+ requires Kotlin 1.9.20+)
    id("com.vanniktech.maven.publish") version "0.25.3"
}

repositories {
    mavenCentral()
    google()
}

// Include the generated Version file
sourceSets {
    main {
        java {
            srcDirs("src/main/kotlin")
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.allWarningsAsErrors = true
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(gradleApi())
    implementation(libs.javassist)
    implementation(libs.kotlin.stdlib)
    compileOnly(libs.android.tools.build.gradle)

    implementation(libs.asm.util)
    implementation(libs.asm.commons)

    // WORKAROUND https://issuetracker.google.com/issues/180889192
    compileOnly(libs.android.tools.common)
    compileOnly(libs.commons.io)
}

println("configure plugin: $name")

apply(from = "../buildscripts/projectMaven.gradle")

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    pom {
        name.set("KARTE Gradle Plugin")
        description.set("Gradle plugin for KARTE Android SDK")
        url.set("https://github.com/plaidev/karte-android-sdk")

        licenses {
            license {
                name.set("The Apache Software License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("kartetaro")
                name.set("Taro Karte")
                email.set("dev.share@plaid.co.jp")
            }
        }

        scm {
            connection.set("scm:git:https://github.com/plaidev/karte-android-sdk.git")
            developerConnection.set("scm:git:ssh://github.com/plaidev/karte-android-sdk.git")
            url.set("https://github.com/plaidev/karte-android-sdk")
        }
    }
}