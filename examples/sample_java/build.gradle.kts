plugins {
    id("com.android.application")
    id("io.karte.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "io.karte.sample_java"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.karte.tracker_sample"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(project(":core"))
    implementation(project(":inappmessaging"))
    implementation(project(":notifications"))
    implementation(project(":variables"))
    implementation(project(":visualtracking"))

    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.firebase:firebase-analytics:22.1.2")
    implementation("com.google.firebase:firebase-messaging:24.1.2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
