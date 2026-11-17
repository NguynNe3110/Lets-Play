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

rootProject.name = "LetsPlay"

// Layer-based multi-module application. Dependencies are declared in each module.
//khai báo các module
include(":shared")
include(":resources")
include(":domain")
include(":data")
include(":composition")
include(":app")
