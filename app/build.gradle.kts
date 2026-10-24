plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}
android {
    namespace = "pt.tiago.ducknight"
    // API 37.2: the same platform the series already builds on F-Droid.
    compileSdk { version = release(37) { minorApiLevel = 2 } }
    defaultConfig {
        applicationId = "pt.tiago.ducknight"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // Release signing comes from environment variables (CI) or ~/.gradle/gradle.properties, never
    // from the repository. Without credentials the release stays unsigned, which is how F-Droid builds it.
    val releaseStoreFile = providers.environmentVariable("SIGNING_KEYSTORE_FILE")
        .orElse(providers.gradleProperty("duckNight.release.storeFile")).orNull
    val releaseStorePassword = providers.environmentVariable("SIGNING_PASSWORD")
        .orElse(providers.gradleProperty("duckNight.release.storePassword")).orNull
    val releaseKeyAlias = providers.environmentVariable("SIGNING_KEY_ALIAS")
        .orElse(providers.gradleProperty("duckNight.release.keyAlias")).orNull
    val hasReleaseSigning = releaseStoreFile != null && releaseStorePassword != null && releaseKeyAlias != null
    signingConfigs {
        if (hasReleaseSigning) create("release") {
            storeFile = file(releaseStoreFile!!)
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = providers.gradleProperty("duckNight.release.keyPassword").orNull ?: releaseStorePassword
        }
    }
    buildTypes {
        release {
            // R8 output is deterministic for the same inputs and keeps the APK small.
            isMinifyEnabled = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Keep the git revision out of the APK so release builds are reproducible from source.
            vcsInfo.include = false
        }
    }
    // The dependency metadata block is encrypted for Google Play only; F-Droid rejects it.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    // Keep native libraries unstripped so the APK is identical with or without the NDK installed.
    packaging { jniLibs { keepDebugSymbols += "**/*.so" } }
    buildFeatures { compose = true; buildConfig = true }
    sourceSets["main"].assets.directories.add(layout.buildDirectory.dir("generated/duckAssets").get().asFile.path)
}
dependencies {
    implementation(project(":core"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.runtime)
    implementation(libs.datastore)
    implementation(libs.serialization.json)
    testImplementation(libs.junit)
}

// The packaged catalog is exported by :leveltool once content exists.
if (rootProject.file("content/catalog.json").exists()) {
    tasks.named("preBuild") { dependsOn(":leveltool:exportGameAssets") }
}
