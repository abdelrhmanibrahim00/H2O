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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.h2o.store.ViewModels.Admin.ManageOrdersViewModel
import com.h2o.store.data.Orders.Order
import com.h2o.store.data.models.AddressData
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditOrderScreen(
    navController: NavHostController,
    viewModel: ManageOrdersViewModel,
    orderId: String,
    onBackClick: () -> Unit
) {

    LaunchedEffect(Unit) {
        // Reset the edit result when first entering the screen
        viewModel.resetEditOrderResult()
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    // States for the form
    var originalOrder by remember { mutableStateOf<Order?>(null) }
    var editedOrder by remember { mutableStateOf<Order?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Status dropdown
    val statusOptions = listOf("Pending", "Processing", "Shipped", "Delivered", "Cancelled")
    var selectedStatus by remember { mutableStateOf("") }
    var statusDropdownExpanded by remember { mutableStateOf(false) }

    // Address fields
    var street by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }

    // Payment method
    var paymentMethod by remember { mutableStateOf("") }

    // Amounts
    var subtotal by remember { mutableStateOf("") }
    var deliveryFee by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }

    // Date
    var estimatedDeliveryDate by remember { mutableStateOf("") }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Observe edit result
    val editResult by viewModel.editOrderResult.collectAsState()

    // Load order details
    LaunchedEffect(orderId) {
        isLoading = true
        try {
            val orderDetails = viewModel.getOrderDetails(orderId)
            originalOrder = orderDetails
            editedOrder = orderDetails?.copy()

            // Initialize form fields
            orderDetails?.let { order ->
                selectedStatus = order.status
                paymentMethod = order.paymentMethod
                subtotal = order.subtotal.toString()
                deliveryFee = order.deliveryFee.toString()
                total = order.total.toString()
                estimatedDeliveryDate = dateFormat.format(order.estimatedDelivery)

                // Initialize address fields
                order.deliveryAddress?.let { address ->
                    street = address.street
                    city = address.city
                    state = address.state
                    postalCode = address.postalCode
                    country = address.country
                }
            }
        } catch (e: Exception) {
            errorMessage = "Failed to load order details: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // Handle edit result
    LaunchedEffect(editResult) {
        if (editResult == true) {
            // Show success message
            snackbarHostState.showSnackbar("Order updated successfully")
            // Navigate back after showing the message
            onBackClick()
        } else if (editResult == false) {
            // Show error in UI
            snackbarHostState.showSnackbar("Failed to update order")
        }
    }

    // Function to update order with form values
    fun prepareUpdatedOrder(): Order? {
        val currentOrder = editedOrder ?: return null

        // Create updated address
        val updatedAddress = AddressData(
            street = street,
            city = city,
            state = state,
            country = country,
            postalCode = postalCode,
            formattedAddress = "$street, $city, $state $postalCode, $country"
        )

        // Parse amounts, defaulting to original values if parsing fails
        val updatedSubtotal = subtotal.toDoubleOrNull() ?: currentOrder.subtotal
        val updatedDeliveryFee = deliveryFee.toDoubleOrNull() ?: currentOrder.deliveryFee
        val updatedTotal = total.toDoubleOrNull() ?: currentOrder.total

        // Parse date, defaulting to original if parsing fails
        val updatedEstimatedDelivery = try {
            dateFormat.parse(estimatedDeliveryDate) ?: currentOrder.estimatedDelivery
        } catch (e: Exception) {
            currentOrder.estimatedDelivery
        }

        // Create updated order
        return currentOrder.copy(
            status = selectedStatus,
            paymentMethod = paymentMethod,
            subtotal = updatedSubtotal,
            deliveryFee = updatedDeliveryFee,
            total = updatedTotal,
            estimatedDelivery = updatedEstimatedDelivery,
            deliveryAddress = updatedAddress
        )
    }

    // Save function
    fun saveChanges() {
        val updatedOrder = prepareUpdatedOrder()
        val originalOrderCopy = originalOrder

        if (updatedOrder == null || originalOrderCopy == null) {
            scope.launch {
                snackbarHostState.showSnackbar("Cannot update order: Missing data")
            }
            return
        }

        // Check if anything changed
        val hasChanges = viewModel.updateOrder(updatedOrder, originalOrderCopy)

        if (!hasChanges) {
            // Show message that nothing changed
            scope.launch {
                snackbarHostState.showSnackbar("No changes were made to the order")
            }
        }
    }

    // Confirmation dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Changes") },
            text = { Text("Are you sure you want to save the changes to this order?") },
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
                title = { Text("Edit Order") },
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
                    // Order ID (non-editable)
                    Text(
                        text = "Order #${originalOrder?.orderId?.take(8)}",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    // Status dropdown with correct implementation
                    Text(
                        text = "Order Status",
                        style = MaterialTheme.typography.titleMedium
                    )

                    ExposedDropdownMenuBox(
                        expanded = statusDropdownExpanded,
                        onExpandedChange = { statusDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedStatus,
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
                            expanded = statusDropdownExpanded,
                            onDismissRequest = { statusDropdownExpanded = false }
                        ) {
                            statusOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        selectedStatus = option
                                        statusDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Payment method
                    Text(
                        text = "Payment Method",
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = { paymentMethod = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Delivery Address section
                    Text(
                        text = "Delivery Address",
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
                            value = city,
                            onValueChange = { city = it },
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

                    // Price information
                    Text(
                        text = "Order Pricing",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = subtotal,
                        onValueChange = { subtotal = it },
                        label = { Text("Subtotal") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = deliveryFee,
                        onValueChange = {
                            deliveryFee = it
                            // Optionally update total automatically
                            try {
                                val subTotal = subtotal.toDoubleOrNull() ?: 0.0
                                val delFee = it.toDoubleOrNull() ?: 0.0
                                total = (subTotal + delFee).toString()
                            } catch (e: Exception) {
                                // Ignore parsing errors during typing
                            }
                        },
                        label = { Text("Delivery Fee") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = total,
                        onValueChange = { total = it },
                        label = { Text("Total") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Estimated delivery date
                    Text(
                        text = "Estimated Delivery Date",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = estimatedDeliveryDate,
                        onValueChange = { estimatedDeliveryDate = it },
                        label = { Text("Date (yyyy-MM-dd)") },
                        modifier = Modifier.fillMaxWidth()
                    )

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