package com.h2o.store.ViewModels.Admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.h2o.store.data.Orders.Order
import com.h2o.store.repositories.Admin.OrderRepository
import com.h2o.store.repositories.ProductRepository
import com.h2o.store.repositories.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AdminViewModel(
    private val orderRepository: OrderRepository,
    private val profileRepository: ProfileRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val TAG = "AdminViewModel"

    // Fetch all orders regardless of userId
    fun fetchAllOrders() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                orderRepository.getAllOrdersFlow()
                    .catch { e ->
                        Log.e(TAG, "Error fetching orders: ${e.message}")
                        _errorMessage.value = "Failed to load orders: ${e.message}"
                        _isLoading.value = false
                    }
                    .collectLatest { orders ->
                        _orders.value = orders
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in fetchAllOrders: ${e.message}")
                _errorMessage.value = "Failed to load orders: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Fetch orders for a specific status
    fun fetchOrdersByStatus(status: String) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                orderRepository.getOrdersByStatusFlow(status)
                    .catch { e ->
                        Log.e(TAG, "Error fetching orders by status: ${e.message}")
                        _errorMessage.value = "Failed to load orders: ${e.message}"
                        _isLoading.value = false
                    }
                    .collectLatest { orders ->
                        _orders.value = orders
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in fetchOrdersByStatus: ${e.message}")
                _errorMessage.value = "Failed to load orders: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Update order status
    fun updateOrderStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                val success = orderRepository.updateOrderStatus(orderId, newStatus)
                if (!success) {
                    _errorMessage.value = "Failed to update order status"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error updating order: ${e.message}"
            }
        }
    }

    // Assign delivery personnel to an order
    fun assignDeliveryPersonnel(orderId: String, deliveryPersonnelId: String) {
        viewModelScope.launch {
            try {
                val success = orderRepository.assignDeliveryPersonnel(orderId, deliveryPersonnelId)
                if (!success) {
                    _errorMessage.value = "Failed to assign delivery personnel"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error assigning delivery personnel: ${e.message}"
            }
        }
    }

    // Get order details
    suspend fun getOrderDetails(orderId: String): Order? {
        return try {
            orderRepository.getOrderDetails(orderId)
        } catch (e: Exception) {
            _errorMessage.value = "Error fetching order details: ${e.message}"
            null
        }
    }

    // Calculate dashboard metrics from current order list
    fun getOrderMetrics(): Map<String, Int> {
        val currentOrders = orders.value

        return mapOf(
            "totalOrders" to currentOrders.size,
            "pendingOrders" to currentOrders.count { it.status == "Pending" },
            "inProgressOrders" to currentOrders.count { it.status == "In Progress" },
            "deliveredOrders" to currentOrders.count { it.status == "Delivered" },
            "cancelledOrders" to currentOrders.count { it.status == "Cancelled" }
        )
    }

    // Calculate revenue metrics from current order list
    fun getRevenueMetrics(): Map<String, Double> {
        val currentOrders = orders.value

        return mapOf(
            "totalRevenue" to currentOrders.sumOf { it.total },
            "averageOrderValue" to if (currentOrders.isNotEmpty())
                currentOrders.sumOf { it.total } / currentOrders.size else 0.0,
            "totalDeliveryFees" to currentOrders.sumOf { it.deliveryFee }
        )
    }

    // Factory for creating this ViewModel with dependencies
    class Factory(
        private val orderRepository: OrderRepository,
        private val profileRepository: ProfileRepository,
        private val productRepository: ProductRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
                return AdminViewModel(orderRepository, profileRepository, productRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}