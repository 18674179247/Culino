@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    wasmJs { browser() }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":common:util"))
            implementation(project(":framework:network"))
            implementation(libs.kotlinx.coroutines.core)
        }

        val nonWasmMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.datastore.preferences)
            }
        }
        jvmMain.get().dependsOn(nonWasmMain)
        iosMain.get().dependsOn(nonWasmMain)

        val wasmJsMain by getting {
            dependencies {
                implementation(libs.kotlinx.browser)
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
