package com.example.medsense.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RxNormApi {
    @GET("REST/drugs.json")
    suspend fun getDrugs(
        @Query("name") name: String
    ): RxNormResponse

    @GET("REST/approximateTerm.json")
    suspend fun getApproximateMatch(
        @Query("term") term: String,
        @Query("maxEntries") maxEntries: Int = 1
    ): RxNormApproximateResponse

    @GET("REST/rxcui/{rxcui}/properties.json")
    suspend fun getRxcuiProperties(
        @Path("rxcui") rxcui: String
    ): RxNormPropertiesResponse

    @GET("REST/rxcui/{rxcui}/allrelated.json")
    suspend fun getAllRelatedInfo(
        @Path("rxcui") rxcui: String
    ): RxNormAllRelatedResponse
}
