package com.uzuu.letplay.domain.repository

import com.uzuu.letplay.domain.model.User
import com.uzuu.letplay.shared.result.Result
import com.uzuu.letplay.domain.usecase.base.PageResult

interface UserRepository {
    suspend fun getUsers(page: Int = 1, pageSize: Int = 10): Result<PageResult<User>>
}