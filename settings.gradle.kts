pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "MahavtaarPanchanga"

include(":app")
include(":core")
include(":core-ui")
include(":core-database")
include(":core-location")
include(":astronomy-engine")
include(":panchanga-domain")
include(":panchanga-data")
include(":feature-home")
include(":feature-daily-panchanga")
include(":feature-calendar")
include(":feature-hora")
include(":feature-swara-yoga")
include(":feature-settings")
