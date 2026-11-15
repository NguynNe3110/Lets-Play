package com.example.base.domain.repository

import com.example.base.domain.model.User
import com.example.base.shared.result.Result
import com.example.base.domain.usecase.base.PageResult

interface UserRepository {
    suspend fun getUsers(page: Int = 1, pageSize: Int = 10): Result<PageResult<User>>
}