package com.h2o.store.Graph

import android.content.Context
import androidx.room.Room
import com.h2o.store.data.Cart.CartDatabase
import com.h2o.store.data.Cart.CartRepository
import com.h2o.store.repositories.ProductRepository

object Graph {
    private lateinit var database: CartDatabase

    val cartRepository by lazy {
        CartRepository(cartDao = database.cartDao())
    }

    val productRepository by lazy {
        ProductRepository()  // Initialize with Firestore
    }

    fun provide(context: Context) {
        database = Room.databaseBuilder(
            context,
            CartDatabase::class.java,
            "cart.db"
        )
            .fallbackToDestructiveMigration()  // This will delete and recreate the database on version change
            .build()
    }
}