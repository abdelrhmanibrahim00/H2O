package com.h2o.store.data.remote

import com.h2o.store.data.models.GeocodingResponse
import com.h2o.store.data.models.PlacesResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingApiService {
    @GET("maps/api/geocode/json")
    suspend fun getAddressFromCoordinates(
        @Query("latlng") latlng: String,
        @Query("key") apiKey: String
    ): GeocodingResponse

    @GET("maps/api/geocode/json")
    suspend fun getCoordinatesFromAddress(
        @Query("address") address: String,
        @Query("key") apiKey: String
    ): GeocodingResponse
    @GET("maps/api/place/autocomplete/json")
    suspend fun getPlacePredictions(
        @Query("input") input: String,
        @Query("key") apiKey: String,
        @Query("components") components: String = "country:eg" // Restrict to Egypt
    ): PlacesResponse
}