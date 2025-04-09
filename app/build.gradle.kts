plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.asiochatfrontend"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.asiochatfrontend"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        multiDexEnabled = true
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // AndroidX and Material
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.core.ktx)

    // Kotlin Coroutines + Lifecycle
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.livedata.ktx)

    // Room (SQLite ORM)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)

    // Retrofit (Relay Mode)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)

    // WebSocket (Relay Mode)
    implementation(libs.java.websocket)

    // Hilt (Dependency Injection)
    implementation(libs.hilt.android)

    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.converter.moshi)

    // Encryption (RSA, AES)
    implementation(libs.bcprov.jdk15to18)

    // Media & File Handling
    implementation(libs.media)
    implementation(libs.imagepicker)

    // Background Tasks (WorkManager)
    implementation(libs.work.runtime.ktx)

    // Multidex
    implementation(libs.multidex)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
