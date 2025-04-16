package com.h2o.store.data.Cart

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CartItem::class], version = 3, exportSchema = false)
abstract class CartDatabase : RoomDatabase() {
    abstract fun cartDao(): CartDao

    companion object {
        // Migration from version 2 to 3
        val MIGRATION_2_3 = object : Migration(1, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create a new table with the updated schema - without the extra fields
                database.execSQL(
                    "CREATE TABLE cart_items_new (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "productId TEXT NOT NULL, " +
                            "userId TEXT NOT NULL DEFAULT '', " +
                            "name TEXT NOT NULL DEFAULT '', " +
                            "description TEXT NOT NULL DEFAULT '', " +
                            "price REAL NOT NULL DEFAULT 0, " +
                            "discountPercentage REAL NOT NULL DEFAULT 0, " +
                            "imageUrl TEXT NOT NULL DEFAULT '', " +
                            "category TEXT NOT NULL DEFAULT '', " +
                            "stock INTEGER NOT NULL DEFAULT 0, " +
                            "brand TEXT NOT NULL DEFAULT '', " +
                            "onSale INTEGER NOT NULL DEFAULT 0, " +
                            "featured INTEGER NOT NULL DEFAULT 0, " +
                            "rating REAL NOT NULL DEFAULT 0, " +
                            "quantity INTEGER NOT NULL DEFAULT 0)"
                )
                // Create an index for product ID and user ID
                database.execSQL(
                    "CREATE UNIQUE INDEX index_cart_items_productId_userId ON cart_items_new(productId, userId)"
                )
                // Delete all old data as requested
                database.execSQL("DELETE FROM cart_items")
                // Drop the old table
                database.execSQL("DROP TABLE cart_items")
                // Rename the new table to match the old one
                database.execSQL("ALTER TABLE cart_items_new RENAME TO cart_items")
            }
        }
    }
}