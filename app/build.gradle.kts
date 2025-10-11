plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.jewelleryapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.jewelleryapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Add signing configurations
    signingConfigs {
        create("release") {
            storeFile  =file("my-app-release.keystore") // Put your keystore file in the app/ folder
            storePassword = "ishank"   // Replace with your actual password
            keyAlias = "key0"                 // Replace with your key alias
            keyPassword=  "ishank"       // Replace with your key password
        }
    }


    // Add this packaging block to exclude the problematic META-INF file
    packaging {
        resources {
            excludes += "META-INF/androidx.emoji2_emoji2.version"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Add signing config to release build
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Core Android dependencies
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")

    // Jetpack Compose
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.material:material:1.5.4")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.4")
    implementation("androidx.activity:activity-compose:1.8.2")
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.4")

    // ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.6")

    implementation("io.coil-kt:coil-compose:2.5.0")

    // For Google Sign-In (if you're actually implementing it)
    implementation(libs.play.services.auth)
    implementation(libs.androidx.material.icons.extended)


    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    //Dependencies from denis course
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    //firebase storage
    implementation (libs.firebase.storage.ktx);


    implementation(libs.firebase.analytics)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)


    implementation ("com.google.firebase:firebase-dynamic-links-ktx:22.1.0")
    implementation (libs.firebase.analytics.ktx)
    implementation (libs.firebase.messaging)


    implementation("androidx.datastore:datastore-preferences:1.1.7")

    // In build.gradle (Module: app) - Add these dependencies
    implementation (libs.zoomable)
    implementation ("net.engawapg.lib:zoomable:2.7.0")

    implementation ("com.google.accompanist:accompanist-swiperefresh:0.32.0");

    // ExoPlayer for video streaming
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.2.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
    implementation("androidx.media3:media3-common:1.2.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}