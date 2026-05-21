package com.ruoshui.health.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.ruoshui.health.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

object NetworkModule {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun healthApi(baseUrl: String = BuildConfig.DEFAULT_API_BASE_URL): HealthApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(HealthApi::class.java)
    }
}
