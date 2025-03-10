package com.h2o.store.data.Cart

class CartRepository(private val cartDao: CartDao) {
    // Original methods for backward compatibility
    fun getAllCartItems() = cartDao.getAllCartItems()

    // New method for user-specific cart items
    fun getUserCartItems(userId: String) = cartDao.getUserCartItems(userId)

    suspend fun addToCart(cartItem: CartItem) = cartDao.addToCart(cartItem)

    // Original method for backward compatibility
    fun getCartItemById(productId: String) = cartDao.getCartItemById(productId)

    // New method for user-specific cart item lookup
    fun getUserCartItemById(productId: String, userId: String) = cartDao.getUserCartItemById(productId, userId)

    suspend fun updateCartItem(cartItem: CartItem) = cartDao.updateCartItem(cartItem)

    suspend fun removeFromCart(cartItem: CartItem) = cartDao.removeFromCart(cartItem)

    // Original method for backward compatibility
    suspend fun clearCart() = cartDao.clearCart()

    // New method for clearing user-specific cart
    suspend fun clearUserCart(userId: String) = cartDao.clearUserCart(userId)
}