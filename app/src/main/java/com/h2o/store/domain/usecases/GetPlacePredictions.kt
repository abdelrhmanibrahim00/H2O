package com.h2o.store.domain.usecases

import com.h2o.store.data.models.PlacesResponse
import com.h2o.store.repositories.LocationRepository

class GetPlacePredictions(private val repository: LocationRepository) {
    suspend operator fun invoke(input: String, apiKey: String): PlacesResponse {
        return repository.getPlacePredictions(input, apiKey)
    }
}