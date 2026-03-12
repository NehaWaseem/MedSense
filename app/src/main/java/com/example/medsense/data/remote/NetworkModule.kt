package com.example.medsense.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    private const val OPENFDA_BASE_URL = "https://api.fda.gov/"
    private const val RXNORM_BASE_URL = "https://rxnav.nlm.nih.gov/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            // Set to BODY for better debugging of API requests and responses
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    fun createOpenFdaApi(): OpenFdaDrugLabelApi {
        return Retrofit.Builder()
            .baseUrl(OPENFDA_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenFdaDrugLabelApi::class.java)
    }

    fun createRxNormApi(): RxNormApi {
        return Retrofit.Builder()
            .baseUrl(RXNORM_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(RxNormApi::class.java)
    }
}
