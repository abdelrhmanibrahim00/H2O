package com.h2o.store.Models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.h2o.store.data.Cart.CartItem
import com.h2o.store.data.models.AddressData
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

    // User address state
    private val _userAddress = MutableStateFlow<AddressData?>(null)
    val userAddress: StateFlow<AddressData?> = _userAddress

    // Address loading state
    private val _isAddressLoading = MutableStateFlow(true)
    val isAddressLoading: StateFlow<Boolean> = _isAddressLoading

    init {
        // Fetch user address when ViewModel is created
        fetchUserAddress()
    }

    private fun fetchUserAddress() {
        viewModelScope.launch {
            _isAddressLoading.value = true
            try {
                val address = userRepository.getUserAddress(userId)
                _userAddress.value = address
            } catch (e: Exception) {
                _error.value = "Failed to load address: ${e.message}"
            } finally {
                _isAddressLoading.value = false
            }
        }
    }

    fun placeOrder(
        cartItems: List<CartItem>,
        totalAmount: Double,
        paymentMethod: String
    ) {
        if (cartItems.isEmpty()) return

        val address = _userAddress.value
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
                    address = address
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