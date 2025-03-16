package com.h2o.store.Screens.User

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.h2o.store.ViewModels.Location.LocationViewModel
import com.h2o.store.data.models.AddressData
import com.h2o.store.data.models.LocationData
import com.h2o.store.data.models.PlacePrediction

private val purpleColor = Color(0xFF6200EE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    locationViewModel: LocationViewModel,
    onLocationSelected: (LocationData, AddressData) -> Unit,
    onBackPressed: () -> Unit
) {
    val userLocation = remember {
        mutableStateOf(LatLng(locationViewModel.location.value?.latitude ?: 31.2001,
            locationViewModel.location.value?.longitude ?: 29.9187))
    }

    val searchQuery by locationViewModel.searchQuery.collectAsState()
    val suggestions by locationViewModel.suggestions.collectAsState()
    val locationUpdatedFromSearch by locationViewModel.locationUpdatedFromSearch.collectAsState()
    val locationData by locationViewModel.location.collectAsState()
    val streetAddress by locationViewModel.streetAddress.collectAsState()
    val structuredAddress by locationViewModel.structuredAddress.collectAsState()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation.value, 15f)
    }

    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = true,
            myLocationButtonEnabled = false,
            compassEnabled = true
        )
    }

    // Effect to update camera when searching
    LaunchedEffect(locationUpdatedFromSearch) {
        if (locationUpdatedFromSearch) {
            locationData?.let { location ->
                val newLatLng = LatLng(location.latitude, location.longitude)
                userLocation.value = newLatLng
                cameraPositionState.position = CameraPosition.fromLatLngZoom(newLatLng, 17f)
                locationViewModel.resetLocationUpdatedFlag()
            }
        }
    }

    // Effect to update address when camera moves
    LaunchedEffect(cameraPositionState.position) {
        if (!locationUpdatedFromSearch) {
            val newLocation = cameraPositionState.position.target
            userLocation.value = newLocation
            val locationData = LocationData(
                latitude = newLocation.latitude,
                longitude = newLocation.longitude
            )
            locationViewModel.fetchAddress("${locationData.latitude},${locationData.longitude}")
        }
    }

    // Use Scaffold with proper insets for a more reliable layout
    Scaffold(
        topBar = {
            Surface(
                color = purpleColor,
                elevation = 4.dp
            ) {
                TopAppBar(
                    title = {
                        Text(
                            "Set Your Location",
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackPressed) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    modifier = Modifier.statusBarsPadding(),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = purpleColor
                    )
                )
            }
        },
        bottomBar = {
            // Ensure the bottom button is always visible with proper padding
            Surface(
                color = purpleColor,
                elevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            locationData?.let { location ->
                                structuredAddress?.let { address ->
                                    // Use structured address if available
                                    onLocationSelected(location, address)
                                } ?: run {
                                    // Fallback to creating a basic address from the street address
                                    val fallbackAddress = AddressData(
                                        street = streetAddress,
                                        city = "Alexandria", // Default city
                                        state = "",
                                        country = "Egypt", // Default country
                                        postalCode = "",
                                        formattedAddress = streetAddress
                                    )
                                    onLocationSelected(location, fallbackAddress)
                                }
                            } ?: run {
                                // Create a new location from the current map position
                                val newLocation = LocationData(
                                    latitude = userLocation.value.latitude,
                                    longitude = userLocation.value.longitude
                                )

                                // Use structured address or create a fallback
                                if (structuredAddress != null) {
                                    onLocationSelected(newLocation, structuredAddress!!)
                                } else {
                                    val fallbackAddress = AddressData(
                                        street = streetAddress,
                                        city = "Alexandria", // Default city
                                        state = "",
                                        country = "Egypt", // Default country
                                        postalCode = "",
                                        formattedAddress = streetAddress
                                    )
                                    onLocationSelected(newLocation, fallbackAddress)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Set Location",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Map takes the full available space
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = mapUiSettings,
                onMapClick = { clickedLocation ->
                    userLocation.value = clickedLocation
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(clickedLocation, 17f)
                    val locationData = LocationData(
                        latitude = clickedLocation.latitude,
                        longitude = clickedLocation.longitude
                    )
                    locationViewModel.fetchAddress("${locationData.latitude},${locationData.longitude}")
                }
            ) {
                Marker(
                    state = MarkerState(position = userLocation.value),
                    title = "Selected Location"
                )
            }

            // Search container overlaid at the top of the map
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = 4.dp,
                shape = RoundedCornerShape(8.dp),
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Search Box
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { query ->
                            locationViewModel.onSearchQueryChange(query)
                            if (query.isEmpty()) {
                                locationViewModel.clearSuggestions()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search address") },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (searchQuery.isNotEmpty()) {
                                        locationViewModel.searchAddress(searchQuery)
                                        locationViewModel.clearSuggestions()
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = purpleColor
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (searchQuery.isNotEmpty()) {
                                    locationViewModel.searchAddress(searchQuery)
                                    locationViewModel.clearSuggestions()
                                }
                            }
                        ),
                        singleLine = true,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            backgroundColor = Color.White,
                            focusedBorderColor = purpleColor,
                            unfocusedBorderColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Address display if available
                    if (streetAddress.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = streetAddress,
                            style = MaterialTheme.typography.subtitle2.copy(
                                fontSize = 14.sp,
                                lineHeight = 18.sp
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Suggestions dropdown
                    if (suggestions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 200.dp)
                            ) {
                                items(suggestions) { suggestion ->
                                    SuggestionItem(
                                        suggestion = suggestion,
                                        onClick = {
                                            locationViewModel.setSearchQueryWithoutSuggestions(suggestion.description)
                                            locationViewModel.searchAddress(suggestion.description)
                                            locationViewModel.clearSuggestions()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionItem(
    suggestion: PlacePrediction,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.White
    ) {
        Text(
            text = suggestion.description,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            style = MaterialTheme.typography.body1,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}