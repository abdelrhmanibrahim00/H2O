package com.h2o.store.Screens.User

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.h2o.store.Navigation.Screen
import com.h2o.store.R
import com.h2o.store.Utils.LocationUtils
import com.h2o.store.ViewModels.Location.LocationLoadingState
import com.h2o.store.ViewModels.Location.LocationViewModel
import com.h2o.store.ViewModels.User.SignUpViewModel
import com.h2o.store.ViewModels.User.SignUpViewModel.SignUpState
import com.h2o.store.repositories.AuthRepository
import com.h2o.store.reusableComposable.InfoIcon
import com.h2o.store.reusableComposable.PasswordTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    navController: NavHostController,
    context: Context,
    OnNavigateToMap: () -> Unit,
    locationUtils: LocationUtils,
    onSignUpSuccess: () -> Unit,
    locationViewModel: LocationViewModel,
    onBackPressed: () -> Unit,
    viewModel: SignUpViewModel = viewModel(factory = SignUpViewModel.Factory(AuthRepository()))
) {
    val name by viewModel.name.collectAsState()
    val password by viewModel.password.collectAsState()
    val phone by viewModel.phone.collectAsState()
    val email by viewModel.email.collectAsState()
    val whatsapp by viewModel.whatsapp.collectAsState()
    val district by viewModel.district.collectAsState()
    val addressData by viewModel.addressData.collectAsState()
    val locationData by viewModel.locationData.collectAsState()
    val signUpState by viewModel.signUpState.collectAsState()
    val accountType by viewModel.accountType.collectAsState()


    val userLocation by locationViewModel.location.collectAsState()
    val structuredAddress by locationViewModel.structuredAddress.collectAsState()
    val streetAddress by locationViewModel.streetAddress.collectAsState()
    val locationLoadingState by locationViewModel.locationLoadingState.collectAsState()
    val timeRemaining by locationViewModel.timeRemaining.collectAsState()

    // Effect to update address display when address data changes
    LaunchedEffect(userLocation, structuredAddress) {
        val currentLocation = userLocation
        val currentAddress = structuredAddress

        if (currentLocation != null && currentAddress != null && locationData == null) {
            // Only update if we have location and address from LocationViewModel
            // but nothing in SignUpViewModel yet
            viewModel.updateLocationAndAddress(currentLocation, currentAddress)
        }
    }

    // Store selected address display text
    var selectedAddress by remember {
        mutableStateOf(
            if (addressData != null && addressData!!.formattedAddress.isNotEmpty())
                addressData!!.formattedAddress
            else "Select Address"
        )
    }

    // Update displayed address when it changes
    LaunchedEffect(addressData) {
        val currentAddressData = addressData
        if (currentAddressData != null && currentAddressData.formattedAddress.isNotEmpty()) {
            selectedAddress = currentAddressData.formattedAddress
        }
    }

    val hasPermission = remember { mutableStateOf(locationUtils.hasLocationPermission(context)) }

    // Handle permission request
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {

                hasPermission.value = true

                // Start location updates with timeout handling in ViewModel
                locationUtils.requestLocationUpdates(locationViewModel)
                locationViewModel.startLocationUpdates()
            } else {
                val rationaleRequired = ActivityCompat.shouldShowRequestPermissionRationale(
                    context as android.app.Activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                if (rationaleRequired) {
                    Toast.makeText(context, "Location Permission is required", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Enable Location Permission in Settings", Toast.LENGTH_LONG).show()
                }
            }
        }
    )

    // Handle location state changes
    LaunchedEffect(locationLoadingState) {
        when (locationLoadingState) {
            LocationLoadingState.LOCATION_READY -> {
                // Location is ready, navigate to map
                OnNavigateToMap()
                // Reset loading state after navigation
                locationViewModel.resetLocationState()
            }
            LocationLoadingState.TIMEOUT -> {
                // Timeout occurred, we can still navigate to map with whatever
                // location data we have (or none)
                OnNavigateToMap()
                // Reset loading state after navigation
                locationViewModel.resetLocationState()
            }
            LocationLoadingState.ERROR -> {
                // Show error toast and reset state
                Toast.makeText(context, "Error getting location. Please try again.", Toast.LENGTH_SHORT).show()
                locationViewModel.resetLocationState()
            }
            else -> {} // No action needed for IDLE or LOADING states
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign Up") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Form fields
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Name
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { viewModel.onNameChanged(it) },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                }

                // Email
                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { viewModel.onEmailChanged(it) },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                }

                // Password with visibility toggle
                item {
                    PasswordTextField(
                        password = password,
                        onPasswordChange = { viewModel.onPasswordChanged(it) }
                    )
                }

                // City (Non-editable)
                item {
                    OutlinedTextField(
                        value = "Alexandria",
                        onValueChange = {},
                        label = { Text("City") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = false
                    )
                }

                // District
                item {
                    Text(
                        text = "District",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = district,
                            onValueChange = { viewModel.onDistrictChanged(it) },
                            label = { Text("District") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        Box(modifier = Modifier.padding(start = 8.dp)) {
                            InfoIcon(infoTextResourceId = R.string.district_info)
                        }
                    }
                }



// for the Address Section with this improved version

                item {
                    when (locationLoadingState) {
                        LocationLoadingState.LOADING -> {
                            // Improved loading UI with circular progress indicator
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { /* No action while loading */ },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(1.dp, Color.Gray),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = Color.Gray
                                    ),
                                    enabled = false
                                ) {
                                    Text(
                                        text = "Getting location...",
                                        modifier = Modifier.weight(1f),
                                        color = Color.Gray
                                    )
                                }

                                Box(
                                    modifier = Modifier.size(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }

                                // Skip button as a small button
                                TextButton(
                                    onClick = { locationViewModel.skipLocationWait() },
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    Text("Skip", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        else -> {
                            // Keep the original address selection UI for non-loading states
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (hasPermission.value) {
                                            // If we already have permission, start updates and go to map
                                            locationUtils.requestLocationUpdates(locationViewModel)
                                            OnNavigateToMap()
                                        } else {
                                            // Otherwise request permissions first
                                            requestPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(1.dp, Color.Gray),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Text(
                                        text = selectedAddress,
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Color.Gray
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = "Select Location",
                                    tint = Color.DarkGray
                                )
                            }
                        }
                    }
                }

                // Account Type dropdown with properly aligned info icon
                item {

                    var expanded by remember { mutableStateOf(false) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = accountType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Account Type") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.exposedDropdownSize()
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Home") },
                                    onClick = {
                                        viewModel.onAccountTypeChanged("Home")
                                        expanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Market") },
                                    onClick = {
                                        viewModel.onAccountTypeChanged("Market")
                                        expanded = false
                                    }
                                )
                            }
                        }

                        // Info icon aligned to the right
                        Box(modifier = Modifier.padding(start = 8.dp)) {
                            InfoIcon(infoTextResourceId = R.string.account_type_info)
                        }
                    }
                }

                // Phone Number
                item {
                    OutlinedTextField(
                        value = phone.ifEmpty { "+2" },
                        onValueChange = {
                            if (it.length <= 15) viewModel.onPhoneChanged(it)
                        },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                }

                // WhatsApp Number (Optional)
                item {
                    OutlinedTextField(
                        value = whatsapp.ifEmpty { "+2" },
                        onValueChange = {
                            if (it.length <= 15) viewModel.onWhatsAppChanged(it)
                        },
                        label = { Text("WhatsApp Number (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                }

                // Sign-Up Button
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.signUp() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Sign Up")
                    }
                }

                // Navigate to Login
                item {
                    TextButton(onClick = { navController.navigate(Screen.Login.route) }) {
                        Text("Already have an account? Log In")
                    }
                }

                // Handle Sign-Up State with better spacing
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    when (val state = signUpState) {
                        is SignUpState.Idle -> {}
                        is SignUpState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        is SignUpState.Success -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Sign up successful!",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            LaunchedEffect(Unit) {
                                onSignUpSuccess()
                            }
                        }
                        is SignUpState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = state.message,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Add extra space at the bottom to ensure error messages are visible
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}