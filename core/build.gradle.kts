plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.vanniktech.maven.publish")
}

android {
    namespace = "io.karte.android"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        minSdk = 21
        //noinspection OldTargetApi
        targetSdk = 34
        buildConfigField("String", "LIB_VERSION", "\"$version\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("proguard-rules.pro")
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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    testOptions {
        unitTests.apply {
            isIncludeAndroidResources = true
        }
    }

    tasks.withType<Test> {
        systemProperty("robolectric.sqliteMode", "NATIVE")
    }

    lint {
        lintConfig = file("../lint.xml")
        warningsAsErrors = true
    }
}

val kotlin_version: String by rootProject.extra

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")

    compileOnly("androidx.core:core-ktx:1.16.0")
    compileOnly("androidx.ads:ads-identifier:1.0.0-alpha05")
    compileOnly("com.google.android.gms:play-services-ads-identifier:18.2.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("com.google.truth:truth:1.4.5")
    testImplementation("io.mockk:mockk:1.13.5")
    testRuntimeOnly("net.bytebuddy:byte-buddy:1.18.2")
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("com.google.android.gms:play-services-ads-identifier:18.2.0")
    testImplementation(project(":test_lib"))
}

apply(from = "../buildscripts/projectMaven.gradle")
apply(from = "../buildscripts/projectPublishing.gradle")