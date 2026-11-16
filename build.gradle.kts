plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
}

tasks.register("verifyModuleBoundaries") { //task kiểm tra dependencies (nó được nối đến các task check và được CI gọi rõ ràng)
    group = "verification"
    description = "Checks the allowed project dependencies of the architecture layers."
    doLast {
        val allowed = mapOf( // thiết lập chốt chặn kiểm tra, không được phép import vào file gradle (chưa nói đến code)
            ":app" to setOf(":domain", ":data", ":shared", ":resources"),
            ":data" to setOf(":domain", ":shared"),
            ":domain" to setOf(":shared"), // Domain CHỈ ĐƯỢC PHỤ THUỘC shared, cấm import data hay app!
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
