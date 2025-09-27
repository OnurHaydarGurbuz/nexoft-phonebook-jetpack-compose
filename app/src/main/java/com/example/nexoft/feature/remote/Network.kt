package com.example.nexoft.feature.remote

import com.example.nexoft.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import android.util.Log

private const val NET = "NET"
// GEÇİCİ: kesinlemek için fallback (teslim öncesi kaldır!)
private const val API_KEY_FALLBACK = "1117bf03-16c2-41f6-8c15-df997e248cd1"

private val headerInterceptor = Interceptor { chain ->
    val raw = BuildConfig.API_KEY
    val usedFallback = raw.isBlank()
    val key = (if (usedFallback) API_KEY_FALLBACK else raw).trim()
    Log.d(NET, "ApiKey source=${if (usedFallback) "FALLBACK" else "BUILD"} len=${key.length}")

    val req = chain.request().newBuilder()
        .addHeader("ApiKey", key)
        //.addHeader("accept", "application/json")
        .addHeader("accept", "text/plain")
        .build()
    chain.proceed(req)
}

private val logging = HttpLoggingInterceptor { msg -> Log.d(NET, msg) }.apply {
    level = HttpLoggingInterceptor.Level.HEADERS // teslim öncesi BASIC/NONE yap
}

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

val httpClient: OkHttpClient = OkHttpClient.Builder()
    .addInterceptor(headerInterceptor)
    .addInterceptor(logging)
    .connectTimeout(20, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()

val retrofit: Retrofit = Retrofit.Builder()
    .baseUrl(BuildConfig.BASE_URL)
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .client(httpClient)
    .build()
