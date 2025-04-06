package com.h2o.store.data.models

data class Product(
    val id: String = "",
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

data class ProductResponse(
    val products: List<Product> = emptyList()
)