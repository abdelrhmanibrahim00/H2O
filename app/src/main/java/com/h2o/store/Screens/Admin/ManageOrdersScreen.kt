package com.h2o.store.Screens.Admin


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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.h2o.store.ViewModels.Admin.ManageOrdersViewModel
import com.h2o.store.data.Orders.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageOrdersScreen(
    navController: NavHostController,
    viewModel: ManageOrdersViewModel,
    onOrderDetails: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("All", "Pending", "Processing", "Delivered", "Cancelled")

    // Collect state from ViewModel
    val allOrders by viewModel.allOrders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Effect to fetch orders when the screen is shown
    LaunchedEffect(key1 = true) {
        viewModel.fetchAllOrders()
    }

    // Effect to apply filter when tab changes
    LaunchedEffect(key1 = selectedTab) {
        when (selectedTab) {
            0 -> viewModel.fetchAllOrders()
            else -> viewModel.fetchOrdersByStatus(tabs[selectedTab])
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Orders") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.filterOrdersByText(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search orders...") },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            viewModel.fetchAllOrders()
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                }
            )

            // Tab layout for filtering
            ScrollableTabRow(selectedTabIndex = selectedTab) {
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
            } else if (allOrders.isEmpty()) {
                // Show empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No orders found",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                // Show orders list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allOrders) { order ->
                        OrderCard(
                            order = order,
                            onCardClick = { onOrderDetails(order.orderId) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderCard(
    order: Order,
    onCardClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onCardClick
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
                StatusChip(status = order.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Show order items summary
            if (order.items.isNotEmpty()) {
                Text(
                    text = "${order.items.size} items",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Display first item as an example
                val firstItem = order.items.first()
                Text(
                    text = "${firstItem.productName} (${firstItem.quantity}x) " +
                            "${if (order.items.size > 1) "and more..." else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Display delivery address summary if available
            order.deliveryAddress?.let { address ->
                Text(
                    text = "To: ${formatAddress(address)}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Display date and total
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDate(order.orderDate),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "$${order.total}",
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val chipColor = when (status) {
        "Pending" -> MaterialTheme.colorScheme.primary
        "Processing" -> MaterialTheme.colorScheme.tertiary
        "Delivered" -> MaterialTheme.colorScheme.secondary
        "Cancelled" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }

    Surface(
        color = chipColor.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status,
            color = chipColor,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
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