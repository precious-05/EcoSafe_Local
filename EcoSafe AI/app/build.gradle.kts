plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.eco_front"
    compileSdk = 36  // ✅ Changed from 35 to 36

    defaultConfig {
        applicationId = "com.example.eco_front"
        minSdk = 24
        targetSdk = 36  // ✅ Changed from 35 to 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Camera X
    implementation("androidx.camera:camera-core:1.4.0")  // ✅ Updated
    implementation("androidx.camera:camera-camera2:1.4.0")  // ✅ Updated
    implementation("androidx.camera:camera-lifecycle:1.4.0")  // ✅ Updated
    implementation("androidx.camera:camera-view:1.4.0")  // ✅ Updated

    // Network
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Permissions
    implementation("com.karumi:dexter:6.2.3")

    // Image Loading
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Guava for CameraX
    implementation("com.google.guava:guava:33.3.0-android")  // ✅ Updated
    // Google Maps
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Charts for risk analysis
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}