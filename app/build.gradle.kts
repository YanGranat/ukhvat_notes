import java.util.Properties
import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "1.9.21-1.0.15"
    // Removed Hilt plugin and kapt for Koin migration (Koin uses runtime DI, no annotation processing)
}

android {
    namespace = "com.ukhvat.notes"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ukhvat.notes"
        minSdk = 24
        targetSdk = 34
        versionCode = 3
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Read signing config from local signing.properties (not in VCS)
    val signingPropsFile = rootProject.file("signing.properties")
    val signingProps = Properties().apply {
        if (signingPropsFile.exists()) signingPropsFile.inputStream().use { load(it) }
    }
    val hasSigningProps = signingPropsFile.exists()

    signingConfigs {
        create("release") {
            if (hasSigningProps) {
                keyAlias = signingProps.getProperty("keyAlias")
                keyPassword = signingProps.getProperty("keyPassword")
                val storePath = signingProps.getProperty("storeFile")
                if (storePath != null) {
                    val store = File(storePath)
                    storeFile = if (store.isAbsolute) store else rootProject.file(storePath)
                }
                storePassword = signingProps.getProperty("storePassword")
            }
        }
    }

    buildTypes {
        debug {
            // TEMP: commented to test if we're building the right version
            // applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasSigningProps) {
                signingConfig = signingConfigs.getByName("release")
            }
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
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.7"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")
    
    // AppCompat for localization support
    implementation("androidx.appcompat:appcompat:1.6.1")
    
    // Koin - replacing Hilt for better startup performance
    implementation("io.insert-koin:koin-android:4.0.0")
    implementation("io.insert-koin:koin-androidx-compose:4.0.0")
    
    // Room - using KSP instead of kapt for better performance
    implementation("androidx.room:room-runtime:2.6.1")  
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    


    // ViewModels
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    
    // Document file support for folder access
    implementation("androidx.documentfile:documentfile:1.0.1")

    // Network client for AI integrations
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
} 