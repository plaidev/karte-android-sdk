plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

apply(from = "../buildscripts/ktlint.gradle")

android {
    namespace = "io.karte.android.inappframe"
    compileSdk = 34

    buildFeatures {
        viewBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.3.2"
    }

    defaultConfig {
        minSdk = 21
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

    testOptions {
        unitTests.apply {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }

    kotlinOptions {
        allWarningsAsErrors = true
    }

    lint {
        lintConfig = file("../lint.xml")
        warningsAsErrors = true
    }
}

dependencies {
    implementation(fileTree("dir" to "libs", "include" to listOf("*.jar")))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.3")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation(project(":core"))
    implementation(project(":variables"))
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("androidx.cardview:cardview:1.0.0")

    implementation("androidx.compose.foundation:foundation:1.5.0")
    implementation("androidx.compose.ui:ui:1.3.2")
    implementation("androidx.compose.material:material:1.3.1")
    implementation("androidx.compose.ui:ui-tooling:1.3.2")
    implementation("androidx.activity:activity-compose:1.3.2")
    implementation("androidx.compose.runtime:runtime-livedata:1.3.2")
    implementation("io.coil-kt:coil-compose:2.4.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.4.3")
    testImplementation("androidx.test:core:1.4.0")
    testImplementation("io.mockk:mockk:1.10.0")
    testImplementation("com.google.truth:truth:1.0.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.8.0")
    testImplementation("org.json:json:20180813")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation(project(":inappframe"))
    testImplementation(project(":test_lib"))
}

afterEvaluate {
    (rootProject.findProperty("afterConfigurate") as? (Project) -> Unit)?.invoke(project)
}