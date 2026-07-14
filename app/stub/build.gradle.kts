plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.lsparanoid)
}

lsparanoid {
    seed = if (RAND_SEED != 0) RAND_SEED else null
    includeDependencies = true
    classFilter = { true }
}

android {
    namespace = "com.topjohnwu.magisk"
    enableKotlin = false

    defaultConfig {
        applicationId = Config.applicationId
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "UPDATE_URL", "\"${Config.updateUrl}\"")
        buildConfigField("int", "STUB_VERSION", Config.stubVersion)
    }

    buildTypes {
        release {
            proguardFiles("proguard-rules.pro")
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

setupStubApk()

dependencies {
    implementation(project(":shared"))
}
