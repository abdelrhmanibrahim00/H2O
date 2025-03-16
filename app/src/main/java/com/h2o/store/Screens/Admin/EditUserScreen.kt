package com.h2o.store.Screens.Admin


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.h2o.store.ViewModels.Admin.ManageUsersViewModel
import com.h2o.store.data.User.UserData
import com.h2o.store.data.models.AddressData
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserScreen(
    navController: NavHostController,
    viewModel: ManageUsersViewModel,
    userId: String,
    onBackClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // States for the form
    var originalUser by remember { mutableStateOf<UserData?>(null) }
    var editedUser by remember { mutableStateOf<UserData?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Form fields
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }

    // Role dropdown
    val roleOptions = listOf("user", "admin", "delivery")
    var selectedRole by remember { mutableStateOf("") }
    var roleDropdownExpanded by remember { mutableStateOf(false) }

    // Address fields
    var street by remember { mutableStateOf("") }
    var addressCity by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }

    // Observe edit result
    val editResult by viewModel.editUserResult.collectAsState()

    // Load user details
    LaunchedEffect(userId) {
        isLoading = true
        try {
            val userDetails = viewModel.getUserDetails(userId)
            originalUser = userDetails
            editedUser = userDetails?.copy()

            // Initialize form fields
            userDetails?.let { user ->
                name = user.name
                email = user.email
                phone = user.phone
                whatsapp = user.whatsapp ?: ""
                city = user.city
                district = user.district
                selectedRole = user.role

                // Initialize address fields
                user.address?.let { address ->
                    street = address.street
                    addressCity = address.city
                    state = address.state
                    postalCode = address.postalCode
                    country = address.country
                }
            }
        } catch (e: Exception) {
            scope.launch {
                snackbarHostState.showSnackbar("Failed to load user details: ${e.message}")
            }
        } finally {
            isLoading = false
        }
    }

    // Reset edit result when entering the screen
    LaunchedEffect(Unit) {
        viewModel.resetEditUserResult()
    }

    // Handle edit result
    LaunchedEffect(editResult) {
        if (editResult == true) {
            // Show success message
            snackbarHostState.showSnackbar("User updated successfully")
            // Navigate back after showing the message
            onBackClick()
        } else if (editResult == false) {
            // Show error in UI
            snackbarHostState.showSnackbar("Failed to update user")
        }
    }

    // Function to update user with form values
    fun prepareUpdatedUser(): UserData? {
        val currentUser = editedUser ?: return null

        // Create updated address
        val updatedAddress = AddressData(
            street = street,
            city = addressCity,
            state = state,
            country = country,
            postalCode = postalCode,
            formattedAddress = "$street, $addressCity, $state $postalCode, $country"
        )

        // Create updated user
        return currentUser.copy(
            name = name,
            email = email,
            phone = phone,
            whatsapp = whatsapp.ifEmpty { null },
            city = city,
            district = district,
            role = selectedRole,
            address = updatedAddress
        )
    }

    // Save function
    fun saveChanges() {
        val updatedUser = prepareUpdatedUser()
        val originalUserCopy = originalUser

        if (updatedUser == null || originalUserCopy == null) {
            scope.launch {
                snackbarHostState.showSnackbar("Cannot update user: Missing data")
            }
            return
        }

        // Check if anything changed
        val hasChanges = viewModel.updateUser(updatedUser, originalUserCopy)

        if (!hasChanges) {
            // Show message that nothing changed
            scope.launch {
                snackbarHostState.showSnackbar("No changes were made to the user")
            }
        }
    }

    // Confirmation dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Changes") },
            text = { Text("Are you sure you want to save the changes to this user?") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        saveChanges()
                    }
                ) {
                    Text("Yes, Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit User") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showConfirmDialog = true }
                    ) {
                        Text("Save")
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
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
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // User ID (non-editable)
                    Text(
                        text = "User ID: ${originalUser?.id?.take(8)}...",
                        style = MaterialTheme.typography.bodySmall
                    )

                    // User basic information
                    Text(
                        text = "User Information",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = whatsapp,
                        onValueChange = { whatsapp = it },
                        label = { Text("WhatsApp (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Role dropdown with correct implementation
                    Text(
                        text = "User Role",
                        style = MaterialTheme.typography.titleMedium
                    )

                    ExposedDropdownMenuBox(
                        expanded = roleDropdownExpanded,
                        onExpandedChange = { roleDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedRole,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, "Dropdown arrow")
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = roleDropdownExpanded,
                            onDismissRequest = { roleDropdownExpanded = false }
                        ) {
                            roleOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        selectedRole = option
                                        roleDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Location fields
                    Text(
                        text = "Location",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("City") },
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = district,
                            onValueChange = { district = it },
                            label = { Text("District") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Address section
                    Text(
                        text = "Address Details",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = street,
                        onValueChange = { street = it },
                        label = { Text("Street") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = addressCity,
                            onValueChange = { addressCity = it },
                            label = { Text("City") },
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = state,
                            onValueChange = { state = it },
                            label = { Text("State") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = postalCode,
                            onValueChange = { postalCode = it },
                            label = { Text("Postal Code") },
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = country,
                            onValueChange = { country = it },
                            label = { Text("Country") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Save button
                    Button(
                        onClick = { showConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Changes")
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}