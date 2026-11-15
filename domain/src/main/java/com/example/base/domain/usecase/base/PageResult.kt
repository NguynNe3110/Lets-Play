package com.example.base.domain.usecase.base

data class PageResult<out T>(
    val items: List<T>,
    val page: Int,
    val pageSize: Int,
    val hasNext: Boolean
) : BaseOutput
