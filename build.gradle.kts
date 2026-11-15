plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
}

tasks.register("verifyModuleBoundaries") {
    group = "verification"
    description = "Checks the allowed project dependencies of the architecture layers."
    doLast {
        val allowed = mapOf(
            ":app" to setOf(":domain", ":data", ":shared", ":resources"),
            ":data" to setOf(":domain", ":shared"),
            ":domain" to setOf(":shared"),
            ":resources" to emptySet(),
            ":shared" to emptySet()
        )
        subprojects.forEach { module ->
            val permitted = checkNotNull(allowed[module.path]) { "Define boundaries for ${module.path}" }
            module.configurations.forEach { configuration ->
                configuration.dependencies.withType<org.gradle.api.artifacts.ProjectDependency>().forEach { dependency ->
                    val target = dependency.path
                    check(target == module.path || target in permitted) {
                        "Forbidden dependency: ${module.path} -> $target (${configuration.name})"
                    }
                }
            }
        }
    }
}

subprojects {
    tasks.matching { it.name == "check" }.configureEach {
        dependsOn(rootProject.tasks.named("verifyModuleBoundaries"))
    }
}
