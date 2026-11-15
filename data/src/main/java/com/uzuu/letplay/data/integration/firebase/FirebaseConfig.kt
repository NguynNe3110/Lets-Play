package com.uzuu.letplay.data.integration.firebase

/**
 * Configuration for Firebase integration with explicit variant separation (e.g. dev vs prod project).
 */
data class FirebaseConfig(
    val applicationId: String,
    val projectId: String,
    val apiKey: String,
    val databaseUrl: String? = null,
    val storageBucket: String? = null
) {
    init {
        require(applicationId.isNotBlank()) { "Firebase applicationId must not be blank" }
        require(projectId.isNotBlank()) { "Firebase projectId must not be blank" }
        require(apiKey.isNotBlank()) { "Firebase apiKey must not be blank" }
    }

    val isDev: Boolean get() = projectId.contains("dev", ignoreCase = true) || applicationId.contains("dev", ignoreCase = true)
    val isProd: Boolean get() = !isDev
}
