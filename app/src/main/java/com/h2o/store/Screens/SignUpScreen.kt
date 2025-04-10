package com.h2o.store.Screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.h2o.store.Models.Location.LocationViewModel
import com.h2o.store.Models.SignUpViewModel
import com.h2o.store.Models.SignUpViewModel.SignUpState
import com.h2o.store.Navigation.Screen
import com.h2o.store.R
import com.h2o.store.Utils.LocationUtils
import com.h2o.store.data.models.AddressData
import com.h2o.store.repositories.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SignUpScreen(
    navController: NavHostController,
    context: Context,
    OnNavigateToMap: () -> Unit,
    locationUtils: LocationUtils,
    onSignUpSuccess: () -> Unit,
    locationViewModel: LocationViewModel,
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

    val userLocation by locationViewModel.location.collectAsState()
    val structuredAddress by locationViewModel.structuredAddress.collectAsState()
    val streetAddress by locationViewModel.streetAddress.collectAsState()

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
    val isLoading = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {

                hasPermission.value = true
                isLoading.value = true
                locationUtils.requestLocationUpdates(locationViewModel)

                coroutineScope.launch {
                    delay(8000)
                    isLoading.value = false
                    OnNavigateToMap()
                }
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(text = "Sign Up", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(24.dp))
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
            Spacer(modifier = Modifier.height(16.dp))
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
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Password with visibility toggle
        item {
            PasswordTextField(
                password = password,
                onPasswordChange = { viewModel.onPasswordChanged(it) }
            )
            Spacer(modifier = Modifier.height(16.dp))
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
            Spacer(modifier = Modifier.height(16.dp))
        }

        // District
        item {
            OutlinedTextField(
                value = district,
                onValueChange = { viewModel.onDistrictChanged(it) },
                label = { Text("District") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Address Section with Navigation & Permission Handling
        item {
            if (isLoading.value) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (hasPermission.value) {
                                locationUtils.requestLocationUpdates(locationViewModel)
                                OnNavigateToMap()
                            } else {
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
                        painter = painterResource(id = R.drawable.baseline_location_on_24),
                        contentDescription = "Select Location",
                        tint = Color.DarkGray
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
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
            Spacer(modifier = Modifier.height(16.dp))
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
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Sign-Up Button
        item {
            Button(
                onClick = { viewModel.signUp() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Sign Up")
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Navigate to Login
        item {
            TextButton(onClick = { navController.navigate(Screen.Login.route) }) {
                Text("Already have an account? Log In")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Handle Sign-Up State
        item {
            when (val state = signUpState) {
                is SignUpState.Idle -> {}
                is SignUpState.Loading -> CircularProgressIndicator()
                is SignUpState.Success -> onSignUpSuccess()
                is SignUpState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// Helper function to transfer location data from LocationViewModel to SignUpViewModel
private fun transferLocationToSignUp(
    locationViewModel: LocationViewModel,
    signUpViewModel: SignUpViewModel
) {
    val location = locationViewModel.location.value
    val structuredAddress = locationViewModel.structuredAddress.value
    val streetAddress = locationViewModel.streetAddress.value

    if (location != null && structuredAddress != null) {
        // Transfer both location and structured address
        signUpViewModel.updateLocationAndAddress(location, structuredAddress)
    } else if (location != null && streetAddress.isNotEmpty()) {
        // Fallback if we only have street address
        val fallbackAddress = AddressData(
            street = streetAddress,
            city = "Alexandria",
            formattedAddress = streetAddress
        )
        signUpViewModel.updateLocationAndAddress(location, fallbackAddress)
    }
}

@Composable
fun PasswordTextField(
    password: String,
    onPasswordChange: (String) -> Unit
) {
    var isPasswordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text("Password") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                Icon(
                    painter = painterResource(id = if (isPasswordVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off),
                    contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                    tint = Color.Gray
                )
            }
        }
    )
}