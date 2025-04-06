package com.h2o.store.Screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.h2o.store.ViewModels.Delivery.DeliveryViewModel
import com.h2o.store.data.Orders.Order
import com.h2o.store.data.Orders.OrderItem
import com.h2o.store.data.User.UserData
import com.h2o.store.data.models.AddressData
import com.h2o.store.repositories.UserRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDeliveryScreen(
    navController: NavController,
    viewModel: DeliveryViewModel,
    orderId: String
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var order by remember { mutableStateOf<Order?>(null) }
    var customer by remember { mutableStateOf<UserData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }


    // Fetch order and customer details
    LaunchedEffect(orderId) {
        isLoading = true
        try {
            // Get order details
            val orderDetails = viewModel.getOrderDetails(orderId)
            order = orderDetails

            if (orderDetails != null) {
                // Get customer details using their ID
                val userRepository = UserRepository()
                val userData = userRepository.getUserData(orderDetails.userId)
                customer = userData
            }

            isLoading = false
        } catch (e: Exception) {
            errorMessage = "Error loading details: ${e.message}"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Delivery Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else if (order != null) {
                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Order ID and Status
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Order #${order!!.orderId.take(8)}",
                                style = MaterialTheme.typography.titleLarge
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Status: ${order!!.status}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = when (order!!.status) {
                                    "Shipped" -> MaterialTheme.colorScheme.primary
                                    "Delivered" -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Date: ${formatDate(order!!.orderDate)}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Payment Method: ${order!!.paymentMethod}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Order Items
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Order Items",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            order!!.items.forEach { item ->
                                OrderItemRow(item)
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                            }

                            // Order Summary
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Subtotal:",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "$${order!!.subtotal}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Delivery Fee:",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "$${order!!.deliveryFee}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$${order!!.total}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Customer Information
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Customer Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (customer != null) {
                                Text(
                                    text = "Name: ${customer!!.name}",
                                    style = MaterialTheme.typography.bodyLarge
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Phone with call button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Phone: ${customer!!.phone}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (customer!!.phone.isNotEmpty()) {
                                        IconButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                                    data = Uri.parse("tel:${customer!!.phone}")
                                                }
                                                context.startActivity(intent)
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Call,
                                                contentDescription = "Call customer",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "Customer information not available",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Delivery Address with navigation button
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Delivery Address",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            order!!.deliveryAddress?.let { address ->
                                val fullAddress = formatAddressForDisplay(address)

                                Text(
                                    text = fullAddress,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Navigation Button
                                Button(
                                    onClick = {
                                        // Open Google Maps or other navigation app with the address
                                        val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(fullAddress)}")
                                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                        mapIntent.setPackage("com.google.android.apps.maps") // Try Google Maps first

                                        // If Google Maps is not installed, open with any available map app
                                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                                            context.startActivity(mapIntent)
                                        } else {
                                            // Fallback to browser with Google Maps
                                            val browserIntent = Intent(Intent.ACTION_VIEW,
                                                Uri.parse("https://maps.google.com/maps?q=${Uri.encode(fullAddress)}"))
                                            context.startActivity(browserIntent)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Map,
                                        contentDescription = "Navigate",
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text("Navigate to Address")
                                }
                            } ?: run {
                                Text(
                                    text = "Delivery address not available",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Mark as Delivered Button
                    // Add a state to track if the dialog is showing

// Add the confirmation dialog
                    if (showConfirmDialog) {
                        AlertDialog(
                            title = { Text("Confirm Delivery") },
                            text = { Text("Are you sure you want to mark this order as delivered? This action cannot be undone.") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.updateOrderStatus(orderId, "Delivered")
                                            showConfirmDialog = false
                                            navController.popBackStack()
                                        }
                                    }
                                ) {
                                    Text("Confirm Delivery")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showConfirmDialog = false }
                                ) {
                                    Text("Cancel")
                                }
                            },
                            onDismissRequest = { showConfirmDialog = false }
                        )
                    }

// Mark as Delivered Button - now just shows the dialog
                    Button(
                        onClick = { showConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Mark as Delivered",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Mark as Delivered")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun OrderItemRow(item: OrderItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.productName,
                style = MaterialTheme.typography.bodyLarge
            )

            item.productDescription?.let {
                if (it.isNotEmpty()) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Text(
            text = "${item.quantity}x",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Text(
            text = "$${item.price}",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun formatAddressForDisplay(address: AddressData): String {
    return buildString {
        append(address.street)
        if (address.city.isNotEmpty()) {
            append(", ${address.city}")
        }
        if (address.state.isNotEmpty()) {
            append(", ${address.state}")
        }
        if (address.postalCode.isNotEmpty()) {
            append(" ${address.postalCode}")
        }
        if (address.country.isNotEmpty()) {
            append(", ${address.country}")
        }
    }
}

private fun formatDate(date: java.util.Date?): String {
    if (date == null) return "Unknown date"

    val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return format.format(date)
}