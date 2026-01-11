plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.humblecoders.jewelleryapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.humblecoders.jewelleryapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"

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
    implementation(libs.androidx.core.ktx.v1120)
    implementation(libs.androidx.appcompat)
    implementation(libs.material);

    // Jetpack Compose - Updated to 1.6.8 for beyondBoundsItemCount support
    implementation("androidx.compose.ui:ui:1.10.0")
    implementation("androidx.compose.material:material:1.10.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.10.0")
    implementation("androidx.compose.foundation:foundation:1.10.0")
    implementation(libs.androidx.activity.compose.v182)
    debugImplementation("androidx.compose.ui:ui-tooling:1.10.0")

    // ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.9.6")

    // Coil for image loading with decoders
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-gif:2.7.0")
    implementation("io.coil-kt:coil-video:2.7.0")

    // For Google Sign-In (if you're actually implementing it)
    implementation(libs.play.services.auth)
    implementation(libs.androidx.material.icons.extended)


    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

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


    implementation ("com.google.firebase:firebase-dynamic-links-ktx:22.1.0");
    implementation (libs.firebase.analytics.ktx)
    implementation (libs.firebase.messaging)


    implementation("androidx.datastore:datastore-preferences:1.2.0")

    // In build.gradle (Module: app) - Add these dependencies
    implementation (libs.zoomable)
    implementation (libs.zoomable.v270)

    implementation ("com.google.accompanist:accompanist-swiperefresh:0.36.0");

    // ExoPlayer for video streaming
    implementation("androidx.media3:media3-exoplayer:1.9.0")
    implementation("androidx.media3:media3-exoplayer-dash:1.9.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.9.0")
    implementation("androidx.media3:media3-ui:1.9.0")
    implementation("androidx.media3:media3-common:1.9.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core);
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}