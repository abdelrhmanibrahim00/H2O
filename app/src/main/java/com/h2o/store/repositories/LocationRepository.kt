package com.h2o.store.repositories

import com.h2o.store.data.models.GeocodingResponse
import com.h2o.store.data.models.PlacesResponse
import com.h2o.store.data.remote.GeocodingApiService

class LocationRepository(private val apiService: GeocodingApiService) {

    suspend fun getAddressFromCoordinates(latlng: String, apiKey: String): GeocodingResponse {
        return apiService.getAddressFromCoordinates(latlng, apiKey)
    }
    suspend fun getCoordinatesFromAddress(address: String, apiKey: String): GeocodingResponse {
        return apiService.getCoordinatesFromAddress(address, apiKey)
    }
    suspend fun getPlacePredictions(input: String, apiKey: String): PlacesResponse {
        return apiService.getPlacePredictions(input, apiKey)
    }
}