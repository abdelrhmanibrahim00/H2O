package com.h2o.store.data.models


import com.h2o.store.data.Order.OrderItem
import java.util.Date

data class Order(
    val orderId: String = "",
    val userId: String = "",
    val items: List<OrderItem> = listOf(),
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val total: Double = 0.0,
    val status: String = "",
    val paymentMethod: String = "",
    val orderDate: Date = Date(),
    val estimatedDelivery: Date = Date(),
    val deliveryAddress: AddressData = AddressData()
)