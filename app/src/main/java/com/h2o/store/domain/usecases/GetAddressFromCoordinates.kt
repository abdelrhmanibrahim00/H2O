package com.h2o.store.domain.usecases


import com.h2o.store.data.models.GeocodingResponse
import com.h2o.store.repositories.LocationRepository

class GetAddressFromCoordinates(private val repository: LocationRepository) {
    suspend operator fun invoke(latlng: String, apiKey: String): GeocodingResponse {
        return repository.getAddressFromCoordinates(latlng, apiKey)
    }
}
