package com.h2o.store.domain.usecases

import com.h2o.store.data.models.GeocodingResponse
import com.h2o.store.repositories.LocationRepository

class GetCoordinatesFromAddress(private val repository: LocationRepository) {
    suspend operator fun invoke(address: String, apiKey: String): GeocodingResponse {
        return repository.getCoordinatesFromAddress(address, apiKey)
    }
}
