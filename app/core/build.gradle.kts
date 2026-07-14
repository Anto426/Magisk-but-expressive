plugins {
    alias(libs.plugins.android.library)
    kotlin("plugin.parcelize")
    alias(libs.plugins.moshix)
    alias(libs.plugins.ksp)
    alias(libs.plugins.wire)
}

setupCoreLib()

ksp {
    arg("room.generateKotlin", "true")
}

wire {
    kotlin {}
}

android {
    namespace = "com.topjohnwu.magisk.core"

    defaultConfig {
        buildConfigField("String", "APP_PACKAGE_NAME", "\"${Config.applicationId}\"")
        buildConfigField("int", "APP_VERSION_CODE", "${Config.versionCode}")
        buildConfigField("String", "APP_VERSION_NAME", "\"${Config.version}\"")
        buildConfigField("int", "MBE_VERSION_CODE", "${Config.mbeVersionCode}")
        buildConfigField("String", "MBE_VERSION_NAME", "\"${Config.mbeVersionName}\"")
        buildConfigField("String", "UPDATE_URL", "\"${Config.updateUrl}\"")
        buildConfigField("int", "STUB_VERSION", Config.stubVersion)
        consumerProguardFile("proguard-rules.pro")
    }

    buildFeatures {
        aidl = true
        buildConfig = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    api(project(":shared"))
    coreLibraryDesugaring(libs.jdk.libs)

    // Public core APIs
    api(libs.markwon.core)
    api(libs.timber)

    // Archive / crypto
    implementation(libs.bcpkix)
    implementation(libs.commons.compress)
    implementation(libs.xz)

    // Generated models
    implementation(libs.wire.runtime)

    // Root services
    api(libs.libsu.core)
    api(libs.libsu.nio)
    api(libs.libsu.service)

    // Network
    implementation(libs.okhttp)
    implementation(libs.okhttp.dnsoverhttps)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.retrofit.scalars)

    // Database
    implementation(libs.room.ktx)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)

    // AndroidX
    implementation(libs.activity)
    implementation(libs.collection.ktx)
    implementation(libs.core.ktx)
    implementation(libs.core.splashscreen)
    implementation(libs.profileinstaller)

    // We also implement all our tests in this module.
    // However, we don't want to bundle test dependencies.
    // That's why we make it compileOnly.
    compileOnly(libs.test.junit)
    compileOnly(libs.test.uiautomator)
    testImplementation(libs.junit4)
}
