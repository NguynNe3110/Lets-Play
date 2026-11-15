package com.example.base.data.integration.rest

/**
 * Configuration contract for REST environment endpoints (e.g. dev vs prod).
 * Validates endpoint scheme and trailing slash invariants.
 */
data class RestEnvironmentConfig(
    val environment: String,
    val baseUrl: String,
    val connectTimeoutMs: Long = 5_000,
    val readTimeoutMs: Long = 5_000
) {
    init {
        require(baseUrl.isNotBlank()) { "API base URL must not be blank" }
        require(baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) {
            "API base URL must have http or https scheme: $baseUrl"
        }
        require(baseUrl.endsWith("/")) {
            "API base URL must end with a trailing slash: $baseUrl"
        }
    }

    val isDev: Boolean get() = environment.equals("dev", ignoreCase = true)
    val isProd: Boolean get() = environment.equals("prod", ignoreCase = true)
}
