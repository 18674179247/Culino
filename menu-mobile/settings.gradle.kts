pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "menu-mobile"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":core:common")
include(":core:model")
include(":core:network")
include(":core:data")
include(":core:ui")
include(":feature:user")
include(":composeApp")
