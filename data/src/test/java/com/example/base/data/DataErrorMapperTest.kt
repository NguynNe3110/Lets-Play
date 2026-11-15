package com.example.base.data

import com.example.base.data.api.ApiErrorParser
import com.example.base.data.api.DataErrorMapper
import com.example.base.shared.error.AppException
import com.google.gson.Gson
import retrofit2.HttpException
import retrofit2.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import kotlin.test.Test
import kotlin.test.assertEquals

class DataErrorMapperTest {
    @Test
    fun mapsServerStatusToRetryableFailure() {
        val failure = DataErrorMapper(ApiErrorParser(Gson())).map(
            HttpException(
                Response.error<Any>(503, "".toResponseBody(null))
            )
        )

        assertEquals(true, failure is AppException.ServerError)
        assertEquals(true, failure.retryable)
    }
}