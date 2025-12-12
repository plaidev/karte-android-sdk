plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.vanniktech.maven.publish")
}

apply(from = "../buildscripts/ktlint.gradle")

android {
    namespace = "io.karte.android.inappframe"
    compileSdk = 36

    buildFeatures {
        viewBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.4"
    }

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

    testOptions {
        unitTests.apply {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }

    lint {
        lintConfig = file("../lint.xml")
        warningsAsErrors = true
        // FIXME: ObsoleteLintCustomCheckをdisableしないとlintでエラーが出てしまっていた。
        // jetified-activity-compose-1.10.1/jars/lint.jar: Error: Library lint checks reference invalid APIs; these checks will be skipped!
        // このObsoleteLintCustomCheckをdisableにしてしまうことで、本来なされるべきComposeのLintが走らないという問題が起きる。
        // Kotlinが1.9.10以上になったら消せるかもなのでその後また試すこと。
        disable += "ObsoleteLintCustomCheck"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        allWarningsAsErrors = true
    }
}

dependencies {
    implementation(fileTree("dir" to "libs", "include" to listOf("*.jar")))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation(project(":core"))
    implementation(project(":variables"))
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.cardview:cardview:1.0.0")

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.runtime:runtime-livedata")

    implementation("androidx.activity:activity-compose:1.9.3")
    // 2.7以降はkotlin 2系でコンパイルされているので2.6で止める
    implementation("io.coil-kt:coil-compose:2.6.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("com.google.truth:truth:1.4.5")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.json:json:20250517")
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation(project(":inappframe"))
    testImplementation(project(":test_lib"))
}

apply(from = "../buildscripts/projectMaven.gradle")
apply(from = "../buildscripts/projectPublishing.gradle")
