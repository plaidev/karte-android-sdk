plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.karte.android.test_lib"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
        targetSdk = 34
        multiDexEnabled = true
    }
    lintOptions {
        isAbortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(project(":core"))
    implementation("androidx.test:core:1.7.0")
    implementation("io.mockk:mockk:1.13.5")
    implementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    implementation("com.google.truth:truth:1.4.5")
    implementation("org.robolectric:robolectric:4.16")
    implementation("net.bytebuddy:byte-buddy:1.18.2")
}
