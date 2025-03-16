package com.h2o.store.ViewModels.Admin


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.h2o.store.data.Orders.Order
import com.h2o.store.data.models.AddressData
import com.h2o.store.repositories.Admin.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ManageOrdersViewModel(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _allOrders = MutableStateFlow<List<Order>>(emptyList())
    val allOrders: StateFlow<List<Order>> = _allOrders

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // State for edit operation result
    private val _editOrderResult = MutableStateFlow<Boolean?>(null)
    val editOrderResult: StateFlow<Boolean?> = _editOrderResult

    private val TAG = "ManageOrdersViewModel"

    // Fetch all orders
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
                        _allOrders.value = orders
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in fetchAllOrders: ${e.message}")
                _errorMessage.value = "Failed to load orders: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Filter orders by status
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

    // Update order status (Processing)
    fun updateOrderToProcessing(orderId: String): StateFlow<Boolean> {
        val result = MutableStateFlow(false)

        viewModelScope.launch {
            try {
                val success = orderRepository.updateOrderStatus(orderId, "Processing")
                if (success) {
                    Log.d(TAG, "Order $orderId status updated to Processing")
                    result.value = true
                } else {
                    Log.e(TAG, "Failed to update order $orderId status")
                    _errorMessage.value = "Failed to update order status"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating order status: ${e.message}")
                _errorMessage.value = "Error updating order: ${e.message}"
            }
        }

        return result
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

    // Filter by text search
    fun filterOrdersByText(query: String) {
        if (query.isBlank()) {
            fetchAllOrders()
            return
        }

        viewModelScope.launch {
            try {
                // We'll implement client-side filtering since Firestore doesn't support full-text search without extensions
                val allOrdersList = _allOrders.value
                val filteredOrders = allOrdersList.filter { order ->
                    // Search in orderId
                    order.orderId.contains(query, ignoreCase = true) ||
                            // Search in user-related information if available
                            order.toString().contains(query, ignoreCase = true)
                }

                // Update the state with filtered orders
                _allOrders.value = filteredOrders
            } catch (e: Exception) {
                Log.e(TAG, "Error filtering orders: ${e.message}")
                _errorMessage.value = "Error filtering orders: ${e.message}"
            }
        }
    }
    // Function to update an order
    fun updateOrder(order: Order, originalOrder: Order): Boolean {
        // Do a proper comparison by checking each field that we allow editing
        val hasChanges = !(
                order.status == originalOrder.status &&
                        order.paymentMethod == originalOrder.paymentMethod &&
                        order.deliveryFee == originalOrder.deliveryFee &&
                        order.subtotal == originalOrder.subtotal &&
                        order.total == originalOrder.total &&
                        compareAddresses(order.deliveryAddress, originalOrder.deliveryAddress) &&
                        order.estimatedDelivery.time == originalOrder.estimatedDelivery.time
                )

        // If nothing has changed, return false immediately
        if (!hasChanges) {
            Log.d(TAG, "No changes detected in order ${order.orderId}")
            return false
        }

        // Log what changed for debugging
        Log.d(TAG, "Changes detected in order ${order.orderId}:")
        if (order.status != originalOrder.status)
            Log.d(TAG, "Status changed: ${originalOrder.status} -> ${order.status}")
        if (order.paymentMethod != originalOrder.paymentMethod)
            Log.d(TAG, "Payment method changed: ${originalOrder.paymentMethod} -> ${order.paymentMethod}")
        if (order.deliveryFee != originalOrder.deliveryFee)
            Log.d(TAG, "Delivery fee changed: ${originalOrder.deliveryFee} -> ${order.deliveryFee}")
        if (order.subtotal != originalOrder.subtotal)
            Log.d(TAG, "Subtotal changed: ${originalOrder.subtotal} -> ${order.subtotal}")
        if (order.total != originalOrder.total)
            Log.d(TAG, "Total changed: ${originalOrder.total} -> ${order.total}")
        if (!compareAddresses(order.deliveryAddress, originalOrder.deliveryAddress))
            Log.d(TAG, "Address changed")
        if (order.estimatedDelivery.time != originalOrder.estimatedDelivery.time)
            Log.d(TAG, "Estimated delivery changed")

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _editOrderResult.value = null

            try {
                val success = orderRepository.updateOrder(order)
                _editOrderResult.value = success

                if (success) {
                    Log.d(TAG, "Successfully updated order ${order.orderId}")
                } else {
                    _errorMessage.value = "Failed to update order"
                    Log.e(TAG, "Failed to update order ${order.orderId}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating order: ${e.message}")
                _errorMessage.value = "Error updating order: ${e.message}"
                _editOrderResult.value = false
            } finally {
                _isLoading.value = false
            }
        }

        return true
    }

    // Helper method to compare addresses
    private fun compareAddresses(address1: AddressData?, address2: AddressData?): Boolean {
        if (address1 == null && address2 == null) return true
        if (address1 == null || address2 == null) return false

        return address1.street == address2.street &&
                address1.city == address2.city &&
                address1.state == address2.state &&
                address1.country == address2.country &&
                address1.postalCode == address2.postalCode
    }

    // Reset edit result state
    fun resetEditOrderResult() {
        _editOrderResult.value = null
    }

    // Factory for creating this ViewModel with dependencies
    class Factory(private val orderRepository: OrderRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ManageOrdersViewModel::class.java)) {
                return ManageOrdersViewModel(orderRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}