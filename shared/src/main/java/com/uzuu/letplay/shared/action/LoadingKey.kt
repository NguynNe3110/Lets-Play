package com.uzuu.letplay.shared.action

/**
 * Type-safe key representing a category of loading operation.
 * Reference-counted by [LoadingManager] so overlapping operations do not hide each other's indicators.
 */
@JvmInline
value class LoadingKey(val value: String) {
    companion object {
        val Initial = LoadingKey("initial")
        val Refresh = LoadingKey("refresh")
        val Submit = LoadingKey("submit")
        val Pagination = LoadingKey("pagination")
    }
}
