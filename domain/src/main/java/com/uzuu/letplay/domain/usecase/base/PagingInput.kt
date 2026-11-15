package com.uzuu.letplay.domain.usecase.base

interface PagingInput : BaseInput {
    val page: Int
    val pageSize: Int
}
