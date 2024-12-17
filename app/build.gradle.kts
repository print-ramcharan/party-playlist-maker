plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt") // Added for annotation processing
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.partyplaylist"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.partyplaylist"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders.putAll(mapOf(
            "redirectSchemeName" to "http",
            "redirectHostName" to "localhost",
            "redirectPath" to "/callback"
        ))

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX Libraries
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.fragment:fragment:1.5.5")
    implementation("androidx.viewpager:viewpager:1.0.0")

    // Compose Libraries
    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Material Design
    implementation("com.google.android.material:material:1.12.0")

    // Material Drawer
    implementation("com.mikepenz:materialdrawer:8.1.7")

    // Iconics
    implementation("com.mikepenz:iconics-core:5.2.8")
    implementation("com.mikepenz:iconics-views:5.2.8")
    implementation("com.mikepenz:fontawesome-typeface:5.9.0.0-kotlin@aar")

    // Spotify Auth
    implementation("com.spotify.android:auth:2.1.1")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.11")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.1")

    // Gson
    implementation("com.google.code.gson:gson:2.9.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.1.1"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-firestore:24.5.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.15.1") {
        exclude(group = "com.github.bumptech.glide", module = "annotations")
    }
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("com.android.volley:volley:1.2.1")
    implementation("androidx.test.espresso:espresso-core:3.6.1")
    kapt("com.github.bumptech.glide:compiler:4.15.1")

    //notificationcompat
    implementation("androidx.core:core:1.7.0")
    //musicplayer
    implementation ("com.google.android.exoplayer:exoplayer:2.16.1")

    //mediastyle
    implementation("androidx.media:media:1.7.0")
    implementation("androidx.core:core:1.9.0")

    implementation ("androidx.recyclerview:recyclerview:1.2.1")
    implementation ("com.github.bumptech.glide:glide:4.14.2")
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation ("androidx.compose.ui:ui-test-junit4:1.5.0")

    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation ("com.squareup.moshi:moshi:1.12.0")
    implementation ("com.squareup.moshi:moshi-kotlin:1.12.0")

    implementation ("com.squareup.picasso:picasso:2.8")

}
