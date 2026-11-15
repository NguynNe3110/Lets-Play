package com.example.base.shared.action

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages reference counts for active loading operations by [LoadingKey].
 * Independent of Android ViewModel and presentation frameworks.
 *
 * Each operation starting with a given key increments that key's count.
 * When the operation ends (including on error or cancellation), the count is decremented.
 * Overlapping operations with the same key do not prematurely hide each other's indicators.
 */
class LoadingManager {
    private val _loadingKeys = MutableStateFlow<Map<LoadingKey, Int>>(emptyMap())

    /**
     * Observable map of active loading keys and their reference counts.
     */
    val loadingKeys: StateFlow<Map<LoadingKey, Int>> = _loadingKeys.asStateFlow()

    /**
     * Whether any loading operation is currently active.
     */
    val isLoading: Boolean
        get() = _loadingKeys.value.isNotEmpty()

    /**
     * Checks if a specific [key] is currently active (count > 0).
     */
    fun isLoading(key: LoadingKey): Boolean = (_loadingKeys.value[key] ?: 0) > 0

    /**
     * Returns the current reference count for [key].
     */
    fun count(key: LoadingKey): Int = _loadingKeys.value[key] ?: 0

    /**
     * Increments the reference count for [key].
     */
    fun startLoading(key: LoadingKey = LoadingKey.Initial) {
        updateLoading(key, 1)
    }

    /**
     * Decrements the reference count for [key].
     */
    fun stopLoading(key: LoadingKey = LoadingKey.Initial) {
        updateLoading(key, -1)
    }

    private fun updateLoading(key: LoadingKey, delta: Int) {
        _loadingKeys.update { current ->
            val next = (current[key] ?: 0) + delta
            if (next <= 0) current - key
            else current + (key to next)
        }
    }

    /**
     * Executes [block] while incrementing the reference count for [key] for the duration.
     * Decrements the count in a `finally` block, ensuring cleanup on cancellation or failure.
     */
    suspend fun <T> withLoading(
        key: LoadingKey = LoadingKey.Initial,
        block: suspend () -> T
    ): T {
        startLoading(key)
        try {
            return block()
        } finally {
            stopLoading(key)
        }
    }

    /**
     * Clears all loading states.
     */
    fun clear() {
        _loadingKeys.value = emptyMap()
    }
}
