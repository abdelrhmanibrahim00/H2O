package com.h2o.store.ViewModels.User

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.h2o.store.Graph.Graph
import com.h2o.store.data.Cart.CartItem
import com.h2o.store.data.Cart.CartRepository
import com.h2o.store.data.models.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CartViewModel(
    private val cartRepository: CartRepository = Graph.cartRepository,
    private val userId: String = ""  // Default empty string for backward compatibility
) : ViewModel() {

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _totalPrice = MutableStateFlow(0)
    val totalPrice: StateFlow<Int> = _totalPrice.asStateFlow()

    init {
        viewModelScope.launch {
            // Use user-specific cart items if userId is provided, otherwise use all cart items
            val cartFlow = if (userId.isNotEmpty()) {
                cartRepository.getUserCartItems(userId)
            } else {
                cartRepository.getAllCartItems()
            }

            cartFlow.collect { items ->
                _cartItems.value = items
                calculateTotal()
            }
        }
    }

    private fun calculateTotal() {
        _totalPrice.value = _cartItems.value.sumOf {
            if (it.priceAfterDiscount > 0) it.priceAfterDiscount * it.quantity
            else it.productPrice * it.quantity
        }
    }

    fun addToCart(product: Product) {
        viewModelScope.launch {
            val existingItemFlow = if (userId.isNotEmpty()) {
                cartRepository.getUserCartItemById(product.id, userId)
            } else {
                cartRepository.getCartItemById(product.id)
            }

            val existingItem = existingItemFlow.first()

            if (existingItem != null) {
                updateQuantity(existingItem, existingItem.quantity + 1)
            } else {
                val cartItem = CartItem(
                    productId = product.id,
                    quantity = 1,
                    productName = product.name,
                    productPrice = product.price.toInt(),
                    priceAfterDiscount = 9,
                    productImage = product.Image,
                    userId = userId  // Set the user ID for new cart items
                )
                cartRepository.addToCart(cartItem)
            }
        }
    }

    fun updateQuantity(cartItem: CartItem, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeFromCart(cartItem)
        } else {
            viewModelScope.launch {
                cartRepository.updateCartItem(cartItem.copy(quantity = newQuantity))
            }
        }
    }

    fun removeFromCart(cartItem: CartItem) {
        viewModelScope.launch {
            cartRepository.removeFromCart(cartItem)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            if (userId.isNotEmpty()) {
                cartRepository.clearUserCart(userId)
            } else {
                cartRepository.clearCart()
            }
        }
    }

    // Factory for creating CartViewModel with a specific user ID
    class Factory(private val userId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CartViewModel::class.java)) {
                return CartViewModel(userId = userId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}