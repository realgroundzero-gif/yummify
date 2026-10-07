import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Signing secrets stay outside Git: local.properties or environment variables.
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use(::load)
}
fun secret(key: String): String? = localProperties.getProperty(key)
    ?: System.getenv(key.uppercase().replace('.', '_'))
val releaseKeystore = file(secret("yummify.storeFile") ?: "release.jks")
val releaseSigningAvailable = releaseKeystore.isFile && secret("yummify.storePassword") != null

android {
    namespace = "de.yummify.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.yummify.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (releaseSigningAvailable) create("release") {
            storeFile = releaseKeystore
            storePassword = secret("yummify.storePassword")
            keyAlias = secret("yummify.keyAlias") ?: "yummify"
            keyPassword = secret("yummify.keyPassword") ?: secret("yummify.storePassword")
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["appLabel"] = "Yummify"
        }
        // Installs next to the regular app, so test builds never replace its data or signature.
        create("preview") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".preview"
            versionNameSuffix = "-preview"
            manifestPlaceholders["appLabel"] = "Yummify Preview"
            matchingFallbacks += listOf("debug")
        }
        release {
            manifestPlaceholders["appLabel"] = "Yummify"
            isMinifyEnabled = false
            if (releaseSigningAvailable) signingConfig = signingConfigs.getByName("release")
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions { unitTests.isIncludeAndroidResources = true }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    
    implementation(libs.coil.compose)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.play.services.code.scanner)
    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
}
