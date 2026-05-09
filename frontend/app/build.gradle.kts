@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions { jvmTarget = "17" }
        }
    }

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":common:util"))
            implementation(project(":common:model"))
            implementation(project(":common:api"))
            implementation(project(":common:ui"))
            implementation(project(":framework:network"))
            implementation(project(":framework:storage"))
            implementation(project(":feature:user"))
            implementation(project(":feature:recipe"))
            implementation(project(":feature:social"))
            implementation(project(":feature:tool"))
            implementation(project(":feature:ingredient"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.animation)
            implementation(libs.navigation.compose)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlin.inject.runtime)
            implementation(libs.napier)
            implementation(libs.ktor.client.core)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        val wasmJsMain by getting {
            dependencies {
                implementation(libs.kotlinx.browser)
            }
        }
    }
}

android {
    namespace = "com.culino.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.culino.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            isDebuggable = true
            buildConfigField("String", "API_BASE_URL", "\"http://127.0.0.1:3000/api/v1/\"")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "API_BASE_URL", "\"https://your-prod-domain.com/api/v1/\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.kotlin.inject.compiler)
}
