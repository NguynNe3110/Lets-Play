package com.example.base.shared.error

interface ErrorMapper {
    fun map(throwable: Throwable): AppException = AppException.from(throwable)
}

object DefaultErrorMapper : ErrorMapper