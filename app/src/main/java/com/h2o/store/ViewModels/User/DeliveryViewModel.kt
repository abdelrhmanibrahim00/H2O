package com.h2o.store.ViewModels.User

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.h2o.store.data.Orders.Order
import com.h2o.store.repositories.Admin.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DeliveryViewModel(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _allOrders = MutableStateFlow<List<Order>>(emptyList())
    val allOrders: StateFlow<List<Order>> = _allOrders

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val TAG = "DeliveryViewModel"

    // Get all delivery orders
    fun fetchAllDeliveryOrders() {
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
                        _allOrders.value = orders
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in fetchAllDeliveryOrders: ${e.message}")
                _errorMessage.value = "Failed to load orders: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Get orders by status
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
                        _allOrders.value = orders
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in fetchOrdersByStatus: ${e.message}")
                _errorMessage.value = "Failed to load orders: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Update order status (e.g., from "Pending" to "In Progress" or "Delivered")
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

    // Get order details
    suspend fun getOrderDetails(orderId: String): Order? {
        return try {
            orderRepository.getOrderDetails(orderId)
        } catch (e: Exception) {
            _errorMessage.value = "Error fetching order details: ${e.message}"
            null
        }
    }

    // Factory for creating this ViewModel with dependencies
    class Factory(private val orderRepository: OrderRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DeliveryViewModel::class.java)) {
                return DeliveryViewModel(orderRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}