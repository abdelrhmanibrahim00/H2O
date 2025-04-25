import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services) // ✅ Corrected way to apply Google Services
   // id("kotlin-kapt")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    id("com.google.devtools.ksp") version "1.9.0-1.0.13" // Use the version compatible with your Kotlin version


}

android {
    buildFeatures {
        buildConfig = true  // ✅ Enable BuildConfig fields
    }
    namespace = "com.h2o.store"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.h2o.store"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        val localPropertiesFile = rootProject.file("local.properties")
        val localProperties = Properties()

        if (localPropertiesFile.exists()) {
            localProperties.load(FileInputStream(localPropertiesFile))
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // ✅ Use Firebase BoM correctly
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.androidx.runner)
    implementation(libs.androidx.runner)
    implementation(libs.androidx.espresso.core)
    implementation(libs.play.services.location)
    implementation(libs.places)
    implementation(libs.androidx.constraintlayout.compose.android)
    implementation(libs.androidx.foundation.layout.android)
    implementation(libs.androidx.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // View Model
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")

    // NavHost
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // Retrofit dependencies
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // OkHttp for logging (Optional, useful for debugging API calls)
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")


    //GMaps
    implementation ("com.google.maps.android:maps-compose:2.15.0")
    implementation ("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Compose dependencies
    implementation ("androidx.compose.material:material:1.5.4")
    implementation ("androidx.compose.material:material-icons-extended:1.5.4")

// Coil dependency
    implementation("io.coil-kt:coil-compose:2.5.0")
// Google places
    implementation ("com.google.android.libraries.places:places:3.3.0")

    implementation("com.squareup.okhttp3:okhttp:4.9.3")


    val room = "2.6.1"

    // Room
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    //kapt("androidx.room:room-compiler:$room")
    ksp("androidx.room:room-compiler:$room") // Add this line


    // Coroutines
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    // WebView for payment gateway
    implementation ("androidx.webkit:webkit:1.6.0")

    // Gson for JSON serialization/deserialization
    implementation ("com.google.code.gson:gson:2.10.1")

    // Update to the latest versions
//    implementation ("com.google.android.gms:play-services-auth:20.7.0")
//    implementation ("com.google.firebase:firebase-auth:22.3.0")
//    implementation ("com.google.firebase:firebase-firestore:24.10.0")
    // ✅ Use Firebase BoM correctly
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)



}
