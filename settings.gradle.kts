pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "kotlin_mvvm_base"

// Layer-based multi-module application. Dependencies are declared in each module.
include(":shared")
include(":resources")
include(":domain")
include(":data")
include(":app")
