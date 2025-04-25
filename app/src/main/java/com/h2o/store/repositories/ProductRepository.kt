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

    // Simple in-memory cache to improve performance
    private var productsCache: List<Product> = emptyList()
    private var lastCacheTime: Long = 0

    // Get all products (regular method)
    suspend fun getProducts(): List<Product> {
        return try {
            // Use cache if it's recent (less than 5 minutes old)
            val cacheAge = System.currentTimeMillis() - lastCacheTime
            if (productsCache.isNotEmpty() && cacheAge < 5 * 60 * 1000) {
                Log.d(TAG, "Using cached products (${productsCache.size} items)")
                return productsCache
            }

            val snapshot = productsCollection.get().await()
            val products = snapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(Product::class.java)?.copy(id = document.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting document ${document.id}: ${e.message}")
                    null
                }
            }

            // Update cache
            productsCache = products
            lastCacheTime = System.currentTimeMillis()
            Log.d(TAG, "Fetched ${products.size} products from Firestore")

            products
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching products: ${e.message}")
            // Return cache if available, otherwise throw
            if (productsCache.isNotEmpty()) {
                Log.d(TAG, "Returning cached products after error")
                productsCache
            } else {
                throw e
            }
        }
    }

    // Get product by ID
    suspend fun getProductById(productId: String): Product? {
        return try {
            // Check cache first
            productsCache.find { it.id == productId }?.let {
                Log.d(TAG, "Found product $productId in cache")
                return it
            }

            val document = productsCollection.document(productId).get().await()
            document.toObject(Product::class.java)?.copy(id = document.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching product by ID: ${e.message}")
            throw e
        }
    }

    // Get all products as a Flow (for admin purposes)
    fun getAllProductsFlow(): Flow<List<Product>> = callbackFlow {
        Log.d(TAG, "Starting real-time listener for ALL products")

        // Send cached data immediately if available
        if (productsCache.isNotEmpty()) {
            trySend(productsCache)
        }

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

                    // Update cache
                    productsCache = products
                    lastCacheTime = System.currentTimeMillis()

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

    // Get paginated products (to fix the duplicate key issue)
    suspend fun getPaginatedProducts(lastProductId: String? = null, pageSize: Int = 10): List<Product> {
        return try {
            val startTime = System.currentTimeMillis()

            // Build query with pagination
            var query = productsCollection
                .orderBy("name", Query.Direction.ASCENDING)
                .limit(pageSize.toLong())

            // If we have a last document ID, fetch that document and use it as starting point
            if (lastProductId != null) {
                val lastDocSnapshot = productsCollection.document(lastProductId).get().await()
                if (lastDocSnapshot.exists()) {
                    query = query.startAfter(lastDocSnapshot)
                }
            }

            val snapshot = query.get().await()
            val products = snapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(Product::class.java)?.copy(id = document.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting document ${document.id}: ${e.message}")
                    null
                }
            }

            Log.d(TAG, "Paginated products fetch took ${System.currentTimeMillis() - startTime}ms")
            products
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching paginated products: ${e.message}")
            emptyList() // Return empty list on error to prevent crashes
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
                "discountPercentage" to product.discountPercentage,
                "imageUrl" to product.imageUrl,
                "category" to product.category,
                "stock" to product.stock,
                "brand" to product.brand,
                "onSale" to product.onSale,
                "featured" to product.featured,
                "rating" to product.rating,
                "quantity" to product.quantity
            )

            // Perform the update
            productsCollection.document(product.id)
                .update(updates)
                .await()

            // Update in cache if present
            val cacheIndex = productsCache.indexOfFirst { it.id == product.id }
            if (cacheIndex >= 0) {
                val updatedCache = productsCache.toMutableList()
                updatedCache[cacheIndex] = product
                productsCache = updatedCache
            }

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
                "discountPercentage" to product.discountPercentage,
                "imageUrl" to product.imageUrl,
                "category" to product.category,
                "stock" to product.stock,
                "brand" to product.brand,
                "onSale" to product.onSale,
                "featured" to product.featured,
                "rating" to product.rating,
                "quantity" to product.quantity
            )

            // Add the product and get the new document ID
            val documentRef = productsCollection.add(productMap).await()
            val newProductId = documentRef.id

            // Add to cache if we have one
            if (productsCache.isNotEmpty()) {
                val newProduct = product.copy(id = newProductId)
                productsCache = productsCache + newProduct
                lastCacheTime = System.currentTimeMillis()
            }

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

            // Remove from cache if present
            if (productsCache.any { it.id == productId }) {
                productsCache = productsCache.filter { it.id != productId }
                lastCacheTime = System.currentTimeMillis()
            }

            Log.d(TAG, "Product successfully deleted: $productId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting product: ${e.message}")
            false
        }
    }

    // Clear cache
    fun clearCache() {
        productsCache = emptyList()
        lastCacheTime = 0
        Log.d(TAG, "Product cache cleared")
    }
}