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

rootProject.name = "culino-frontend"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// framework 层:纯技术基建
include(":framework:network")
include(":framework:storage")
include(":framework:media")

// common 层:跨 feature 共享
include(":common:util")
include(":common:model")
include(":common:api")
include(":common:ui")

// feature 层:业务功能
include(":feature:user")
include(":feature:recipe")
include(":feature:social")
include(":feature:ingredient")
include(":feature:tool")

// 应用壳
include(":app")

// 物理路径映射:所有业务/通用/框架模块都位于 src/ 下,app 保持独立
project(":framework:network").projectDir = file("src/framework/network")
project(":framework:storage").projectDir = file("src/framework/storage")
project(":framework:media").projectDir = file("src/framework/media")
project(":common:util").projectDir = file("src/common/util")
project(":common:model").projectDir = file("src/common/model")
project(":common:api").projectDir = file("src/common/api")
project(":common:ui").projectDir = file("src/common/ui")
project(":feature:user").projectDir = file("src/feature/user")
project(":feature:recipe").projectDir = file("src/feature/recipe")
project(":feature:social").projectDir = file("src/feature/social")
project(":feature:ingredient").projectDir = file("src/feature/ingredient")
project(":feature:tool").projectDir = file("src/feature/tool")
