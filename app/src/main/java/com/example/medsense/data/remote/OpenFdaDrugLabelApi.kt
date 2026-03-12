package com.example.medsense.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenFdaDrugLabelApi {
    @GET("drug/label.json")
    suspend fun searchDrugLabels(
        @Query("search") search: String,
        @Query("limit") limit: Int = 1
    ): OpenFdaDrugLabelResponse
}

