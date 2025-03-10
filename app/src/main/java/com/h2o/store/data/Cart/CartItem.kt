package com.h2o.store.data.Cart

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val productId: String,  // Reference to your Product
    val quantity: Int,
    val productName: String,
    val productPrice: Int,
    @ColumnInfo(name = "price_after_discount")
    val priceAfterDiscount: Int,
    val productImage: String,
    @ColumnInfo(name = "user_id", defaultValue = "")  // Default value for existing records
    val userId: String = ""  // Add user ID field with default value
)