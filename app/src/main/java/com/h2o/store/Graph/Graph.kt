package com.h2o.store.Graph

import android.content.Context
import androidx.room.Room
import com.h2o.store.data.Cart.CartDatabase
import com.h2o.store.data.Cart.CartRepository
import com.h2o.store.repositories.Admin.OrderRepository
import com.h2o.store.repositories.ProductRepository
import com.h2o.store.repositories.UserRepository

object Graph {
    private lateinit var database: CartDatabase

    val cartRepository by lazy {
        CartRepository(cartDao = database.cartDao())
    }

    val productRepository by lazy {
        ProductRepository()  // Initialize with Firestore
    }

    // Add order repository with lazy initialization
    val orderRepository by lazy {
        OrderRepository()  // Initialize with Firestore
    }

    // Add user repository with lazy initialization
    val userRepository by lazy {
        UserRepository()  // Initialize with Firestore
    }

    fun provide(context: Context) {
        database = Room.databaseBuilder(
            context,
            CartDatabase::class.java,
            "cart.db"
        )
            .addMigrations(CartDatabase.MIGRATION_2_3) // Use plural "addMigrations"
            .build()
    }
}