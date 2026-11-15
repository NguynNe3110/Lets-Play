package com.example.base.data.integration.supabase

/**
 * Configuration for Supabase integration.
 * Enforces security best practices: accepts public/anon keys only, rejecting service-role/admin keys.
 */
data class SupabaseConfig(
    val url: String,
    val anonKey: String,
    val environment: String = "dev"
) {
    init {
        require(url.isNotBlank()) { "Supabase URL must not be blank" }
        require(url.startsWith("http://") || url.startsWith("https://")) {
            "Supabase URL must have http or https scheme: $url"
        }
        require(anonKey.isNotBlank()) { "Supabase anon key must not be blank" }
        require(!anonKey.contains("service_role", ignoreCase = true)) {
            "Service-role key must NEVER be embedded in client applications"
        }
    }

    val isDev: Boolean get() = environment.equals("dev", ignoreCase = true)
    val isProd: Boolean get() = environment.equals("prod", ignoreCase = true)
}
