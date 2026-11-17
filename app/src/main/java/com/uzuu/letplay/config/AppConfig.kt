package com.uzuu.letplay.config

import com.uzuu.letplay.BuildConfig
import java.net.URI

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
        require(apiBaseUrl.endsWith("/")) {
            "API base URL must end with a trailing slash: $apiBaseUrl"
        }

        val uri = try {
            val parsed = URI(apiBaseUrl)
            require(parsed.isAbsolute) { "API base URL must be absolute: $apiBaseUrl" }
            parsed
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException("API base URL is not a valid URI: $apiBaseUrl", e)
        }

        val scheme = uri.scheme?.lowercase()
        require(scheme == "http" || scheme == "https") {
            "API base URL must have http or https scheme: $apiBaseUrl"
        }

        val host = uri.host ?: uri.authority?.substringAfter('@')?.substringBefore(':')
        require(!host.isNullOrBlank()) {
            "API base URL must have a valid non-empty host: $apiBaseUrl"
        }

        if (environment.isProd && !isDebug) {
            val isLocal = host.equals("localhost", ignoreCase = true) ||
                host == "127.0.0.1" ||
                host == "10.0.2.2"
            require(scheme == "https" || isLocal) {
                "Production release requires HTTPS API base URL: $apiBaseUrl"
            }
        }
    }

    companion object {
        fun normalizeUrl(rawUrl: String): String {
            val trimmed = rawUrl.trim()
            require(trimmed.isNotBlank()) { "API base URL must not be blank" }
            return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        }

        fun resolveBaseUrl(
            key: String,
            propertyLookup: (String) -> String? = { System.getProperty(it) },
            envLookup: (String) -> String? = { System.getenv(it) },
            defaultUrl: String = "https://jsonplaceholder.typicode.com/"
        ): String {
            val prop = propertyLookup(key)?.trim()
            if (!prop.isNullOrEmpty()) return normalizeUrl(prop)
            val env = envLookup(key)?.trim()
            if (!env.isNullOrEmpty()) return normalizeUrl(env)
            return normalizeUrl(defaultUrl)
        }

        // Hàm này biến cấu hình được sinh thành object Kotlin mà app sử dụng.
        fun fromBuildConfig(): AppConfig {
            val normalizedUrl = normalizeUrl(BuildConfig.API_BASE_URL)
            return AppConfig(
                environment = AppEnvironment.from(BuildConfig.ENVIRONMENT),
                apiBaseUrl = normalizedUrl,
                isDebug = BuildConfig.DEBUG,
            )
        }
    }
}
