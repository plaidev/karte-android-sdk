plugins {
    id("com.android.application")
    id("io.karte.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "io.karte.sample_java"
    compileSdk = 34

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

    implementation("androidx.appcompat:appcompat:1.1.0")
    implementation("com.google.firebase:firebase-core:17.2.2")
    implementation("com.google.firebase:firebase-messaging:20.1.0")

    testImplementation("junit:junit:4.12")
    androidTestImplementation("androidx.test:runner:1.2.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.2.0")
}
