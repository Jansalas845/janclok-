plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.example.claudeproto"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.claudeproto"
        minSdk = 24
        targetSdk = 34
        versionCode = 3
        versionName = "0.3-offline"
        ndk { abiFilters += listOf("arm64-v8a") }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources.excludes += listOf("/META-INF/{AL2.0,LGPL2.1}", "META-INF/DEPENDENCIES") }
}
dependencies {
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation("com.google.mediapipe:tasks-genai:0.10.24")
}
