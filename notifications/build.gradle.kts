plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.vanniktech.maven.publish")
}

android {
    namespace = "io.karte.android.notifications"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
        //noinspection OldTargetApi
        targetSdk = 34
        buildConfigField("String", "LIB_VERSION", "\"$version\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("proguard-rules.pro")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        allWarningsAsErrors = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    lint {
        lintConfig = file("../lint.xml")
        warningsAsErrors = true
    }
}

val kotlin_version: String by rootProject.extra

dependencies {
    implementation(fileTree("dir" to "libs", "include" to listOf("*.jar")))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    implementation("com.google.firebase:firebase-messaging:24.1.2")
    api(project(":core"))

    testImplementation("com.google.firebase:firebase-messaging:24.1.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("com.google.truth:truth:1.4.5")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation(project(":test_lib"))
}

apply(from = "../buildscripts/projectMaven.gradle")
apply(from = "../buildscripts/projectPublishing.gradle")