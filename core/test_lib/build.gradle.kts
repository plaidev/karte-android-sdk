plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.karte.android.test_lib"
    compileSdk = 34

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
    implementation("androidx.test:core:1.4.0")
    implementation("io.mockk:mockk:1.10.0")
    implementation("com.squareup.okhttp3:mockwebserver:4.8.0")
    implementation("com.google.truth:truth:1.0.1")
    implementation("org.robolectric:robolectric:4.11.1")
    implementation("org.objenesis:objenesis:3.2") { 
        version { 
            strictly("3.2") 
        } 
    }
}
