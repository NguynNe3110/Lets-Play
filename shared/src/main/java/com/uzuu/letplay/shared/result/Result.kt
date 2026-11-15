package com.uzuu.letplay.shared.result

import com.uzuu.letplay.shared.error.AppException

/**
 * Sealed interface `Result<T>` đóng gói kết quả trả về của một tác vụ asynchronous.
 * Giúp xử lý `Success` (Thành công) và `Failure` (Thất bại với AppException) một cách rõ ràng, an toàn.
 */
sealed interface Result<out T> {
    data class Success<out T>(val data: T) : Result<T>
    data class Failure(val exception: AppException) : Result<Nothing>

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = (this as? Success)?.data
    fun exceptionOrNull(): AppException? = (this as? Failure)?.exception
}
