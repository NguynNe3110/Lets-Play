package com.example.base.shared.extension

import com.example.base.shared.error.AppException
import com.example.base.shared.result.Result
import kotlinx.coroutines.CancellationException

fun <T> Result<T>.getOrThrow(): T = when (this) {
    is Result.Success -> data
    is Result.Failure -> throw exception
}

suspend inline fun <T> runCatchingApp(
    crossinline action: suspend () -> T
): Result<T> {
    return try {
        Result.Success(action())
    } catch (throwable: Throwable) {
        if (throwable is CancellationException) throw throwable
        Result.Failure(AppException.from(throwable))
    }
}

/**
 * Extension functions giúp thao tác với Result<T> một cách fluent & gọn gàng.
 */
inline fun <T> Result<T>.onSuccess(action: (value: T) -> Unit): Result<T> {
    if (this is Result.Success) {
        action(data)
    }
    return this
}

inline fun <T> Result<T>.onFailure(action: (exception: AppException) -> Unit): Result<T> {
    if (this is Result.Failure) {
        action(exception)
    }
    return this
}

inline fun <T, R> Result<T>.map(transform: (value: T) -> R): Result<R> {
    return when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Failure -> Result.Failure(exception)
    }
}

inline fun <T, R> Result<T>.flatMap(transform: (value: T) -> Result<R>): Result<R> {
    return when (this) {
        is Result.Success -> transform(data)
        is Result.Failure -> Result.Failure(exception)
    }
}
