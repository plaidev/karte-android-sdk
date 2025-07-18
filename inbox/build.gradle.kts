plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.vanniktech.maven.publish")
}

android {
    namespace = "io.karte.android.inbox"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        //noinspection OldTargetApi
        targetSdk = 34
        buildConfigField("String", "LIB_VERSION", "\"$version\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("proguard-rules.pro")
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    testOptions {
        unitTests.apply {
            isReturnDefaultValues = true
        }
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += "-Xexplicit-api=strict"
    }

    lint {
        lintConfig = file("../lint.xml")
        warningsAsErrors = true
    }
}

val kotlin_version: String by rootProject.extra
val coroutine_version: String by rootProject.extra

dependencies {
    implementation(fileTree("dir" to "libs", "include" to listOf("*.jar")))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutine_version")
    compileOnly("androidx.annotation:annotation:1.3.0")
    api(project(":core"))

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutine_version")
    testImplementation("androidx.test:core:1.4.0")
    testImplementation("io.mockk:mockk:1.10.0")
    testImplementation("com.google.truth:truth:1.0.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.8.0")
    testImplementation("org.json:json:20180813")
    testImplementation(project(":inbox"))
}

apply(from = "../buildscripts/projectMaven.gradle")
apply(from = "../buildscripts/projectPublishing.gradle")