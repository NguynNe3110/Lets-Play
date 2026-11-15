package com.example.base.domain.usecase.base

interface PagingInput : BaseInput {
    val page: Int
    val pageSize: Int
}
