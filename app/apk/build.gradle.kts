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
    implementation(libs.foundation.layout)
    implementation(libs.navigation.runtime.ktx)
    implementation(libs.runtime)
    implementation(libs.ui.text)
    coreLibraryDesugaring(libs.jdk.libs)

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
    implementation(libs.iconsax.compose)
    implementation(libs.material.kolor)
    implementation(libs.haze.jetpack.compose)
    implementation(libs.backdrop)

    // Navigation
    implementation(libs.navigation.compose)

    // UI assets / network
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
}
