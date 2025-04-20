package com.h2o.store.Screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.h2o.store.ViewModels.User.ProfileState
import com.h2o.store.ViewModels.User.ProfileViewModel
import com.h2o.store.data.models.AddressData
import com.h2o.store.data.models.LocationData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: ProfileViewModel,
    navController: NavController,
    onNavigateToMap: (LocationData?, (LocationData, AddressData) -> Unit) -> Unit
) {
    // Collect state values
    val name by viewModel.name.collectAsState()
    val phone by viewModel.phone.collectAsState()
    val whatsapp by viewModel.whatsapp.collectAsState()
    val district by viewModel.district.collectAsState()
    val profileState by viewModel.profileState.collectAsState()
    val locationData by viewModel.locationData.collectAsState()
    val addressData by viewModel.addressData.collectAsState()

    // Format address for display
    val displayAddress = remember(addressData) {
        addressData?.formattedAddress ?: "Set your location"
    }

    // Handler for location selection from map
    val handleLocationSelected: (LocationData, AddressData) -> Unit = { location, address ->
        viewModel.updateLocationAndAddress(location, address)
        // After updating location in ViewModel, navigate back to EditProfileScreen
        navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { viewModel.onNameChanged(it) },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Phone with number keyboard
            OutlinedTextField(
                value = phone,
                onValueChange = { viewModel.onPhoneChanged(it) },
                label = { Text("Phone") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // WhatsApp with number keyboard
            OutlinedTextField(
                value = whatsapp,
                onValueChange = { viewModel.onWhatsappChanged(it) },
                label = { Text("WhatsApp") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // District
            OutlinedTextField(
                value = district,
                onValueChange = { viewModel.onDistrictChanged(it) },
                label = { Text("District") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Location Selection
            Text(
                text = "Your Location",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Location display with button to open map
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        // Navigate to MapScreen with current location data
                        onNavigateToMap(locationData, handleLocationSelected)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, Color.Gray),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        text = displayAddress,
                        modifier = Modifier.weight(1f),
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Select Location",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save button
            Button(
                onClick = { viewModel.updateProfile() },
                modifier = Modifier.fillMaxWidth(),
                enabled = profileState !is ProfileState.Loading
            ) {
                Text("Save Changes")
            }

            // Show loading or error message
            when (profileState) {
                is ProfileState.Loading -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                is ProfileState.Error -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = (profileState as ProfileState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {}
            }
        }
    }
}