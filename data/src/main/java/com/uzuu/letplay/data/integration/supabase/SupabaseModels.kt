package com.uzuu.letplay.data.integration.supabase

/**
 * Representation of a Supabase user.
 */
data class SupabaseUser(
    val id: String,
    val email: String?,
    val role: String? = "authenticated"
)

/**
 * Representation of an active Supabase session with JWT access token and refresh token.
 */
data class SupabaseSession(
    val accessToken: String,
    val refreshToken: String?,
    val user: SupabaseUser,
    val expiresIn: Long = 3600
)

/**
 * Credentials for email/password authentication.
 */
data class SupabaseAuthCredentials(
    val email: String,
    val password: String
)

/**
 * Comparison operator for PostgREST filter clauses.
 */
enum class SupabaseFilterOperator(val symbol: String) {
    EQ("eq"),
    NEQ("neq"),
    GT("gt"),
    GTE("gte"),
    LT("lt"),
    LTE("lte"),
    LIKE("like"),
    IN("in")
}

/**
 * Single column filter clause.
 */
data class SupabaseFilter(
    val column: String,
    val operator: SupabaseFilterOperator,
    val value: Any
)

/**
 * Structured query for PostgREST tables.
 */
data class SupabaseQuery(
    val table: String,
    val columns: String = "*",
    val filters: List<SupabaseFilter> = emptyList(),
    val limit: Int? = null,
    val offset: Int? = null,
    val orderColumn: String? = null,
    val ascending: Boolean = true
)
