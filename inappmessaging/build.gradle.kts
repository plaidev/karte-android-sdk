plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.karte.android.inappmessaging"
    compileSdk = 32

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        minSdk = 21
        //noinspection OldTargetApi
        targetSdk = 32
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

    lint {
        lintConfig = file("../lint.xml")
        warningsAsErrors = true
    }
}

val kotlin_version: String by rootProject.extra

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlin_version")
    implementation(project(":core"))

    compileOnly("androidx.appcompat:appcompat:1.2.0")
    compileOnly("androidx.core:core-ktx:1.2.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test:core:1.3.0")
    testImplementation("com.google.truth:truth:1.0.1")
    testImplementation("io.mockk:mockk:1.10.0")
    testImplementation("org.robolectric:robolectric:4.8")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.8.0")
    testImplementation(project(":test_lib"))
}

afterEvaluate {
    rootProject.findProperty("afterConfigurate")?.let {
        (it as? (Project) -> Unit)?.invoke(project)
    }
}