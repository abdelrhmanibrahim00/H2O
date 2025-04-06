package com.h2o.store.data.Orders

import com.google.firebase.firestore.IgnoreExtraProperties
import com.h2o.store.data.models.AddressData
import java.util.Date


data class OrderItem(
    val productId: String = "",
    val productName: String = "",
    val productDescription: String = "",
    val productImage: String = "",
    val quantity: Int = 0,
    val price: Double = 0.0,
    val originalPrice: Double = 0.0,
    val discountPercentage: Double = 0.0,
    val category: String = "",
    val stock: Int = 0,
    val brand: String = "",
    val onSale: Boolean = false,
    val featured: Boolean = false,
    val rating: Double = 0.0
)

@IgnoreExtraProperties
data class Order(
    val orderId: String = "",
    val userId: String = "",
    val items: List<OrderItem> = emptyList(),
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val total: Double = 0.0,
    val status: String = "Pending",
    val paymentMethod: String = "",
    val orderDate: Date = Date(),
    val estimatedDelivery: Date = Date(),
    val deliveryAddress: AddressData? = null,
    val deliveryPersonId: String = "",
    val customerName: String = "",
    val deliveryPersonName: String = "" // Make sure this exists

)
