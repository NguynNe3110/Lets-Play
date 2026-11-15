package com.example.base.domain.model

/**
 * Domain Entity đại diện cho một công việc (Todo).
 * Thuần Kotlin data class, độc lập với bất kỳ framework nào.
 */
data class Todo(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false
)
