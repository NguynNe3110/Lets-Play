package com.uzuu.letplay.data.integration.supabase

import com.uzuu.letplay.shared.result.Result

/**
 * Adapter contract for Supabase authentication operations.
 */
interface SupabaseAuthAdapter {
    suspend fun signInWithPassword(credentials: SupabaseAuthCredentials): Result<SupabaseSession>
    suspend fun signUp(credentials: SupabaseAuthCredentials): Result<SupabaseSession>
    suspend fun refreshToken(refreshToken: String): Result<SupabaseSession>
    suspend fun signOut(): Result<Unit>
    fun currentSession(): SupabaseSession?
}

/**
 * Adapter contract for Supabase PostgREST database CRUD operations.
 */
interface SupabaseDatabaseAdapter {
    suspend fun <T : Any> select(query: SupabaseQuery, mapper: (Map<String, Any?>) -> T): Result<List<T>>
    suspend fun <T : Any> insert(table: String, data: Map<String, Any?>, mapper: (Map<String, Any?>) -> T): Result<T>
    suspend fun <T : Any> update(table: String, id: String, data: Map<String, Any?>, mapper: (Map<String, Any?>) -> T): Result<T>
    suspend fun delete(table: String, id: String): Result<Unit>
}

/**
 * Unified Supabase client adapter combining Auth and Database capabilities.
 */
interface SupabaseAdapter : SupabaseAuthAdapter, SupabaseDatabaseAdapter
