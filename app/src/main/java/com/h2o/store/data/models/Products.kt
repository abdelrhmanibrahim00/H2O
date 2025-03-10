package com.h2o.store.data.models

data class Product(
    val id: String = "",  // Firestore document ID
    val Image: String = "",
    val discount: Boolean = false,
    val name: String = "",
    val price: Int = 0,
    val price_after_discount: Int = 0,
    val quantity: Int = 0,
    val stock: Int = 0
)

data class ProductResponse(
    val products: List<Product> = emptyList()
)