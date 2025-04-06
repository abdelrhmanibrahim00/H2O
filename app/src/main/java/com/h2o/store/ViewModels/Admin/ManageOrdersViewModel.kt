package com.h2o.store.ViewModels.Admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentSnapshot
import com.h2o.store.data.Orders.Order
import com.h2o.store.data.User.UserData
import com.h2o.store.data.models.AddressData
import com.h2o.store.repositories.Admin.OrderRepository
import com.h2o.store.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ManageOrdersViewModel(
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _allOrders = MutableStateFlow<List<Order>>(emptyList())
    val allOrders: StateFlow<List<Order>> = _allOrders

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // State for edit operation result
    private val _editOrderResult = MutableStateFlow<Boolean?>(null)
    val editOrderResult: StateFlow<Boolean?> = _editOrderResult

    // State for delivery personnel
    private val _deliveryPersonnel = MutableStateFlow<List<UserData>>(emptyList())
    val deliveryPersonnel: StateFlow<List<UserData>> = _deliveryPersonnel

    private val _selectedDeliveryPerson = MutableStateFlow<UserData?>(null)
    val selectedDeliveryPerson: StateFlow<UserData?> = _selectedDeliveryPerson

    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab

    // Store orders by status (for better state management)
    private val _activeOrders = MutableStateFlow<List<Order>>(emptyList())
    private val _deliveredOrders = MutableStateFlow<List<Order>>(emptyList())
    private val _cancelledOrders = MutableStateFlow<List<Order>>(emptyList())

    // Pagination state
    private var lastDeliveredDocumentSnapshot: DocumentSnapshot? = null
    private var lastCancelledDocumentSnapshot: DocumentSnapshot? = null
    private var hasMoreDeliveredOrders = true
    private var hasMoreCancelledOrders = true
    // Hold pagination state for each tab
    private val paginationStateMap = mutableMapOf<Int, PaginationState>()




    private val TAG = "ManageOrdersViewModel"

    fun setCurrentTab(tab: Int) {
        Log.d(TAG, "Setting current tab from ${_currentTab.value} to $tab")
        _currentTab.value = tab
        // Clear orders immediately to avoid showing old orders
        _allOrders.value = emptyList()
        // Fetch new data for this tab
        fetchOrdersForCurrentTab()
    }



    // Fetch all orders - using optimized strategy
    fun fetchAllOrders() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                // Launch parallel requests for active, recent delivered, and recent cancelled orders
                launch { fetchActiveOrders() }
                launch { fetchRecentDeliveredOrders() }
                launch { fetchRecentCancelledOrders() }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in fetchAllOrders: ${e.message}")
                _errorMessage.value = "Failed to load orders: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Unified method to load orders for any tab
    private fun loadOrders(
        tab: Int,
        refresh: Boolean = false,
        onComplete: () -> Unit = {}
    ) {
        val isInitialLoad = refresh || !paginationStateMap.containsKey(tab)

        // Set loading state based on whether this is an initial load or pagination
        if (isInitialLoad) {
            _isLoading.value = true
        } else {
            _isLoadingMore.value = true
        }

        // Clear error state
        _errorMessage.value = null

        // Get current pagination state or create new one
        val paginationState = if (refresh) {
            PaginationState() // Create fresh state for refresh
        } else {
            paginationStateMap[tab] ?: PaginationState()
        }

        // If refreshing or initial load, clear the current list
        if (isInitialLoad) {
            _allOrders.value = emptyList()
        }

        // If we've already determined there are no more items, don't load
        if (!paginationState.hasMoreItems && !refresh) {
            _isLoadingMore.value = false
            _isLoading.value = false
            onComplete()
            return
        }

        // Determine parameters based on tab
        val (status, userId, deliveryPersonId) = when (tab) {
            0 -> Triple(null, null, null) // All orders
            1 -> Triple("Pending", null, null) // Pending orders
            2 -> Triple("Processing", null, null) // Processing orders
            3 -> Triple("Delivered", null, null) // Delivered orders
            4 -> Triple("Cancelled", null, null) // Cancelled orders
            else -> Triple(null, null, null)
        }

        viewModelScope.launch {
            try {
                orderRepository.getPaginatedOrdersFlow(
                    status = status,
                    userId = userId,
                    deliveryPersonId = deliveryPersonId,
                    limit = 20,
                    lastDocumentSnapshot = if (isInitialLoad) null else paginationState.lastDocumentSnapshot
                ).collectLatest { result ->
                    // Update pagination state
                    paginationStateMap[tab] = paginationState.copy(
                        lastDocumentSnapshot = result.lastDocumentSnapshot,
                        hasMoreItems = result.hasMore,
                        isLoading = false  // Changed to match the field name in PaginationState
                    )

                    // Update orders list
                    _allOrders.value = if (isInitialLoad) {
                        result.items
                    } else {
                        _allOrders.value + result.items
                    }

                    // Reset loading states
                    _isLoading.value = false
                    _isLoadingMore.value = false

                    // Call completion callback
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading orders for tab $tab: ${e.message}")
                _errorMessage.value = "Failed to load orders: ${e.message}"
                _isLoading.value = false
                _isLoadingMore.value = false
                onComplete()
            }
        }
    }


    // Fetch active orders (Pending and Processing)
    private fun fetchActiveOrders() {
        viewModelScope.launch {
            try {
                orderRepository.getActiveOrdersFlow()
                    .catch { e ->
                        Log.e(TAG, "Error fetching active orders: ${e.message}")
                        _errorMessage.value = "Failed to load active orders: ${e.message}"
                    }
                    .collectLatest { orders ->
                        _activeOrders.value = orders
                        updateDisplayedOrders()
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in fetchActiveOrders: ${e.message}")
                _errorMessage.value = "Failed to load active orders: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Fetch recent delivered orders (last 30 days)
    private fun fetchRecentDeliveredOrders() {
        viewModelScope.launch {
            try {
                orderRepository.getRecentCompletedOrdersFlow("Delivered", 30, 20)
                    .catch { e ->
                        Log.e(TAG, "Error fetching delivered orders: ${e.message}")
                        _errorMessage.value = "Failed to load delivered orders: ${e.message}"
                    }
                    .collectLatest { orders ->
                        _deliveredOrders.value = orders
                        updateDisplayedOrders()
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in fetchRecentDeliveredOrders: ${e.message}")
                _errorMessage.value = "Failed to load delivered orders: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Fetch recent cancelled orders (last 30 days)
    private fun fetchRecentCancelledOrders() {
        viewModelScope.launch {
            try {
                orderRepository.getRecentCompletedOrdersFlow("Cancelled", 30, 20)
                    .catch { e ->
                        Log.e(TAG, "Error fetching cancelled orders: ${e.message}")
                        _errorMessage.value = "Failed to load cancelled orders: ${e.message}"
                    }
                    .collectLatest { orders ->
                        _cancelledOrders.value = orders
                        updateDisplayedOrders()
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in fetchRecentCancelledOrders: ${e.message}")
                _errorMessage.value = "Failed to load cancelled orders: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Load more delivered orders (pagination)
    fun loadMoreDeliveredOrders() {
        if (!hasMoreDeliveredOrders || _isLoadingMore.value) return

        _isLoadingMore.value = true
        viewModelScope.launch {
            try {
                orderRepository.getCompletedOrdersPaginatedFlow("Delivered", 20, lastDeliveredDocumentSnapshot)
                    .catch { e ->
                        Log.e(TAG, "Error loading more delivered orders: ${e.message}")
                        _errorMessage.value = "Failed to load more orders: ${e.message}"
                        _isLoadingMore.value = false
                    }
                    .collectLatest { result ->
                        // Append new orders to existing list
                        _deliveredOrders.value = _deliveredOrders.value + result.items

                        // Update pagination state
                        lastDeliveredDocumentSnapshot = result.lastDocumentSnapshot
                        hasMoreDeliveredOrders = result.hasMore

                        // Update displayed orders
                        updateDisplayedOrders()
                        _isLoadingMore.value = false
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in loadMoreDeliveredOrders: ${e.message}")
                _errorMessage.value = "Failed to load more orders: ${e.message}"
                _isLoadingMore.value = false
            }
        }
    }

    // Load more cancelled orders (pagination)
    fun loadMoreCancelledOrders() {
        if (!hasMoreCancelledOrders || _isLoadingMore.value) return

        _isLoadingMore.value = true
        viewModelScope.launch {
            try {
                orderRepository.getCompletedOrdersPaginatedFlow("Cancelled", 20, lastCancelledDocumentSnapshot)
                    .catch { e ->
                        Log.e(TAG, "Error loading more cancelled orders: ${e.message}")
                        _errorMessage.value = "Failed to load more orders: ${e.message}"
                        _isLoadingMore.value = false
                    }
                    .collectLatest { result ->
                        // Append new orders to existing list
                        _cancelledOrders.value = _cancelledOrders.value + result.items

                        // Update pagination state
                        lastCancelledDocumentSnapshot = result.lastDocumentSnapshot
                        hasMoreCancelledOrders = result.hasMore

                        // Update displayed orders
                        updateDisplayedOrders()
                        _isLoadingMore.value = false
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in loadMoreCancelledOrders: ${e.message}")
                _errorMessage.value = "Failed to load more orders: ${e.message}"
                _isLoadingMore.value = false
            }
        }
    }

    // Update the displayed orders based on the current tab
    fun updateDisplayedOrders() {
        _allOrders.value = when (_currentTab.value) {
            0 -> _activeOrders.value + _deliveredOrders.value + _cancelledOrders.value
            1 -> _activeOrders.value.filter { it.status == "Pending" }
            2 -> _activeOrders.value.filter { it.status == "Processing" }
            3 -> _deliveredOrders.value
            4 -> _cancelledOrders.value
            else -> emptyList()
        }
    }



    // Fetch orders by status (mainly for pending/processing tabs)
    fun fetchOrdersByStatus(status: String) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                // For active statuses, use the filtered active orders if available
                if ((status == "Pending" || status == "Processing") && _activeOrders.value.isNotEmpty()) {
                    _allOrders.value = _activeOrders.value.filter { it.status == status }
                    _isLoading.value = false
                } else if (status == "Delivered") {
                    fetchRecentDeliveredOrders()
                } else if (status == "Cancelled") {
                    fetchRecentCancelledOrders()
                } else {
                    // For other statuses, use the existing method
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
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in fetchOrdersByStatus: ${e.message}")
                _errorMessage.value = "Failed to load orders: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Filter by text search
    fun filterOrdersByText(query: String) {
        if (query.isBlank()) {
            updateDisplayedOrders()
            return
        }

        val lowerCaseQuery = query.lowercase()

        // Apply text filter to the appropriate list based on current tab
        _allOrders.value = when (_currentTab.value) {
            0 -> (_activeOrders.value + _deliveredOrders.value + _cancelledOrders.value)
            1 -> _activeOrders.value.filter { it.status == "Pending" }
            2 -> _activeOrders.value.filter { it.status == "Processing" }
            3 -> _deliveredOrders.value
            4 -> _cancelledOrders.value
            else -> emptyList()
        }.filter { order ->
            order.orderId.contains(lowerCaseQuery) ||
                    order.customerName?.lowercase()?.contains(lowerCaseQuery) == true ||
                    order.status.lowercase().contains(lowerCaseQuery) ||
                    order.paymentMethod?.lowercase()?.contains(lowerCaseQuery) == true ||
                    order.deliveryAddress?.formattedAddress?.lowercase()?.contains(lowerCaseQuery) == true ||
                    order.items.any { item ->
                        item.productName.lowercase().contains(lowerCaseQuery)
                    }
        }
    }

    // Fetch delivery personnel
    fun fetchDeliveryPersonnel() {
        viewModelScope.launch {
            try {
                userRepository.getDeliveryPersonnelFlow()
                    .catch { e ->
                        Log.e(TAG, "Error fetching delivery personnel: ${e.message}")
                        _errorMessage.value = "Failed to load delivery personnel: ${e.message}"
                    }
                    .collectLatest { personnel ->
                        _deliveryPersonnel.value = personnel
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in fetchDeliveryPersonnel: ${e.message}")
                _errorMessage.value = "Failed to load delivery personnel: ${e.message}"
            }
        }
    }

    // Select a delivery person
    fun selectDeliveryPerson(person: UserData?) {
        _selectedDeliveryPerson.value = person
    }

    // Update order to processing and assign delivery person
    fun updateOrderToProcessing(orderId: String, deliveryPersonId: String, deliveryPersonName: String) {
        viewModelScope.launch {
            try {
                val success = orderRepository.updateOrderWithDeliveryPerson(
                    orderId, "Processing", deliveryPersonId, deliveryPersonName
                )

                if (success) {
                    Log.d(TAG, "Order $orderId status updated to Processing with delivery person $deliveryPersonId")
                    // Reset selected delivery person
                    _selectedDeliveryPerson.value = null
                    // Refresh orders to show updated status
                    fetchActiveOrders()
                } else {
                    Log.e(TAG, "Failed to update order $orderId status")
                    _errorMessage.value = "Failed to update order status"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating order status: ${e.message}")
                _errorMessage.value = "Error updating order: ${e.message}"
            }
        }
    }

    // Get order details
    suspend fun getOrderDetails(orderId: String): Order? {
        return try {
            val orderDetails = orderRepository.getOrderDetails(orderId)

            // If order exists and has a userId, fetch the customer name
            if (orderDetails != null && orderDetails.userId.isNotEmpty()) {
                try {
                    val userData = userRepository.getUserData(orderDetails.userId)
                    // Add customer name to the order object
                    val orderWithCustomerName = orderDetails.copy(customerName = userData.name)
                    return orderWithCustomerName
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching customer data: ${e.message}")
                    // Return the order without customer name if fetching user data fails
                    return orderDetails
                }
            }

            orderDetails
        } catch (e: Exception) {
            _errorMessage.value = "Error fetching order details: ${e.message}"
            null
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

                    // Update local cache based on status of the updated order
                    when (order.status) {
                        "Pending", "Processing" -> fetchActiveOrders()
                        "Delivered" -> {
                            if (originalOrder.status != "Delivered") {
                                // If status changed to Delivered, refresh delivered orders
                                fetchRecentDeliveredOrders()
                                // Also remove it from active orders if it was previously active
                                if (originalOrder.status == "Pending" || originalOrder.status == "Processing") {
                                    _activeOrders.value = _activeOrders.value.filter { it.orderId != order.orderId }
                                }
                            }
                        }
                        "Cancelled" -> {
                            if (originalOrder.status != "Cancelled") {
                                // If status changed to Cancelled, refresh cancelled orders
                                fetchRecentCancelledOrders()
                                // Also remove it from active orders if it was previously active
                                if (originalOrder.status == "Pending" || originalOrder.status == "Processing") {
                                    _activeOrders.value = _activeOrders.value.filter { it.orderId != order.orderId }
                                }
                            }
                        }
                    }
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


    // Pagination state class
    data class PaginationState(
        val lastDocumentSnapshot: DocumentSnapshot? = null,
        val hasMoreItems: Boolean = true,
        val isLoading: Boolean = false
    )


    // Check if there are more items to load for the current tab
    fun hasMoreItems(): Boolean {
        return paginationStateMap[currentTab.value]?.hasMoreItems ?: true
    }

    // Load more orders (pagination)
    fun loadMoreOrders() {
        if (_isLoadingMore.value) return

        _isLoadingMore.value = true
        Log.d(TAG, "Loading more orders for tab: ${_currentTab.value}")

        // Get current pagination state
        val currentState = paginationStateMap[_currentTab.value] ?: PaginationState()

        // Don't load if we're already at the end
        if (!currentState.hasMoreItems) {
            _isLoadingMore.value = false
            return
        }

        // Determine status based on tab
        val status = when (_currentTab.value) {
            1 -> "Pending"
            2 -> "Processing"
            3 -> "Delivered"
            4 -> "Cancelled"
            else -> null
        }

        viewModelScope.launch {
            try {
                orderRepository.getOrdersPaginated(
                    status = status,
                    limit = 20,
                    lastDocumentSnapshot = currentState.lastDocumentSnapshot
                ).collectLatest { result ->
                    // Update pagination state
                    paginationStateMap[_currentTab.value] = currentState.copy(
                        lastDocumentSnapshot = result.lastDocumentSnapshot,
                        hasMoreItems = result.hasMore
                    )

                    // Append new orders to existing list
                    _allOrders.value = _allOrders.value + result.items

                    // Reset loading state
                    _isLoadingMore.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading more orders: ${e.message}")
                _errorMessage.value = "Failed to load more orders: ${e.message}"
                _isLoadingMore.value = false
            }
        }
    }

    // Load initial orders for a tab
    fun fetchOrdersForCurrentTab() {
        _isLoading.value = true
        _errorMessage.value = null
        _allOrders.value = emptyList()

        // Reset pagination state for this tab
        paginationStateMap[_currentTab.value] = PaginationState()

        // Determine status based on tab
        val status = when (_currentTab.value) {
            1 -> "Pending"
            2 -> "Processing"
            3 -> "Delivered"
            4 -> "Cancelled"
            else -> null
        }

        viewModelScope.launch {
            try {
                orderRepository.getOrdersPaginated(
                    status = status,
                    limit = 20
                ).collectLatest { result ->
                    // Update pagination state
                    paginationStateMap[_currentTab.value] = PaginationState(
                        lastDocumentSnapshot = result.lastDocumentSnapshot,
                        hasMoreItems = result.hasMore
                    )

                    // Set orders list
                    _allOrders.value = result.items

                    // Reset loading state
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading orders: ${e.message}")
                _errorMessage.value = "Failed to load orders: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Factory for creating this ViewModel with dependencies
    class Factory(
        private val orderRepository: OrderRepository,
        private val userRepository: UserRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ManageOrdersViewModel::class.java)) {
                return ManageOrdersViewModel(orderRepository, userRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}