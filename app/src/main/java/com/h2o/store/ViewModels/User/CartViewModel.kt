package com.h2o.store.ViewModels.User

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.h2o.store.Graph.Graph
import com.h2o.store.data.Cart.CartItem
import com.h2o.store.data.Cart.CartRepository
import com.h2o.store.data.models.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * CartViewModel that handles cart operations
 */
class CartViewModel(
    private val cartRepository: CartRepository,
    private val userId: String = ""  // Default empty string for backward compatibility
) : ViewModel() {

    // UI States (sealed class for cart states)
    sealed class CartState {
        object Loading : CartState()
        data class Success(val items: List<CartItem>) : CartState()
        data class Error(val message: String) : CartState()
    }

    // Main cart state managed by ViewModel
    val cartState: StateFlow<CartState> = cartRepository
        .getUserCartItems(userId)
        .map { items -> CartState.Success(items) as CartState }
        .catch { e -> emit(CartState.Error(e.message ?: "Error fetching cart items")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CartState.Loading
        )

    // Direct flow of cart items for simpler usage in Composables
    val cartItems = cartState.map { state ->
        when (state) {
            is CartState.Success -> state.items
            else -> emptyList()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Flow for total price calculation
    val totalPrice = cartItems.map { items ->
        items.sumOf { item ->
            val priceAfterDiscount = if (item.discountPercentage > 0) {
                item.price * (1 - item.discountPercentage / 100)
            } else {
                item.price
            }
            // Ensure calculation result is Int if StateFlow type is Int
            (priceAfterDiscount * item.quantity).toInt()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    // Events (kept for potential future use if needed)
    sealed class CartEvent {
        data class AddToCart(val product: Product) : CartEvent()
        data class UpdateQuantity(val cartItem: CartItem, val newQuantity: Int) : CartEvent()
        data class RemoveFromCart(val cartItem: CartItem) : CartEvent()
        object ClearCart : CartEvent()
    }

    // Handle events (kept for structure)
    fun handleEvent(event: CartEvent) {
        when (event) {
            is CartEvent.AddToCart -> addToCart(event.product)
            is CartEvent.UpdateQuantity -> updateQuantity(event.cartItem, event.newQuantity)
            is CartEvent.RemoveFromCart -> removeFromCart(event.cartItem)
            is CartEvent.ClearCart -> clearCart()
        }
    }

    /**
     * Add a product to the cart
     */
    fun addToCart(product: Product) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val existingItemFlow = if (userId.isNotEmpty()) {
                        cartRepository.getUserCartItemById(product.id, userId)
                    } else {
                        cartRepository.getCartItemById(product.id)
                    }

                    val existingItem = existingItemFlow.first()

                    if (existingItem != null) {
                        updateQuantityInternal(existingItem, existingItem.quantity + 1)
                    } else {
                        val cartItem = CartItem(
                            productId = product.id,
                            userId = userId,
                            name = product.name,
                            description = product.description,
                            price = product.price,
                            discountPercentage = product.discountPercentage,
                            imageUrl = product.imageUrl,
                            category = product.category,
                            stock = product.stock,
                            brand = product.brand,
                            onSale = product.onSale,
                            featured = product.featured,
                            rating = product.rating,
                            quantity = 1
                        )
                        cartRepository.addToCart(cartItem)
                    }
                }
            } catch (e: Exception) {
                // Error will be automatically handled by flow catch operator
            }
        }
    }

    /**
     * Internal update quantity function to be called from IO context
     */
    private suspend fun updateQuantityInternal(cartItem: CartItem, newQuantity: Int) {
        if (newQuantity <= 0) {
            cartRepository.removeFromCart(cartItem)
        } else if (newQuantity > cartItem.stock) {
            // Update with max stock
            cartRepository.updateCartItem(cartItem.copy(quantity = cartItem.stock))
        } else {
            // Update with new quantity
            cartRepository.updateCartItem(cartItem.copy(quantity = newQuantity))
        }
    }

    /**
     * Update the quantity of a cart item (Public function)
     */
    fun updateQuantity(cartItem: CartItem, newQuantity: Int) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    updateQuantityInternal(cartItem, newQuantity)
                }
            } catch (e: Exception) {
                // Error will be automatically handled by flow catch operator
            }
        }
    }

    /**
     * Remove an item from the cart
     */
    fun removeFromCart(cartItem: CartItem) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    cartRepository.removeFromCart(cartItem)
                }
            } catch (e: Exception) {
                // Error will be automatically handled by flow catch operator
            }
        }
    }

    /**
     * Clear all items from the cart
     */
    fun clearCart() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (userId.isNotEmpty()) {
                        cartRepository.clearUserCart(userId)
                    } else {
                        cartRepository.clearCart()
                    }
                }
            } catch (e: Exception) {
                // Error will be automatically handled by flow catch operator
            }
        }
    }

    /**
     * Get item count for badge display
     */
    val cartItemCount = cartItems.map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Factory for creating CartViewModel with a specific user ID
    class Factory(
        private val userId: String,
        private val cartRepository: CartRepository = Graph.cartRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CartViewModel::class.java)) {
                return CartViewModel(cartRepository, userId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}