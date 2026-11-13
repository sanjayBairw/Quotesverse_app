package com.snj.quoteverse.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton network client configuring Retrofit and OkHttp for ZenQuotes API calls.
 */
object ZenQuotesApiClient {

    private const val BASE_URL = "https://zenquotes.io/"

    val rateLimiter = ZenQuotesRateLimiter()

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "QuoteVerse-Android/1.0")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val service: QuoteApiService by lazy {
        retrofit.create(QuoteApiService::class.java)
    }

    /**
     * Builds a custom service instance (useful for unit tests with MockWebServer).
     */
    fun createCustomService(customBaseUrl: String, customClient: OkHttpClient = okHttpClient): QuoteApiService {
        return Retrofit.Builder()
            .baseUrl(customBaseUrl)
            .client(customClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(QuoteApiService::class.java)
    }
}

