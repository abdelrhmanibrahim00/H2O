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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.derivedStateOf
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
import com.h2o.store.data.User.UserData
import com.h2o.store.data.models.AddressData
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
    val tabs = listOf("All", "Pending", "Processing", "Delivered", "Cancelled")
    val currentTab by viewModel.currentTab.collectAsState()

    // Collect state from ViewModel
    val allOrders by viewModel.allOrders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val deliveryPersonnel by viewModel.deliveryPersonnel.collectAsState()

    // Create a lazy list state to detect when we're near the end
    val listState = rememberLazyListState()

    // Effect to fetch orders when the screen is shown
    LaunchedEffect(key1 = true) {
        viewModel.fetchAllOrders()
        viewModel.fetchDeliveryPersonnel()
    }

    // Effect to apply filter when tab changes
    LaunchedEffect(key1 = currentTab) {
        viewModel.fetchOrdersForCurrentTab()
    }
    LaunchedEffect(currentTab) {
        viewModel.fetchOrdersForCurrentTab()
    }

    // Detect when we're near the end of the list to load more items
    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1

            // Pre-fetch when we're within 5 items of the end (more aggressive)
            lastVisibleItemIndex > (totalItemsNumber - 5)
        }
    }



    // Load more items when we're near the end
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !isLoading && !isLoadingMore) {
            if (viewModel.hasMoreItems()) {
                viewModel.loadMoreOrders()
            }
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
                            viewModel.updateDisplayedOrders()
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                }
            )

            // Tab layout for filtering
            ScrollableTabRow(selectedTabIndex = currentTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = currentTab == index,
                        onClick = { viewModel.setCurrentTab(index) },
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

            // Show loading indicator for initial load
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
                // Show orders list with pagination support
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    state = listState
                ) {
                    items(allOrders) { order ->
                        OrderCard(
                            order = order,
                            onCardClick = { onOrderDetails(order.orderId) },
                            viewModel = viewModel,
                            deliveryPersonnel = deliveryPersonnel
                        )
                    }

                    // Add pagination loading indicator if we're loading more
                    // Add this to the LazyColumn items
                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }

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
    onCardClick: () -> Unit,
    viewModel: ManageOrdersViewModel,
    deliveryPersonnel: List<UserData>
) {
    val selectedDeliveryPerson by viewModel.selectedDeliveryPerson.collectAsState()
    var expanded by remember { mutableStateOf(false) }

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
                    text = "${order.total}",
                    style = MaterialTheme.typography.titleSmall
                )
            }

            // Show delivery personnel selector and process button for pending orders
            if (order.status == "Pending") {
                Spacer(modifier = Modifier.height(16.dp))

                Column {
                    Text(
                        text = "Assign Delivery Person:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                selectedDeliveryPerson?.name ?: "Select Delivery Person",
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Select"
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            deliveryPersonnel.forEach { person ->
                                DropdownMenuItem(
                                    text = { Text(person.name) },
                                    onClick = {
                                        viewModel.selectDeliveryPerson(person)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Only show the "Process Order" button when a delivery person is selected
                    if (selectedDeliveryPerson != null) {
                        // State to control dialog visibility
                        val showConfirmDialog = remember { mutableStateOf(false) }

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                // Show the confirmation dialog instead of immediately updating
                                showConfirmDialog.value = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Process Order")
                        }

                        // Confirmation Dialog
                        if (showConfirmDialog.value) {
                            AlertDialog(
                                onDismissRequest = { showConfirmDialog.value = false },
                                title = { Text("Confirm Order Assignment") },
                                text = {
                                    Text("Are you sure you want to assign this order to ${selectedDeliveryPerson!!.name}?")
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            // Only update when confirmed
                                            viewModel.updateOrderToProcessing(
                                                order.orderId,
                                                selectedDeliveryPerson!!.id,
                                                selectedDeliveryPerson!!.name
                                            )
                                            showConfirmDialog.value = false
                                        }
                                    ) {
                                        Text("Yes")
                                    }
                                },
                                dismissButton = {
                                    Button(
                                        onClick = { showConfirmDialog.value = false }
                                    ) {
                                        Text("Cancel")
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