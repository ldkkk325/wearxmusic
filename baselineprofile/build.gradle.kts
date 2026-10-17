plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
}

/**
 * Generates the release baseline profile for :app.
 *
 * A macrobenchmark that launches the app, walks into the library and flings the list — the
 * TransformingLazyColumn scaling path and first composition are what the profile is meant to
 * pre-compile. Runs only against release variants, and only with a watch (or emulator) attached:
 *
 *     ./gradlew :baselineprofile:generateReleaseBaselineProfile
 */
android {
    namespace = "com.wearx.music.baselineprofile"
    compileSdk = 37

    defaultConfig {
        minSdk = 30
        targetSdk = 36
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    targetProjectPath = ":app"

    experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
    implementation(libs.junit)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}

androidComponents {
    // The baselineprofile plugin gives :app a dedicated `benchmark` build type (release config,
    // profileable, signed for benchmarking). That is the variant whose code path the profile
    // has to warm, so the generator runs against it and nothing else.
    beforeVariants(selector().all()) { variantBuilder ->
        variantBuilder.enable = variantBuilder.name == "benchmarkRelease"
    }
}
