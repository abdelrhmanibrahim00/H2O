package com.h2o.store.Models.Location

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.h2o.store.BuildConfig // Replace with your actual package name
import com.h2o.store.data.models.AddressData
import com.h2o.store.data.models.GeocodingResult
import com.h2o.store.data.models.LocationData
import com.h2o.store.data.models.PlacePrediction
import com.h2o.store.domain.usecases.GetAddressFromCoordinates
import com.h2o.store.domain.usecases.GetCoordinatesFromAddress
import com.h2o.store.domain.usecases.GetPlacePredictions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationViewModel(
    private val getAddressFromCoordinates: GetAddressFromCoordinates,
    private val getCoordinatesFromAddress: GetCoordinatesFromAddress,
    private val getPlacePredictions: GetPlacePredictions
) : ViewModel() {

    private val _apiKey = BuildConfig.MAPS_API_KEY
    init {
        Log.d("MapsDebug", "API Key: ${_apiKey}")
    }
    private val _location = MutableStateFlow<LocationData?>(null)
    val location = _location.asStateFlow()

    private val _address = mutableStateOf<List<GeocodingResult>>(emptyList())
    val address: State<List<GeocodingResult>> = _address

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _suggestions = MutableStateFlow<List<PlacePrediction>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    // Store the street address
    private val _streetAddress = MutableStateFlow("")
    val streetAddress = _streetAddress.asStateFlow()

    // New: Store structured address data
    private val _structuredAddress = MutableStateFlow<AddressData?>(null)
    val structuredAddress = _structuredAddress.asStateFlow()

    // Flag to indicate when location is updated from search
    private val _locationUpdatedFromSearch = MutableStateFlow(false)
    val locationUpdatedFromSearch = _locationUpdatedFromSearch.asStateFlow()

    fun updateLocation(newLocation: LocationData) {
        if (_location.value != newLocation) {
            _location.value = newLocation
        }
    }

    fun fetchAddress(latlng: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = getAddressFromCoordinates(latlng, _apiKey)
                _address.value = result.results

                // Update the street address
                val formattedAddress = result.results.firstOrNull()?.formatted_address ?: ""
                _streetAddress.value = formattedAddress

                // Parse and store structured address components
                parseAddressComponents(result.results.firstOrNull())

                Log.d("LocationViewModel", "Address fetched: $formattedAddress")
            } catch (e: Exception) {
                Log.e("LocationViewModel", "Error fetching address: ${e.message}")
            }
        }
    }

    private fun parseAddressComponents(geocodingResult: GeocodingResult?) {
        geocodingResult?.let { result ->
            // Create a structured address based on the formatted_address string
            val formattedAddress = result.formatted_address ?: ""

            // Extract address components using string parsing
            val addressParts = formattedAddress.split(",").map { it.trim() }

            // Default values
            var street = ""
            var city = ""
            var state = ""
            var country = ""
            var postalCode = ""

            // Basic parsing logic for address components
            when (addressParts.size) {
                0 -> {
                    // Empty address
                }
                1 -> {
                    // Only one component, assume it's the street
                    street = addressParts[0]
                }
                2 -> {
                    // Two components, assume street and city
                    street = addressParts[0]
                    city = addressParts[1]
                }
                3 -> {
                    // Three components, assume street, city, and state/country
                    street = addressParts[0]
                    city = addressParts[1]
                    state = addressParts[2]
                }
                else -> {
                    // More components, more detailed address
                    street = addressParts[0]
                    city = addressParts[1]

                    // Check if the last part contains a postal code
                    val lastPart = addressParts.last()
                    if (lastPart.any { it.isDigit() }) {
                        // Extract postal code if present
                        val postalMatch = Regex("\\d+").find(lastPart)
                        postalCode = postalMatch?.value ?: ""

                        // If postal code was found, the last or second-to-last part is likely the country
                        country = if (addressParts.size > 3) addressParts[addressParts.size - 2] else ""
                    } else {
                        // No postal code, last part is likely the country
                        country = lastPart
                    }

                    // State is likely the third-to-last part if we have enough components
                    if (addressParts.size > 3) {
                        state = addressParts[2]
                    }
                }
            }

            // Create the structured address
            _structuredAddress.value = AddressData(
                street = street,
                city = city,
                state = state,
                country = country,
                postalCode = postalCode,
                formattedAddress = formattedAddress
            )

            Log.d("LocationViewModel", "Structured address: ${_structuredAddress.value}")
        }
    }

    private var searchJob: Job? = null

    fun getSuggestions(query: String) {
        if (query.length >= 3) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val result = getPlacePredictions(query, _apiKey)
                    _suggestions.value = result.predictions
                } catch (e: Exception) {
                    Log.e("LocationViewModel", "Error getting suggestions: ${e.message}")
                }
            }
        } else {
            _suggestions.value = emptyList()
        }
    }

    // Reset the search update flag
    fun resetLocationUpdatedFlag() {
        _locationUpdatedFromSearch.value = false
    }

    /**
     * Set search query without triggering suggestions - use this when selecting from suggestions
     */
    fun setSearchQueryWithoutSuggestions(query: String) {
        _searchQuery.value = query
    }

    /**
     * Update the onSearchQueryChange function to properly handle suggestions
     */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query

        // Clear suggestions when text is empty
        if (query.isEmpty()) {
            _suggestions.value = emptyList()
            return
        }

        // Only get suggestions for longer queries
        if (query.length >= 3) {
            getSuggestions(query)
        } else {
            // Clear suggestions for short queries
            _suggestions.value = emptyList()
        }
    }

    /**
     * Explicitly clear all suggestions
     */
    fun clearSuggestions() {
        _suggestions.value = emptyList()
    }

    /**
     * Updated searchAddress to clear suggestions
     */
    fun searchAddress(address: String) {
        // Clear suggestions immediately when a search is initiated
        clearSuggestions()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("LocationViewModel", "Starting search for address: $address")

                val result = getCoordinatesFromAddress(address, _apiKey)
                Log.d("LocationViewModel", "Search results: $result")

                result.results.firstOrNull()?.let { geocodingResult ->
                    Log.d("LocationViewModel", "First result geometry: ${geocodingResult.geometry}")
                    val newLocation = LocationData(
                        latitude = geocodingResult.geometry.location.lat,
                        longitude = geocodingResult.geometry.location.lng
                    )

                    // Update location
                    _location.value = newLocation

                    // Update street address
                    _streetAddress.value = geocodingResult.formatted_address ?: address

                    // Parse and store structured address components
                    parseAddressComponents(geocodingResult)

                    // Set the flag to indicate location was updated from search
                    _locationUpdatedFromSearch.value = true

                    Log.d("LocationViewModel", "Updated location to: ${newLocation.latitude}, ${newLocation.longitude}")
                    Log.d("LocationViewModel", "Updated address to: ${_streetAddress.value}")
                }
            } catch (e: Exception) {
                Log.e("LocationViewModel", "Error searching address: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}