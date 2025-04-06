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
import java.util.Calendar
import java.util.Date

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

    // Added for period filtering
    private val _selectedPeriod = MutableStateFlow("week")
    val selectedPeriod: StateFlow<String> = _selectedPeriod

    // Custom date range
    private val _startDate = MutableStateFlow<Date?>(null)
    val startDate: StateFlow<Date?> = _startDate

    private val _endDate = MutableStateFlow<Date?>(null)
    val endDate: StateFlow<Date?> = _endDate

    private val TAG = "AdminViewModel"

    init {
        // Set default to current week and immediately load data
        setDefaultWeekPeriod()
        fetchOrdersByDateRange(_startDate.value!!, _endDate.value!!)
    }

    // Set period to current week
    private fun setDefaultWeekPeriod() {
        val calendar = Calendar.getInstance()
        val endDate = calendar.time

        // Go back 7 days for the start date
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startDate = calendar.time

        _startDate.value = startDate
        _endDate.value = endDate
        _selectedPeriod.value = "week"
    }

    // Set period to current month
    fun setMonthPeriod() {
        val calendar = Calendar.getInstance()
        val endDate = calendar.time

        // Set to first day of current month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startDate = calendar.time

        _startDate.value = startDate
        _endDate.value = endDate
        _selectedPeriod.value = "month"

        // Fetch orders with the new date range
        fetchOrdersByDateRange(startDate, endDate)
    }

    // Set period to current week
    fun setWeekPeriod() {
        setDefaultWeekPeriod()

        // Fetch orders with the new date range
        fetchOrdersByDateRange(_startDate.value!!, _endDate.value!!)
    }

    // Set custom period
    fun setCustomPeriod(start: Date, end: Date) {
        _startDate.value = start
        _endDate.value = end
        _selectedPeriod.value = "custom"

        // Fetch orders with the new date range
        fetchOrdersByDateRange(start, end)
    }

    // Fetch orders by date range
    fun fetchOrdersByDateRange(startDate: Date, endDate: Date) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                orderRepository.getOrdersByDateRangeFlow(startDate, endDate)
                    .catch { e ->
                        Log.e(TAG, "Error fetching orders by date range: ${e.message}")
                        _errorMessage.value = "Failed to load orders: ${e.message}"
                        _isLoading.value = false
                    }
                    .collectLatest { orders ->
                        _orders.value = orders
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in fetchOrdersByDateRange: ${e.message}")
                _errorMessage.value = "Failed to load orders: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Fetch all orders regardless of userId (keep for backward compatibility)
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