import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.h2o.store.ViewModels.Delivery.DeliveryViewModel
import com.h2o.store.data.Orders.Order
import com.h2o.store.data.models.AddressData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    // Define the tabs - make sure these match what you want to show
    val tabs = listOf("In Progress", "Delivered", "Other")

    // Collect state from ViewModel
    val allOrders by viewModel.allOrders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Effect to fetch orders for this delivery person when the screen is shown
    LaunchedEffect(key1 = true) {
        viewModel.fetchOrdersForDeliveryPerson()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Delivery Orders") },
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
                    0 -> allOrders.filter { it.status == "In Progress" || it.status == "Processing" || it.status == "Assigned" }
                    1 -> allOrders.filter { it.status == "Delivered" }
                    2 -> allOrders.filter { it.status != "In Progress" && it.status != "Processing" &&
                            it.status != "Assigned" && it.status != "Delivered" }
                    else -> emptyList()
                }

                if (filteredOrders.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No ${tabs[selectedTab].lowercase()} orders assigned to you",
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
                                    // If status is changed to "Shipped", navigate to delivery screen
                                    if (newStatus == "Shipped") {
                                        viewModel.updateOrderStatus(order.orderId, newStatus)
                                        navController.navigate("orderDelivery/${order.orderId}")
                                    } else {
                                        viewModel.updateOrderStatus(order.orderId, newStatus)
                                    }
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
    // State for showing the confirmation dialog
    var showConfirmDialog by remember { mutableStateOf(false) }

    // If dialog should be shown
    if (showConfirmDialog) {
        AlertDialog(
            title = { Text("Start Delivery") },
            text = { Text("Are you sure you want to start delivery for this order? You will be navigated to the delivery screen.") },
            confirmButton = {
                Button(
                    onClick = {
                        // Change status to "Shipped" and navigate to delivery screen
                        onStatusChange("Shipped")
                        showConfirmDialog = false
                    }
                ) {
                    Text("Yes, Start Delivery")
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
                        "Assigned" -> MaterialTheme.colorScheme.primary
                        "In Progress", "Processing" -> MaterialTheme.colorScheme.tertiary
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
                    "Processing", "In Progress", "Assigned" -> {
                        Button(
                            onClick = { showConfirmDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Start Delivery")
                        }
                    }
                    "Delivered", "Cancelled", "Shipped" -> {
                        // No actions for delivered or cancelled or shipped orders
                    }
                    else -> {
                        // No actions for other statuses
                    }
                }
            }
        }
    }
}

// Helper functions
private fun formatAddress(address: AddressData): String {
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