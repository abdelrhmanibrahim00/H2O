package com.h2o.store.Screens.Admin

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.h2o.store.ViewModels.Admin.ManageOrdersViewModel
import com.h2o.store.data.Orders.Order
import com.h2o.store.data.Orders.OrderItem
import kotlinx.coroutines.flow.StateFlow
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrderDetailsScreen(
    navController: NavHostController,
    viewModel: ManageOrdersViewModel,
    orderId: String,
    onBackClick: () -> Unit,
    onEditClick : (String) -> Unit
) {
    var order by remember { mutableStateOf<Order?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showStatusDialog by remember { mutableStateOf(false) }

    // Format currency
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    // Load order details
    LaunchedEffect(orderId) {
        isLoading = true
        try {
            val orderDetails = viewModel.getOrderDetails(orderId)
            order = orderDetails
        } catch (e: Exception) {
            errorMessage = "Failed to load order details: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // Status change result
    val statusChangeResult = remember { mutableStateOf<StateFlow<Boolean>?>(null) }
    statusChangeResult.value?.collectAsState()?.value?.let { success ->
        if (success) {
            LaunchedEffect(Unit) {
                // Refresh order details after status update
                val updatedOrder = viewModel.getOrderDetails(orderId)
                order = updatedOrder
            }
        }
    }

    if (showStatusDialog) {
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text("Change Order Status") },
            text = { Text("Do you want to change this order's status to Processing?") },
            confirmButton = {
                Button(
                    onClick = {
                        statusChangeResult.value = viewModel.updateOrderToProcessing(orderId)
                        showStatusDialog = false
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStatusDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEditClick(orderId) }) {
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
            } else if (order != null) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Order ID and Status
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Order #${order?.orderId?.take(8)}",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Text(
                                    text = "Placed on ${formatDate(order?.orderDate)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            StatusChip(status = order?.status ?: "Unknown")
                        }
                        Divider(modifier = Modifier.padding(vertical = 16.dp))
                    }

                    // Customer Information (if available)
                    item {
                        Text(
                            text = "Customer Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // If you have customer info in the order, display it here
                        Text(
                            text = "User ID: ${order?.userId}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // Delivery Address
                        order?.deliveryAddress?.let { address ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Delivery Address:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatFullAddress(address),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 16.dp))
                    }

                    // Order Items
                    item {
                        Text(
                            text = "Order Items",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // List all items
                    items(order?.items ?: emptyList()) { item ->
                        OrderItemCard(item = item)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Order Summary
                    item {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = "Order Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Payment details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Payment Method:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = order?.paymentMethod ?: "Unknown",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Subtotal
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Subtotal:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = currencyFormat.format(order?.subtotal ?: 0.0),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Delivery Fee
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
                                text = currencyFormat.format(order?.deliveryFee ?: 0.0),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Total
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currencyFormat.format(order?.total ?: 0.0),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Change status button (only show for Pending orders)
                    if (order?.status == "Pending") {
                        item {
                            Button(
                                onClick = { showStatusDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Change Status to Processing")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderItemCard(item: OrderItem) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product image (if available)
            if (item.productImage.isNotEmpty()) {
                AsyncImage(
                    model = item.productImage,
                    contentDescription = "Product image",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder if no image
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = "No image",
                        tint = Color.White
                    )
                }
            }

            // Item details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = item.productName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Quantity: ${item.quantity}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Price
            Text(
                text = "$${item.price * item.quantity}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Helper function for formatting the full address
private fun formatFullAddress(address: com.h2o.store.data.models.AddressData): String {
    return buildString {
        append(address.street)
        if (address.city.isNotEmpty()) {
            append("\n${address.city}")
        }
        if (address.state.isNotEmpty()) {
            append(", ${address.state}")
        }
        if (address.postalCode.isNotEmpty()) {
            append(" ${address.postalCode}")
        }
        if (address.country.isNotEmpty()) {
            append("\n${address.country}")
        }
    }.ifEmpty { "No address provided" }
}

private fun formatDate(date: Date?): String {
    if (date == null) return "Unknown date"

    val format = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    return format.format(date)
}