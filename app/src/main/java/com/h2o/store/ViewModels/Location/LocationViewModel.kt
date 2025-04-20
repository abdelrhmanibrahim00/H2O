package com.h2o.store.ViewModels.Location

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.h2o.store.BuildConfig
import com.h2o.store.data.models.AddressData
import com.h2o.store.data.models.GeocodingResult
import com.h2o.store.data.models.LocationData
import com.h2o.store.data.models.PlacePrediction
import com.h2o.store.domain.usecases.GetAddressFromCoordinates
import com.h2o.store.domain.usecases.GetCoordinatesFromAddress
import com.h2o.store.domain.usecases.GetPlacePredictions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LocationLoadingState {
    IDLE,
    LOADING,
    LOCATION_READY,
    TIMEOUT,
    ERROR
}

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

    // Add a loading state for location updates
    private val _locationLoadingState = MutableStateFlow(LocationLoadingState.IDLE)
    val locationLoadingState: StateFlow<LocationLoadingState> = _locationLoadingState.asStateFlow()

    // Add a timeRemaining for countdown display (in seconds)
    private val _timeRemaining = MutableStateFlow(0)
    val timeRemaining: StateFlow<Int> = _timeRemaining.asStateFlow()

    // Add job to handle timeout
    private var locationTimeoutJob: Job? = null

    // Store location for passing between screens
    private val _storedLocation = MutableStateFlow<LocationData?>(null)
    val storedLocation: StateFlow<LocationData?> = _storedLocation.asStateFlow()

    // Callback storage for map location selection
    private var locationSelectedCallback: ((LocationData, AddressData) -> Unit)? = null

    fun updateLocation(newLocation: LocationData) {
        if (_location.value != newLocation) {
            _location.value = newLocation

            // If we're in LOADING state and get a location, transition to LOCATION_READY
            if (_locationLoadingState.value == LocationLoadingState.LOADING) {
                _locationLoadingState.value = LocationLoadingState.LOCATION_READY
                locationTimeoutJob?.cancel() // Cancel timeout job if it's running
            }
        }
    }

    // In the LocationViewModel class
    fun startLocationUpdates(timeoutDuration: Long = 8000L) {
        // Reset state
        _locationLoadingState.value = LocationLoadingState.LOADING

        // Set up timeout with countdown
        locationTimeoutJob?.cancel()
        locationTimeoutJob = viewModelScope.launch {
            try {
                // Calculate countdown values (every second)
                val totalSeconds = (timeoutDuration / 1000).toInt()
                for (i in totalSeconds downTo 1) {
                    _timeRemaining.value = i
                    delay(1000)
                }

                // If we reach here, timeout has occurred without getting location
                if (_locationLoadingState.value == LocationLoadingState.LOADING) {
                    _locationLoadingState.value = LocationLoadingState.TIMEOUT
                    Log.d("LocationViewModel", "Location request timed out after ${timeoutDuration}ms")
                }
            } catch (e: Exception) {
                // THIS IS WHERE THE CHANGE SHOULD HAPPEN
                if (e is kotlinx.coroutines.CancellationException) {
                    // This is a normal cancellation, not an error
                    Log.d("LocationViewModel", "Location timeout cancelled normally")
                    // Don't change the state to ERROR
                } else {
                    Log.e("LocationViewModel", "Error during location timeout: ${e.message}")
                    _locationLoadingState.value = LocationLoadingState.ERROR
                }
            }
        }
    }

    fun resetLocationState() {
        _locationLoadingState.value = LocationLoadingState.IDLE
        locationTimeoutJob?.cancel()
    }

    fun skipLocationWait() {
        // User manually skipped the wait - cancel timeout and proceed
        locationTimeoutJob?.cancel()
        if (_locationLoadingState.value == LocationLoadingState.LOADING) {
            _locationLoadingState.value = LocationLoadingState.TIMEOUT
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

    // New methods for callback and navigation support

    /**
     * Store callback for location selection and the current location
     */
    fun storeLocationCallback(callback: (LocationData, AddressData) -> Unit) {
        locationSelectedCallback = callback

        // Store current location when navigating to map
        _location.value?.let { currentLocation ->
            _storedLocation.value = currentLocation
        }
    }

    /**
     * Get stored callback
     */
    fun getLocationCallback(): ((LocationData, AddressData) -> Unit)? {
        return locationSelectedCallback
    }

    /**
     * Clear stored callback after use
     */
    fun clearLocationCallback() {
        locationSelectedCallback = null
    }

    /**
     * Updates stored location data (used for initializing map in edit mode)
     */
    fun updateStoredLocation(locationData: LocationData?) {
        _storedLocation.value = locationData
    }
}