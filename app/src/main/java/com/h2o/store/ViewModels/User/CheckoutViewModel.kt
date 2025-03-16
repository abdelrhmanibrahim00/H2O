package com.h2o.store.ViewModels.User

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.h2o.store.data.Cart.CartItem
import com.h2o.store.data.User.UserData
import com.h2o.store.repositories.CheckoutRepository
import com.h2o.store.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CheckoutViewModel(private val userId: String) : ViewModel() {

    private val orderRepository = CheckoutRepository()
    private val userRepository = UserRepository()
    private val cartViewModel = CartViewModel(userId = userId)

    // UI state
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _orderSuccess = MutableStateFlow(false)
    val orderSuccess: StateFlow<Boolean> = _orderSuccess

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // User data state (includes address, name, email, phone, whatsapp)
    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData

    // User data loading state
    private val _isUserDataLoading = MutableStateFlow(true)
    val isUserDataLoading: StateFlow<Boolean> = _isUserDataLoading

    init {
        // Fetch user data when ViewModel is created
        fetchUserData()
    }

    private fun fetchUserData() {
        viewModelScope.launch {
            _isUserDataLoading.value = true
            try {
                val data = userRepository.getUserData(userId)
                _userData.value = data
            } catch (e: Exception) {
                _error.value = "Failed to load user data: ${e.message}"
            } finally {
                _isUserDataLoading.value = false
            }
        }
    }

    fun placeOrder(
        cartItems: List<CartItem>,
        totalAmount: Double,
        paymentMethod: String
    ) {
        if (cartItems.isEmpty()) return

        val userData = _userData.value
        if (userData == null) {
            _error.value = "User data not available"
            return
        }

        val address = userData.address
        if (address == null) {
            _error.value = "No address available for delivery"
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            _error.value = null

            try {
                // Use repository to place order with the user's address
                val success = orderRepository.placeOrder(
                    userId = userId,
                    cartItems = cartItems,
                    subtotal = totalAmount,
                    paymentMethod = paymentMethod,
                    address = address,
                    userName = userData.name,
                    userPhone = userData.phone,
                    userEmail = userData.email
                )

                if (success) {
                    // Clear cart after successful order
                    cartViewModel.clearCart()
                    _orderSuccess.value = true
                } else {
                    _error.value = "Failed to place order"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An unknown error occurred"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    // Factory to create ViewModel with parameters
    class Factory(private val userId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CheckoutViewModel::class.java)) {
                return CheckoutViewModel(userId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}