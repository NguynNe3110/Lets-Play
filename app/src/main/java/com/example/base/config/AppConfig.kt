package com.example.base.config

import com.example.base.BuildConfig

enum class AppEnvironment(val id: String) {
    DEV("dev"),
    PROD("prod");

    val isDev: Boolean get() = this == DEV
    val isProd: Boolean get() = this == PROD

    companion object {
        fun from(value: String): AppEnvironment =
            entries.firstOrNull { it.id.equals(value.trim(), ignoreCase = true) }
                ?: throw IllegalArgumentException(
                    "Unknown environment: '$value'. Expected one of: ${entries.joinToString { it.id }}"
                )
    }
}

data class AppConfig(
    val environment: AppEnvironment,
    val apiBaseUrl: String,
    val isDebug: Boolean = false,
) {
    init {
        require(apiBaseUrl.isNotBlank()) { "API base URL must not be blank" }
        require(apiBaseUrl.startsWith("http://") || apiBaseUrl.startsWith("https://")) {
            "API base URL must have http or https scheme: $apiBaseUrl"
        }
        require(apiBaseUrl.endsWith("/")) {
            "API base URL must end with a trailing slash: $apiBaseUrl"
        }
    }

    companion object {
        fun fromBuildConfig(): AppConfig {
            val rawUrl = BuildConfig.API_BASE_URL.trim()
            val normalizedUrl = if (rawUrl.endsWith("/")) rawUrl else "$rawUrl/"
            return AppConfig(
                environment = AppEnvironment.from(BuildConfig.ENVIRONMENT),
                apiBaseUrl = normalizedUrl,
                isDebug = BuildConfig.DEBUG,
            )
        }
    }
}
