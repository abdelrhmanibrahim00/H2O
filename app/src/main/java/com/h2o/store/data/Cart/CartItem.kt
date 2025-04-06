package com.h2o.store.data.Cart

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cart_items",
    indices = [Index(value = ["productId", "userId"], unique = true)]
)
data class CartItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val productId: String,
    val userId: String = "",
    // Basic product info
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val discountPercentage: Double = 0.0,
    val imageUrl: String = "",
    val category: String = "",
    val stock: Int = 0,
    val brand: String = "",
    val onSale: Boolean = false,
    val featured: Boolean = false,
    val rating: Double = 0.0,
    val quantity: Int = 0,
)