package com.h2o.store.data.models

data class PlacesResponse(
    val predictions: List<PlacePrediction>,
    val status: String
)

data class PlacePrediction(
    val description: String,
    val place_id: String
)