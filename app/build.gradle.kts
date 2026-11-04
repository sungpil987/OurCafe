plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services") apply false
}
val firebaseConfigured = file("google-services.json").exists()
if (firebaseConfigured) apply(plugin = "com.google.gms.google-services")
val signingFile = file("ourcafe-signing.jks")
val signingPassword = System.getenv("OURCAFE_KEYSTORE_PASSWORD")
android {
    namespace = "com.ourcafe.planner"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.ourcafe.planner"
        minSdk = 26
        targetSdk = 35
        versionCode = 18
        versionName = "1.2.0"
        buildConfigField("boolean", "FIREBASE_ENABLED", firebaseConfigured.toString())
    }
    signingConfigs {
        if (signingFile.exists() && !signingPassword.isNullOrBlank()) {
            create("ourcafeCi") {
                storeFile = signingFile
                storePassword = signingPassword
                keyAlias = "ourcafe"
                keyPassword = signingPassword
            }
        }
    }
    buildTypes {
        getByName("debug") {
            if (signingConfigs.findByName("ourcafeCi") != null) signingConfig = signingConfigs.getByName("ourcafeCi")
        }
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
}
dependencies {
    val bom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(bom)
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
}
