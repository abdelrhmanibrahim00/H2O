package com.h2o.store.data.Cart

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CartItem::class], version = 2, exportSchema = false)
abstract class CartDatabase : RoomDatabase() {
    abstract fun cartDao(): CartDao
}