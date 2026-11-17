package com.uzuu.letplay.composition.product

import com.uzuu.letplay.data.api.practice.PracticeDataErrorMapper
import com.uzuu.letplay.data.api.practice.PracticeProductApi
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object ProductNetworkModule {

    const val PRACTICE_API_BASE_URL: String = "https://templetezuu.netlify.app/api/v1/"

    @Provides
    @Singleton
    fun providePracticeDataErrorMapper(gson: Gson): PracticeDataErrorMapper =
        PracticeDataErrorMapper(gson)

    @Provides
    @Singleton
    @PracticeRetrofit
    fun providePracticeRetrofit(
        client: OkHttpClient,
        gson: Gson
    ): Retrofit = Retrofit.Builder()
        .baseUrl(PRACTICE_API_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    @Provides
    @Singleton
    fun providePracticeProductApi(
        @PracticeRetrofit retrofit: Retrofit
    ): PracticeProductApi = retrofit.create(PracticeProductApi::class.java)
}
