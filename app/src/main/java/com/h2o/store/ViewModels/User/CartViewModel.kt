package com.h2o.store.ViewModels.User

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.h2o.store.Graph.Graph
import com.h2o.store.data.Cart.CartItem
import com.h2o.store.data.Cart.CartRepository
import com.h2o.store.data.models.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext // Import withContext

/**
 * CartViewModel that handles cart operations
 */
class CartViewModel(
    private val cartRepository: CartRepository,
    private val userId: String = ""  // Default empty string for backward compatibility
) : ViewModel() {

    // UI State for cart items - Using StateFlow for simpler state management
    // If complex state logic arises, consider distinct Loading/Success/Error states
    private val _cartState = MutableStateFlow<CartState>(CartState.Loading) // Keep for potential future error handling
    val cartState: StateFlow<CartState> = _cartState.asStateFlow() // Keep for potential future error handling

    // Direct flow of cart items for simpler usage in Composables
    val cartItems = cartRepository.getUserCartItems(userId)
        .catch { e ->
            // Update internal state on error, emit empty list for UI stability
            _cartState.value = CartState.Error(e.message ?: "Error fetching cart items")
            emit(emptyList())
        }
        .stateIn(
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

    // UI States (kept for potential future use)
    sealed class CartState {
        object Loading : CartState()
        data class Success(val items: List<CartItem>) : CartState() // Might not be directly used if observing cartItems flow
        data class Error(val message: String) : CartState()
    }

    init {
        // Initial load trigger if needed, but cartItems flow handles it reactively
        // If you need explicit loading state management beyond the flow's initial value:
        viewModelScope.launch {
            _cartState.value = CartState.Loading
            // Force initial collection or handle loading state based on cartItems emission
            cartItems.collect { items ->
                if (_cartState.value is CartState.Loading) { // Only transition from Loading once
                    _cartState.value = CartState.Success(items)
                }
            }
        }
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
     * *** UPDATED METHOD ***
     */
    fun addToCart(product: Product) {
        viewModelScope.launch { // Launches on Main potentially, switch context for IO
            try {
                // Perform repository operations on IO thread
                withContext(Dispatchers.IO) {
                    val existingItemFlow = if (userId.isNotEmpty()) {
                        cartRepository.getUserCartItemById(product.id, userId)
                    } else {
                        cartRepository.getCartItemById(product.id)
                    }

                    // Collect the first item on IO thread
                    val existingItem = existingItemFlow.first()

                    if (existingItem != null) {
                        // If item exists, update its quantity (will also switch context internally)
                        // No need for extra withContext if updateQuantity handles it
                        updateQuantityInternal(existingItem, existingItem.quantity + 1) // Call internal version
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
                        cartRepository.addToCart(cartItem) // Assume repo handles its own IO thread
                    }
                }
            } catch (e: Exception) {
                // Update error state on the main thread
                withContext(Dispatchers.Main){
                    _cartState.value = CartState.Error("Failed to add item: ${e.message}")
                }
            }
        }
    }

    /**
     * Internal update quantity function to be called from IO context
     */
    private suspend fun updateQuantityInternal(cartItem: CartItem, newQuantity: Int) {
        // Assumes this is already called within withContext(Dispatchers.IO)
        if (newQuantity <= 0) {
            cartRepository.removeFromCart(cartItem) // Assume repo handles its own IO thread
        } else if (newQuantity > cartItem.stock) {
            // Update with max stock
            cartRepository.updateCartItem(cartItem.copy(quantity = cartItem.stock)) // Assume repo handles its own IO thread
        } else {
            // Update with new quantity
            cartRepository.updateCartItem(cartItem.copy(quantity = newQuantity)) // Assume repo handles its own IO thread
        }
    }


    /**
     * Update the quantity of a cart item (Public function)
     * *** UPDATED METHOD ***
     */
    fun updateQuantity(cartItem: CartItem, newQuantity: Int) {
        viewModelScope.launch { // Launches on Main potentially, switch context for IO
            try {
                withContext(Dispatchers.IO) {
                    updateQuantityInternal(cartItem, newQuantity)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main){
                    _cartState.value = CartState.Error("Failed to update quantity: ${e.message}")
                }
            }
        }
    }

    /**
     * Remove an item from the cart
     * *** UPDATED METHOD ***
     */
    fun removeFromCart(cartItem: CartItem) {
        viewModelScope.launch { // Launches on Main potentially, switch context for IO
            try {
                withContext(Dispatchers.IO) {
                    cartRepository.removeFromCart(cartItem) // Assume repo handles its own IO thread
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main){
                    _cartState.value = CartState.Error("Failed to remove item: ${e.message}")
                }
            }
        }
    }

    /**
     * Clear all items from the cart
     * *** UPDATED METHOD ***
     */
    fun clearCart() {
        viewModelScope.launch { // Launches on Main potentially, switch context for IO
            try {
                withContext(Dispatchers.IO) {
                    if (userId.isNotEmpty()) {
                        cartRepository.clearUserCart(userId) // Assume repo handles its own IO thread
                    } else {
                        cartRepository.clearCart() // Assume repo handles its own IO thread
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main){
                    _cartState.value = CartState.Error("Failed to clear cart: ${e.message}")
                }
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