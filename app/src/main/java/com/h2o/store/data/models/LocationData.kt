package com.h2o.store.data.models

data class LocationData(
    val latitude: Double,
    val longitude: Double
)

data class GeocodingResponse(
    val results: List<GeocodingResult>,
    val status: String
)

data class GeocodingResult(
    val formatted_address: String,
    val geometry: Geometry
)
data class Location(
    val lat: Double,  // Notice these are 'lat' and 'lng', not 'latitude' and 'longitude'
    val lng: Double
)

data class Geometry(
    val location: Location  // Changed to use LocationData instead
)