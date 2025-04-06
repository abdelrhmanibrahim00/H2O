package com.h2o.store.data.Cart

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToCart(cartItem: CartItem)

    // Keep the original method for backward compatibility
    @Query("SELECT * FROM cart_items")
    fun getAllCartItems(): Flow<List<CartItem>>

    // Original method for backward compatibility
    @Query("SELECT * FROM cart_items WHERE productId = :productId")
    fun getCartItemById(productId: String): Flow<CartItem?>

    @Update
    suspend fun updateCartItem(cartItem: CartItem)

    @Delete
    suspend fun removeFromCart(cartItem: CartItem)

    // Original method for backward compatibility
    @Query("DELETE FROM cart_items")
    suspend fun clearCart()

    // New method to get user-specific cart items
    @Query("SELECT * FROM cart_items WHERE userId = :userId") // Changed from user_id to userId
    fun getUserCartItems(userId: String): Flow<List<CartItem>>

    // New method for user-specific product lookup
    @Query("SELECT * FROM cart_items WHERE productId = :productId AND userId = :userId") // Changed from user_id to userId
    fun getUserCartItemById(productId: String, userId: String): Flow<CartItem?>

    // New method for clearing user-specific cart
    @Query("DELETE FROM cart_items WHERE userId = :userId") // Changed from user_id to userId
    suspend fun clearUserCart(userId: String)
}