package com.uzuu.letplay.data.integration.rest

import com.uzuu.letplay.data.api.ApiErrorParser
import com.uzuu.letplay.data.api.DataErrorMapper
import com.uzuu.letplay.data.api.JsonPlaceholderService
import com.google.gson.Gson
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Factory for creating Retrofit and OkHttpClient instances based on [RestEnvironmentConfig].
 */
object RestClientFactory {

    fun createOkHttpClient(config: RestEnvironmentConfig): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(config.connectTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(config.readTimeoutMs, TimeUnit.MILLISECONDS)
            .build()
    }

    fun createRetrofit(
        config: RestEnvironmentConfig,
        okHttpClient: OkHttpClient = createOkHttpClient(config),
        gson: Gson = Gson()
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun createJsonPlaceholderService(
        config: RestEnvironmentConfig,
        okHttpClient: OkHttpClient = createOkHttpClient(config),
        gson: Gson = Gson()
    ): JsonPlaceholderService {
        return createRetrofit(config, okHttpClient, gson).create(JsonPlaceholderService::class.java)
    }

    fun createDataErrorMapper(gson: Gson = Gson()): DataErrorMapper {
        return DataErrorMapper(ApiErrorParser(gson))
    }
}
