package com.h2o.store.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton Retrofit client for the inventory prediction API
 */
object PredictionRetrofitClient {
    // Keep using your hardcoded BASE_URL since you're staying with localhost
    private const val BASE_URL = "http://192.168.178.62:7860/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)    // Increased from 30
        .readTimeout(60, TimeUnit.SECONDS)       // Increased from 30
        .writeTimeout(60, TimeUnit.SECONDS)      // Increased from 30
        .retryOnConnectionFailure(true)          // Added retry capability
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val inventoryPredictionService: InventoryPredictionApiService by lazy {
        retrofit.create(InventoryPredictionApiService::class.java)
    }
}