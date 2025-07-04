plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.vanniktech.maven.publish")
}

android {
    namespace = "io.karte.android.debugger"
    compileSdk = 34
    
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        minSdk = 21
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
            isReturnDefaultValues = true
        }
    }


    lint {
        lintConfig = file("../lint.xml")
        warningsAsErrors = true
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    compileOnly("androidx.annotation:annotation:1.3.0")
    implementation(project(":core"))

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test:core:1.4.0")
    testImplementation("io.mockk:mockk:1.10.0")
    testImplementation("com.google.truth:truth:1.0.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.8.0")
    testImplementation("org.json:json:20180813")
    testImplementation(project(":debugger"))
}

apply(from = "../buildscripts/projectMaven.gradle")
apply(from = "../buildscripts/projectPublishing.gradle") 