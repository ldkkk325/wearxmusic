import java.util.Properties

plugins {
    // AGP 9 ships built-in Kotlin support, so `org.jetbrains.kotlin.android` must NOT be applied.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// The release signing key is deliberately NOT in this repository. `keystore.properties` sits in the
// project root, is gitignored, and points at a .jks kept outside the tree. When it is absent — a
// fresh clone, CI, any other machine — the release build is simply left unsigned instead of failing,
// and can be signed afterwards by hand.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.wearx.music"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.wearx.music"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        // Registered only when there is a key to register, so `findByName` below returns null on a
        // machine without one rather than the build dying on a missing file.
        if (keystoreProperties.getProperty("storeFile") != null) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                // v3 as well as v2: it costs nothing here and it is what makes a future key
                // rotation possible without every installed copy being orphaned.
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.service)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.common)

    implementation(libs.coil.compose)

    implementation(libs.androidx.palette.ktx)
    implementation(libs.material.components)

    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
