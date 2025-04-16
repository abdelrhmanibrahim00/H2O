package com.h2o.store.Screens.Admin


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.h2o.store.ViewModels.Admin.ManageUsersViewModel
import com.h2o.store.data.User.UserData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailsScreen(
    navController: NavHostController,
    viewModel: ManageUsersViewModel,
    userId: String,
    onEditUser: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    var user by remember { mutableStateOf<UserData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Reset edit result when entering screen
    LaunchedEffect(Unit) {
        viewModel.resetEditUserResult()
    }

    // Load user details
    LaunchedEffect(userId) {
        isLoading = true
        try {
            val userDetails = viewModel.getUserDetails(userId)
            user = userDetails
        } catch (e: Exception) {
            errorMessage = "Failed to load user details: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEditUser(userId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
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
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else if (user != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(scrollState)
                ) {
                    // User basic information
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = user?.name ?: "No Name",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                RoleChip(role = user?.role ?: "")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Divider()

                            Spacer(modifier = Modifier.height(8.dp))

                            InfoItem(label = "Email", value = user?.email ?: "")
                            InfoItem(label = "Phone", value = user?.phone ?: "")
                            user?.whatsapp?.let {
                                if (it.isNotEmpty()) {
                                    InfoItem(label = "WhatsApp", value = it)
                                }
                            }
                            InfoItem(label = "User ID", value = user?.id ?: "")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Location information
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Location",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Divider()

                            Spacer(modifier = Modifier.height(8.dp))

                            InfoItem(label = "City", value = user?.city ?: "")
                            InfoItem(label = "District", value = user?.district ?: "")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Address information
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Address Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Divider()

                            Spacer(modifier = Modifier.height(8.dp))

                            user?.address?.let { address ->
                                InfoItem(label = "Street", value = address.street)
                                InfoItem(label = "City", value = address.city)
                                InfoItem(label = "State", value = address.state)
                                InfoItem(label = "Postal Code", value = address.postalCode)
                                InfoItem(label = "Country", value = address.country)
                                InfoItem(label = "Formatted Address", value = address.formattedAddress)
                            } ?: Text("No address information available")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = value.ifEmpty { "Not provided" },
            style = MaterialTheme.typography.bodyLarge
        )
    }
}