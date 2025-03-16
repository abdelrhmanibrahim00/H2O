package com.h2o.store.data.Cart

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CartItem::class], version = 3, exportSchema = false)
abstract class CartDatabase : RoomDatabase() {
    abstract fun cartDao(): CartDao

    companion object {
        // Migration from version 2 to 3 (int to double)
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create a new table with the updated schema
                database.execSQL(
                    "CREATE TABLE cart_items_new (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "productId TEXT NOT NULL, " +
                            "quantity INTEGER NOT NULL, " +
                            "productName TEXT NOT NULL, " +
                            "productPrice REAL NOT NULL, " + // Changed from INTEGER to REAL
                            "price_after_discount REAL NOT NULL, " + // Changed from INTEGER to REAL
                            "productImage TEXT NOT NULL, " +
                            "user_id TEXT NOT NULL DEFAULT '')"
                )

                // Delete all the old data - as requested
                database.execSQL("DELETE FROM cart_items")

                // Drop the old table
                database.execSQL("DROP TABLE cart_items")

                // Rename the new table to match the old one
                database.execSQL("ALTER TABLE cart_items_new RENAME TO cart_items")
            }
        }
    }
}