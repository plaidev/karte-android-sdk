plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.karte.android.test_lib"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        multiDexEnabled = true
    }
    lint {
        abortOnError = false
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
    implementation(libs.androidx.test.core)
    implementation(libs.byte.buddy)
    implementation(libs.mockk)
    implementation(libs.mockwebserver)
    implementation(libs.truth)
    implementation(libs.robolectric)
    implementation(libs.objenesis) {
        version {
            strictly(libs.versions.objenesis.get())
        }
    }
}
