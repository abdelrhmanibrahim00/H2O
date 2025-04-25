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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.h2o.store.ViewModels.Admin.AdminViewModel
import com.h2o.store.components.DateRangeDisplay
import com.h2o.store.components.EnhancedNotificationButton
import com.h2o.store.components.LogoutButton
import com.h2o.store.components.PeriodFilterButtons
import com.h2o.store.components.rememberDateRangePickerManager
import java.text.NumberFormat
import java.util.Locale


data class AdminDashboardItem(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    navController: NavHostController,
    adminViewModel: AdminViewModel,
    onManageProducts: () -> Unit,
    onManageOrders: () -> Unit,
    onManageUsers: () -> Unit,
    onViewReports: () -> Unit,
    onLogoutClick: () -> Unit,
    onOrderSelected: (String) -> Unit,
    onManageDeliveryConfig: () -> Unit
) {
    // Collect state from ViewModel
    val orders by adminViewModel.orders.collectAsState()
    val isLoading by adminViewModel.isLoading.collectAsState()
    val errorMessage by adminViewModel.errorMessage.collectAsState()
    val selectedPeriod by adminViewModel.selectedPeriod.collectAsState()
    val startDate by adminViewModel.startDate.collectAsState()
    val endDate by adminViewModel.endDate.collectAsState()

    // Date picker manager
    val dateRangePickerManager = rememberDateRangePickerManager()

    // Effect to fetch data when the screen is shown
    // Effect to fetch data only if needed (if no data is present)
    LaunchedEffect(key1 = true) {
        if (orders.isEmpty() && !isLoading) {
            // If we already set dates in the ViewModel init but haven't loaded data yet
            if (startDate != null && endDate != null) {
                adminViewModel.fetchOrdersByDateRange(startDate!!, endDate!!)
            } else {
                adminViewModel.fetchAllOrders()
            }
        }
    }

    // Calculate metrics
    val totalOrders = orders.size
    val totalRevenue = orders.sumOf { it.total }
    val pendingOrders = orders.count { it.status == "Pending" }
    val completedOrders = orders.count { it.status == "Delivered" }

    // Format currency
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                actions = {
                    // Enhanced Notification Button with priority system
                    EnhancedNotificationButton(
                        totalCount = adminViewModel.totalNotificationsCount.collectAsState().value,
                        hasUrgentNotifications = adminViewModel.hasUrgentNotifications.collectAsState().value,
                        notifications = adminViewModel.getAllNotificationsSorted(),
                        onNotificationClick = {
                            // Mark as read when notifications are viewed
                            adminViewModel.markAllNotificationsAsRead()
                        },
                        onNotificationItemClick = { orderId ->
                            // The parameter name here should match what you're passing in PrioritizedNotificationItem
                            onOrderSelected(orderId)
                        }
                    )

                    // Existing Logout Button
                    LogoutButton(onClick = { onLogoutClick() }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Welcome, Admin",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Period Filter Buttons
                PeriodFilterButtons(
                    selectedPeriod = selectedPeriod,
                    onWeekSelected = { adminViewModel.setWeekPeriod() },
                    onMonthSelected = { adminViewModel.setMonthPeriod() },
                    onCustomPeriodSelected = {
                        // Show start date picker first
                        dateRangePickerManager.showStartDatePicker(startDate) { newStartDate ->
                            // Then show end date picker
                            dateRangePickerManager.showEndDatePicker(
                                startDate = newStartDate,
                                initialDate = endDate
                            ) { newEndDate ->
                                // Set custom period with selected dates
                                adminViewModel.setCustomPeriod(newStartDate, newEndDate)
                            }
                        }
                    }
                )

                // Display selected date range
                DateRangeDisplay(startDate, endDate)

                // Display error message if any
                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }

                // Quick Stats Cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Total Orders",
                        value = totalOrders.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Revenue",
                        value = currencyFormat.format(totalRevenue),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Pending",
                        value = pendingOrders.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Completed",
                        value = completedOrders.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Admin Dashboard Grid
                val dashboardItems = listOf(
                    AdminDashboardItem(
                        title = "Manage Products",
                        icon = Icons.Default.Inventory,
                        onClick = onManageProducts
                    ),
                    AdminDashboardItem(
                        title = "Manage Orders",
                        icon = Icons.Default.ShoppingCart,
                        onClick = onManageOrders
                    ),
                    AdminDashboardItem(
                        title = "Manage Users",
                        icon = Icons.Default.People,
                        onClick = onManageUsers
                    ),
                    AdminDashboardItem(
                        title = "View Reports",
                        icon = Icons.Default.BarChart,
                        onClick = onViewReports
                    ),
                    // Add new dashboard item for delivery configuration
                    AdminDashboardItem(
                        title = "Delivery Configuration",
                        icon = Icons.Default.LocalShipping,
                        onClick = onManageDeliveryConfig
                    )
                )

                AdminDashboardGrid(items = dashboardItems)
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(80.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardGrid(items: List<AdminDashboardItem>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowItems.forEach { item ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp),
                        onClick = item.onClick
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                // If odd number of items, add an empty weight
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}