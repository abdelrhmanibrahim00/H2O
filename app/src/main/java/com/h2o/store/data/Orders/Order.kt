package com.h2o.store.data.Orders

import com.h2o.store.data.models.AddressData
import java.util.Date


data class OrderItem(
    val productId: String = "",
    val productName: String = "",
    val productImage: String = "",
    val quantity: Int = 0,
    val price: Double = 0.0
)

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
    val deliveryAddress: AddressData? = null
)
