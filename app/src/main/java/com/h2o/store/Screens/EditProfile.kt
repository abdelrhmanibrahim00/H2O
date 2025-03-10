package com.h2o.store.Screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.h2o.store.Models.ProfileState
import com.h2o.store.Models.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: ProfileViewModel,
    navController: NavController
) {
    // Collect state values
    val name by viewModel.name.collectAsState()
    val phone by viewModel.phone.collectAsState()
    val whatsapp by viewModel.whatsapp.collectAsState()
    val district by viewModel.district.collectAsState()
    val profileState by viewModel.profileState.collectAsState()


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

            // Phone
            OutlinedTextField(
                value = phone,
                onValueChange = { viewModel.onPhoneChanged(it) },
                label = { Text("Phone") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // WhatsApp
            OutlinedTextField(
                value = whatsapp,
                onValueChange = { viewModel.onWhatsappChanged(it) },
                label = { Text("WhatsApp") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // District
            OutlinedTextField(
                value = district,
                onValueChange = { viewModel.onDistrictChanged(it) },
                label = { Text("District") },
                modifier = Modifier.fillMaxWidth()
            )

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