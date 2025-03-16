package com.h2o.store.repositories

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.h2o.store.data.models.Product
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ProductRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val productsCollection = firestore.collection("products")
    private val TAG = "ProductRepository"

    // Get all products (regular method)
    suspend fun getProducts(): List<Product> {
        return try {
            val snapshot = productsCollection.get().await()
            snapshot.documents.mapNotNull { document ->
                document.toObject(Product::class.java)?.copy(id = document.id)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    // Get product by ID
    suspend fun getProductById(productId: String): Product? {
        return try {
            val document = productsCollection.document(productId).get().await()
            document.toObject(Product::class.java)?.copy(id = document.id)
        } catch (e: Exception) {
            throw e
        }
    }

    // Get all products as a Flow (for admin purposes)
    fun getAllProductsFlow(): Flow<List<Product>> = callbackFlow {
        Log.d(TAG, "Starting real-time listener for ALL products")

        // Query all products and sort by name
        val query = productsCollection
            .orderBy("name", Query.Direction.ASCENDING)

        // Set up the snapshot listener
        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening for product updates: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    val products = snapshot.documents.mapNotNull { document ->
                        try {
                            document.toObject(Product::class.java)?.copy(id = document.id)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error converting document ${document.id}: ${e.message}")
                            null
                        }
                    }

                    Log.d(TAG, "Real-time update: ${products.size} total products")
                    trySend(products)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing snapshot: ${e.message}")
                }
            }
        }

        // Clean up the listener when the flow is cancelled
        awaitClose {
            Log.d(TAG, "Removing all products listener")
            listenerRegistration.remove()
        }
    }

    // Update product information
    suspend fun updateProduct(product: Product): Boolean {
        return try {
            Log.d(TAG, "Updating product: ${product.id}")

            // Create a map of fields to update
            val updates = mapOf(
                "name" to product.name,
                "description" to product.description,
                "price" to product.price,
//                "discountPercentage" to product.discountPercentage,
//                "imageUrl" to product.imageUrl,
//                "category" to product.category,
//                "stock" to product.stock,
//                "brand" to product.brand,
//                "onSale" to product.onSale,
//                "featured" to product.featured,
//                "rating" to product.rating
            )

            // Perform the update
            productsCollection.document(product.id)
                .update(updates)
                .await()

            // Log success
            Log.d(TAG, "Product successfully updated: ${product.id}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating product: ${e.message}")
            Log.e(TAG, "Stack trace: ", e)
            false
        }
    }

    // Add a new product
    suspend fun addProduct(product: Product): String? {
        return try {
            Log.d(TAG, "Adding new product")

            // Convert product to map
            val productMap = mapOf(
                "name" to product.name,
                "description" to product.description,
                "price" to product.price,
//                "discountPercentage" to product.discountPercentage,
//                "imageUrl" to product.imageUrl,
//                "category" to product.category,
//                "stock" to product.stock,
//                "brand" to product.brand,
//                "onSale" to product.onSale,
//                "featured" to product.featured,
//                "rating" to product.rating
            )

            // Add the product and get the new document ID
            val documentRef = productsCollection.add(productMap).await()
            val newProductId = documentRef.id

            Log.d(TAG, "New product added with ID: $newProductId")
            newProductId
        } catch (e: Exception) {
            Log.e(TAG, "Error adding product: ${e.message}")
            null
        }
    }

    // Delete a product
    suspend fun deleteProduct(productId: String): Boolean {
        return try {
            Log.d(TAG, "Deleting product: $productId")
            productsCollection.document(productId).delete().await()
            Log.d(TAG, "Product successfully deleted: $productId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting product: ${e.message}")
            false
        }
    }
}