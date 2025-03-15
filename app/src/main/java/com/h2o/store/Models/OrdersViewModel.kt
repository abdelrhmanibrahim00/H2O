package com.h2o.store.Models

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.h2o.store.data.Orders.Order
import com.h2o.store.repositories.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class OrdersViewModel(
    private val userId: String,
    private val repository: OrderRepository
) : ViewModel() {
    private val TAG = "OrdersViewModel"

    // State for orders list
    private val _orderState = MutableStateFlow<OrdersState>(OrdersState(loading = true))
    val orderState: StateFlow<OrdersState> = _orderState

    // State for order details (when user clicks on an order)
    private val _selectedOrder = MutableStateFlow<OrderDetailsState>(OrderDetailsState())
    val selectedOrder: StateFlow<OrderDetailsState> = _selectedOrder

    init {
        Log.d(TAG, "Initializing OrdersViewModel for user: $userId")
        // Start collecting the orders flow
        startOrdersCollection()
    }

    private fun startOrdersCollection() {
        viewModelScope.launch {
            _orderState.value = OrdersState(loading = true)

            repository.getOrdersFlow(userId)
                .catch { e ->
                    Log.e(TAG, "Error collecting orders: ${e.message}")
                    _orderState.value = OrdersState(
                        loading = false,
                        error = "Failed to load orders: ${e.message}"
                    )
                }
                .collect { orders ->
                    Log.d(TAG, "Received ${orders.size} orders from flow")
                    _orderState.value = OrdersState(
                        orders = orders,
                        loading = false,
                        error = null
                    )
                }
        }
    }

    fun getOrderDetails(orderId: String) {
        Log.d(TAG, "Getting details for order: $orderId")
        viewModelScope.launch {
            _selectedOrder.value = OrderDetailsState(loading = true)

            try {
                val order = repository.getOrderDetails(orderId)
                if (order != null) {
                    Log.d(TAG, "Successfully loaded order details")
                    _selectedOrder.value = OrderDetailsState(
                        order = order,
                        loading = false,
                        error = null
                    )
                } else {
                    Log.e(TAG, "Order not found")
                    _selectedOrder.value = OrderDetailsState(
                        loading = false,
                        error = "Order not found"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading order details: ${e.message}")
                _selectedOrder.value = OrderDetailsState(
                    loading = false,
                    error = "Failed to load order details: ${e.message}"
                )
            }
        }
    }

    fun cancelOrder(orderId: String) {
        Log.d(TAG, "Attempting to cancel order: $orderId")
        viewModelScope.launch {
            try {
                val success = repository.cancelOrder(orderId)
                if (success) {
                    Log.d(TAG, "Order cancelled successfully")
                    // The orders flow will automatically update with the new status

                    // Update selected order if it's the one that was cancelled
                    val currentSelectedOrder = _selectedOrder.value.order
                    if (currentSelectedOrder != null && currentSelectedOrder.orderId == orderId) {
                        _selectedOrder.value = _selectedOrder.value.copy(
                            order = currentSelectedOrder.copy(status = "Cancelled")
                        )
                    }
                } else {
                    Log.e(TAG, "Failed to cancel order")
                    _orderState.value = _orderState.value.copy(
                        error = "Failed to cancel order"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling order: ${e.message}")
                _orderState.value = _orderState.value.copy(
                    error = "Error cancelling order: ${e.message}"
                )
            }
        }
    }

    // Reset error state
    fun clearError() {
        _orderState.value = _orderState.value.copy(error = null)
    }

    // UI helper methods

    // Get formatted order date
    fun getFormattedOrderDate(order: Order): String {
        return try {
            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            dateFormat.format(order.orderDate)
        } catch (e: Exception) {
            "Unknown date"
        }
    }

    // Get formatted estimated delivery date
    fun getFormattedEstimatedDelivery(order: Order): String {
        return try {
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            dateFormat.format(order.estimatedDelivery)
        } catch (e: Exception) {
            "Unknown date"
        }
    }

    // Get a color code based on order status
    fun getStatusColor(status: String): Color {
        return when (status.lowercase()) {
            "pending" -> Color(0xFFFFA000)  // Orange
            "processing" -> Color(0xFF2196F3)  // Blue
            "shipped" -> Color(0xFF9C27B0)  // Purple
            "delivered" -> Color(0xFF4CAF50)  // Green
            "cancelled" -> Color(0xFFF44336)  // Red
            else -> Color.Gray
        }
    }

    // Format order ID for display (last 6 characters)
    fun getFormattedOrderId(orderId: String): String {
        return "#${orderId.takeLast(6).uppercase()}"
    }

    // Check if order can be cancelled
    fun canCancelOrder(order: Order): Boolean {
        return order.status.lowercase() == "pending"
    }

    // State classes
    data class OrdersState(
        val orders: List<Order> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null
    )

    data class OrderDetailsState(
        val order: Order? = null,
        val loading: Boolean = false,
        val error: String? = null
    )

    // Factory for dependency injection with repository
    class Factory(
        private val userId: String,
        private val repository: OrderRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(OrdersViewModel::class.java)) {
                return OrdersViewModel(userId, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}