package com.h2o.store.Screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.h2o.store.ViewModels.User.DeliveryViewModel
import com.h2o.store.data.Orders.Order
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryHomeScreen(
    navController: NavHostController,
    viewModel: DeliveryViewModel,
    onOrderSelected: (String) -> Unit,
    onProfileClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Pending", "In Progress", "Delivered")

    // Collect state from ViewModel
    val allOrders by viewModel.allOrders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Effect to fetch orders when the screen is shown
    LaunchedEffect(key1 = true) {
        viewModel.fetchAllDeliveryOrders()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Delivery Dashboard") },
                actions = {
                    IconButton(onClick = { onProfileClick() }) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                    IconButton(onClick = { onLogoutClick() }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab layout for different order statuses
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // Show error message if any
            errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Show loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Filter orders based on selected tab/status
                val filteredOrders = when (selectedTab) {
                    0 -> allOrders.filter { it.status == "Pending" || it.status == "Assigned" }
                    1 -> allOrders.filter { it.status == "In Progress" }
                    2 -> allOrders.filter { it.status == "Delivered" }
                    else -> emptyList()
                }

                if (filteredOrders.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No ${tabs[selectedTab].lowercase()} orders",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredOrders) { order ->
                            DeliveryOrderCard(
                                order = order,
                                onOrderClick = { onOrderSelected(order.orderId) },
                                onStatusChange = { newStatus ->
                                    viewModel.updateOrderStatus(order.orderId, newStatus)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryOrderCard(
    order: Order,
    onOrderClick: () -> Unit,
    onStatusChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOrderClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Order #${order.orderId.take(8)}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = order.status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (order.status) {
                        "Pending", "Assigned" -> MaterialTheme.colorScheme.primary
                        "In Progress" -> MaterialTheme.colorScheme.tertiary
                        "Delivered" -> MaterialTheme.colorScheme.secondary
                        "Cancelled" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Show order items summary
            if (order.items.isNotEmpty()) {
                Text(
                    text = "Items: ${order.items.size} products",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Display first item as an example
                val firstItem = order.items.first()
                Text(
                    text = "${firstItem.productName} (${firstItem.quantity}x) " +
                            "${if (order.items.size > 1) "and more..." else ""}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Display delivery address
            order.deliveryAddress?.let { address ->
                Text(
                    text = "Address: ${formatAddress(address)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = "Date: ${formatDate(order.orderDate)}",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Total: $${order.total}",
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status change buttons based on current status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (order.status) {
                    "Pending", "Assigned" -> {
                        Button(
                            onClick = { onStatusChange("In Progress") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Start Delivery")
                        }
                    }
                    "In Progress" -> {
                        Button(
                            onClick = { onStatusChange("Delivered") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Mark Delivered")
                        }
                    }
                    "Delivered" -> {
                        // No actions for delivered orders
                    }
                }
            }
        }
    }
}

// Helper functions
private fun formatAddress(address: com.h2o.store.data.models.AddressData): String {
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
    }.ifEmpty { "No address provided" }
}

private fun formatDate(date: Date?): String {
    if (date == null) return "Unknown date"

    val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return format.format(date)
}