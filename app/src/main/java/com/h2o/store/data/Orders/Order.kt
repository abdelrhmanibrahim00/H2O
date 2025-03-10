package com.h2o.store.data.Order

import java.util.Date

data class Order(
    val orderId: String = "",
    val userId: String = "",
    val items: List<OrderItem> = emptyList(),
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val total: Double = 0.0,
    val status: String = "",
    val paymentMethod: String = "",
    val orderDate: Date = Date(),
    val estimatedDelivery: Date = Date(),
    val deliveryAddress: DeliveryAddress? = null
)

data class OrderItem(
    val productId: String = "",
    val productName: String = "",
    val productImage: String = "",
    val quantity: Int = 0,
    val price: Double = 0.0
)

data class DeliveryAddress(
    val street: String = "",
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val country: String = "",
    val phoneNumber: String = ""
)