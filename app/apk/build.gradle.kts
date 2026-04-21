plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    kotlin("plugin.parcelize")
}

setupMainApk()

android {
    buildFeatures {
        compose = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    defaultConfig {
        proguardFile("proguard-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }
}

dependencies {
    implementation(project(":core"))
    coreLibraryDesugaring(libs.jdk.libs)

    implementation(libs.appcompat)
    implementation(libs.material)

    // Compose
    implementation(libs.compose.ui)
    implementation(libs.foundation)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.material3)

    // Navigation
    implementation(libs.navigation.compose)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.navigationevent.compose)
    implementation(libs.lifecycle.viewmodel.navigation3)

    // UI assets / network
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
}
