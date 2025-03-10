package com.h2o.store.repositories

import com.google.firebase.firestore.FirebaseFirestore
import com.h2o.store.data.models.Product
import kotlinx.coroutines.tasks.await

class ProductRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val productsCollection = firestore.collection("products")

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

    suspend fun getProductById(productId: String): Product? {
        return try {
            val document = productsCollection.document(productId).get().await()
            document.toObject(Product::class.java)?.copy(id = document.id)
        } catch (e: Exception) {
            throw e
        }
    }
}