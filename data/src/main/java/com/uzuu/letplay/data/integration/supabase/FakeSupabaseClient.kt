package com.uzuu.letplay.data.integration.supabase

import com.uzuu.letplay.domain.usecase.base.BaseOutput
import com.uzuu.letplay.shared.result.Result
import java.util.UUID

/**
 * In-memory Fake Supabase client for verifying auth, PostgREST CRUD operations,
 * security rules, and error mappings.
 */
class FakeSupabaseClient(
    val config: SupabaseConfig = SupabaseConfig(
        url = "https://example.supabase.co",
        anonKey = "anon-key-dev-demo",
        environment = "dev"
    ),
    private val errorMapper: SupabaseErrorMapper = SupabaseErrorMapper()
) : SupabaseAdapter {

    // Auth state
    private val registeredUsers: MutableMap<String, Pair<SupabaseUser, String>> = mutableMapOf() // email -> (User, password)
    private var activeSession: SupabaseSession? = null

    // Database tables: table_name -> map of (id -> row_map)
    private val tables: MutableMap<String, MutableMap<String, Map<String, Any?>>> = mutableMapOf()

    // Fault injection
    var simulatedNetworkError: Throwable? = null
    var simulateRlsViolation: Boolean = false
    var simulateExpiredRefreshToken: Boolean = false

    fun seedUser(email: String, password: String, id: String = UUID.randomUUID().toString()): SupabaseUser {
        val user = SupabaseUser(id = id, email = email)
        registeredUsers[email] = user to password
        return user
    }

    fun seedRows(table: String, rows: List<Map<String, Any?>>) {
        val tableMap = tables.getOrPut(table) { mutableMapOf() }
        for (row in rows) {
            val id = row["id"]?.toString() ?: UUID.randomUUID().toString()
            tableMap[id] = row + ("id" to id)
        }
    }

    override suspend fun signInWithPassword(credentials: SupabaseAuthCredentials): Result<SupabaseSession> {
        simulatedNetworkError?.let { return Result.Failure(errorMapper.mapThrowable(it)) }

        val userEntry = registeredUsers[credentials.email]
        if (userEntry == null || userEntry.second != credentials.password) {
            return Result.Failure(
                errorMapper.mapAuthError(
                    statusCode = 400,
                    errorCode = "invalid_credentials",
                    message = "Invalid email or password"
                )
            )
        }

        val session = SupabaseSession(
            accessToken = "access-token-${UUID.randomUUID()}",
            refreshToken = "refresh-token-${UUID.randomUUID()}",
            user = userEntry.first
        )
        activeSession = session
        return Result.Success(session)
    }

    override suspend fun signUp(credentials: SupabaseAuthCredentials): Result<SupabaseSession> {
        simulatedNetworkError?.let { return Result.Failure(errorMapper.mapThrowable(it)) }

        if (registeredUsers.containsKey(credentials.email)) {
            return Result.Failure(
                errorMapper.mapAuthError(
                    statusCode = 400,
                    errorCode = "user_already_exists",
                    message = "User already registered"
                )
            )
        }

        val newUser = SupabaseUser(id = UUID.randomUUID().toString(), email = credentials.email)
        registeredUsers[credentials.email] = newUser to credentials.password
        val session = SupabaseSession(
            accessToken = "access-token-${UUID.randomUUID()}",
            refreshToken = "refresh-token-${UUID.randomUUID()}",
            user = newUser
        )
        activeSession = session
        return Result.Success(session)
    }

    override suspend fun refreshToken(refreshToken: String): Result<SupabaseSession> {
        simulatedNetworkError?.let { return Result.Failure(errorMapper.mapThrowable(it)) }

        if (simulateExpiredRefreshToken || refreshToken.isBlank()) {
            activeSession = null
            return Result.Failure(
                errorMapper.mapAuthError(
                    statusCode = 401,
                    errorCode = "token_expired",
                    message = "Refresh token expired"
                )
            )
        }

        val currentUser = activeSession?.user ?: SupabaseUser(UUID.randomUUID().toString(), "user@example.com")
        val newSession = SupabaseSession(
            accessToken = "refreshed-access-${UUID.randomUUID()}",
            refreshToken = "refreshed-refresh-${UUID.randomUUID()}",
            user = currentUser
        )
        activeSession = newSession
        return Result.Success(newSession)
    }

    override suspend fun signOut(): Result<Unit> {
        simulatedNetworkError?.let { return Result.Failure(errorMapper.mapThrowable(it)) }
        activeSession = null
        return Result.Success(Unit)
    }

    override fun currentSession(): SupabaseSession? = activeSession

    override suspend fun <T : Any> select(
        query: SupabaseQuery,
        mapper: (Map<String, Any?>) -> T
    ): Result<List<T>> {
        simulatedNetworkError?.let { return Result.Failure(errorMapper.mapThrowable(it)) }
        if (simulateRlsViolation) {
            return Result.Failure(
                errorMapper.mapPostgrestError(
                    statusCode = 403,
                    errorCode = "42501",
                    message = "new row violates row-level security policy for table '${query.table}'"
                )
            )
        }

        val tableRows = tables[query.table]?.values?.toList() ?: emptyList()
        var filtered = tableRows.filter { row ->
            query.filters.all { filter ->
                val rowVal = row[filter.column]
                when (filter.operator) {
                    SupabaseFilterOperator.EQ -> rowVal?.toString() == filter.value.toString()
                    SupabaseFilterOperator.NEQ -> rowVal?.toString() != filter.value.toString()
                    SupabaseFilterOperator.LIKE -> rowVal?.toString()?.contains(filter.value.toString(), ignoreCase = true) == true
                    else -> true
                }
            }
        }

        if (query.offset != null) {
            filtered = filtered.drop(query.offset)
        }
        if (query.limit != null) {
            filtered = filtered.take(query.limit)
        }

        return Result.Success(filtered.map(mapper))
    }

    override suspend fun <T : Any> insert(
        table: String,
        data: Map<String, Any?>,
        mapper: (Map<String, Any?>) -> T
    ): Result<T> {
        simulatedNetworkError?.let { return Result.Failure(errorMapper.mapThrowable(it)) }
        if (simulateRlsViolation) {
            return Result.Failure(
                errorMapper.mapPostgrestError(
                    statusCode = 403,
                    errorCode = "42501",
                    message = "RLS check failed for insert on table '$table'"
                )
            )
        }

        val tableMap = tables.getOrPut(table) { mutableMapOf() }
        val id = data["id"]?.toString() ?: UUID.randomUUID().toString()
        if (tableMap.containsKey(id)) {
            return Result.Failure(
                errorMapper.mapPostgrestError(
                    statusCode = 409,
                    errorCode = "23505",
                    message = "duplicate key value violates unique constraint"
                )
            )
        }

        val rowWithId = data + ("id" to id)
        tableMap[id] = rowWithId
        return Result.Success(mapper(rowWithId))
    }

    override suspend fun <T : Any> update(
        table: String,
        id: String,
        data: Map<String, Any?>,
        mapper: (Map<String, Any?>) -> T
    ): Result<T> {
        simulatedNetworkError?.let { return Result.Failure(errorMapper.mapThrowable(it)) }
        if (simulateRlsViolation) {
            return Result.Failure(
                errorMapper.mapPostgrestError(
                    statusCode = 403,
                    errorCode = "42501",
                    message = "RLS check failed for update on table '$table'"
                )
            )
        }

        val tableMap = tables[table]
        val existing = tableMap?.get(id) ?: return Result.Failure(
            errorMapper.mapPostgrestError(
                statusCode = 404,
                errorCode = "PGRST116",
                message = "The result contains 0 rows"
            )
        )

        val updated = existing + data + ("id" to id)
        tableMap[id] = updated
        return Result.Success(mapper(updated))
    }

    override suspend fun delete(table: String, id: String): Result<Unit> {
        simulatedNetworkError?.let { return Result.Failure(errorMapper.mapThrowable(it)) }
        val tableMap = tables[table]
        tableMap?.remove(id)
        return Result.Success(Unit)
    }

    /**
     * Executes a CRUD query and returns domain [Result] of [BaseOutput].
     */
    suspend fun <Output : BaseOutput> executeDomainQuery(
        query: SupabaseQuery,
        outputMapper: (List<Map<String, Any?>>) -> Output
    ): Result<Output> {
        return when (val result = select(query, { it })) {
            is Result.Failure -> Result.Failure(result.exception)
            is Result.Success -> Result.Success(outputMapper(result.data))
        }
    }
}
