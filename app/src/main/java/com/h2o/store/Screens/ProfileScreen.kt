package com.h2o.store.Screens


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.h2o.store.Models.ProfileState
import com.h2o.store.Models.ProfileViewModel
import com.h2o.store.repositories.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    navController: NavController,
    onEditProfile: () -> Unit
) {
    val profileState by viewModel.profileState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (val state = profileState) {
                is ProfileState.Idle -> {
                    // Initial state, nothing to show yet
                }
                is ProfileState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ProfileState.Success -> {
                    ProfileContent(
                        user = state.user,
                        onEditProfile = onEditProfile,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is ProfileState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error: ${state.message}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadProfile() }) {
                            Text("Try Again")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileContent(
    user: User,
    onEditProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile header with edit button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onEditProfile) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
            }
        }

        // Default profile image (or use user profile image if available)
        AsyncImage(
            model = "https://via.placeholder.com/120",  // Replace with actual profile picture URL
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // User name
        Text(
            text = user.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // User email
        Text(
            text = user.email,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        // User details
        ProfileInfoCard(
            items = listOf(
                "Phone" to user.phone,
                "WhatsApp" to user.whatsapp,
                "District" to user.district,
                "City" to user.city
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ProfileInfoCard(
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            items.forEachIndexed { index, (label, value) ->
                ProfileInfoItem(label = label, value = value)

                if (index < items.size - 1) {
                    Divider(
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileInfoItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value.ifEmpty { "Not provided" },
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
