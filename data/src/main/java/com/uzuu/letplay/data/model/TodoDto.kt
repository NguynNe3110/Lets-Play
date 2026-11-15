package com.uzuu.letplay.data.model

/**
 * DTO mô phỏng dữ liệu nhận từ Network API hoặc Database Local.
 */
data class TodoDto(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
