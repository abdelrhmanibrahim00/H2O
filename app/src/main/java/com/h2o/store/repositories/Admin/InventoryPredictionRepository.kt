package com.h2o.store.repositories

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.h2o.store.data.remote.BulkPredictionResponse
import com.h2o.store.data.remote.PredictionRetrofitClient
import com.h2o.store.data.remote.PredictionResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Repository for handling inventory prediction data operations
 */
class InventoryPredictionRepository {

    private val db = FirebaseFirestore.getInstance()
    private val predictionService = PredictionRetrofitClient.inventoryPredictionService
    private val TAG = "InventoryPredictRepo"

    /**
     * Fetch inventory data from Firebase, then send to prediction API
     */
    suspend fun generateInventoryReport(): Result<BulkPredictionResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching data for inventory prediction report")

                // Fetch products data from Firebase
                val productsCollection = db.collection("inventory").get().await()
                val ordersCollection = db.collection("orders").get().await()

                Log.d(TAG, "Fetched ${productsCollection.size()} products and ${ordersCollection.size()} orders")

                // Create data map to send to API
                val requestData = mapOf(
                    "products" to productsCollection.documents.mapNotNull { doc ->
                        try {
                            mapOf(
                                "productId" to doc.id,
                                "name" to (doc.getString("name") ?: "Unknown"),
                                "category" to (doc.getString("category") ?: "Uncategorized"),
                                "currentStock" to (doc.getLong("quantity") ?: 0),
                                "price" to (doc.getDouble("price") ?: 0.0),
                                "lastRestocked" to (doc.getTimestamp("lastRestocked")?.toDate()?.time ?: Date().time)
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error mapping product ${doc.id}: ${e.message}")
                            null
                        }
                    },
                    "orders" to ordersCollection.documents.mapNotNull { doc ->
                        try {
                            val items = doc.get("items") as? List<Map<String, Any>> ?: emptyList<Map<String, Any>>()
                            mapOf(
                                "orderId" to doc.id,
                                "timestamp" to (doc.getTimestamp("orderDate")?.toDate()?.time ?: Date().time),
                                "items" to items
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error mapping order ${doc.id}: ${e.message}")
                            null
                        }
                    }
                )

                Log.d(TAG, "Sending data to prediction API")

                // Send to prediction API
                val response = predictionService.generateBulkRecommendations(requestData)

                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "Successfully received prediction response")
                    Result.success(response.body()!!)
                } else {
                    Log.e(TAG, "API Error: ${response.code()} - ${response.message()}")
                    Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating inventory report: ${e.message}")
                Result.failure(e)
            }
        }
    }

    /**
     * Predict inventory for a single product
     */
    suspend fun predictSingleProduct(productId: String): Result<PredictionResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching data for single product prediction: $productId")

                // Fetch product data
                val productDoc = db.collection("inventory").document(productId).get().await()

                if (!productDoc.exists()) {
                    return@withContext Result.failure(Exception("Product not found"))
                }

                // Fetch recent orders containing this product
                val ordersWithProduct = db.collection("orders")
                    .get()
                    .await()
                    .documents
                    .filter { doc ->
                        val items = doc.get("items") as? List<Map<String, Any>> ?: emptyList<Map<String, Any>>()
                        items.any { item ->
                            item["productId"] == productId
                        }
                    }

                // Create request data
                val requestData = mapOf(
                    "productId" to productId,
                    "name" to (productDoc.getString("name") ?: "Unknown"),
                    "category" to (productDoc.getString("category") ?: "Uncategorized"),
                    "currentStock" to (productDoc.getLong("quantity") ?: 0),
                    "price" to (productDoc.getDouble("price") ?: 0.0),
                    "lastRestocked" to (productDoc.getTimestamp("lastRestocked")?.toDate()?.time ?: Date().time),
                    "orderHistory" to ordersWithProduct.map { doc ->
                        mapOf(
                            "orderId" to doc.id,
                            "timestamp" to (doc.getTimestamp("orderDate")?.toDate()?.time ?: Date().time),
                            "quantity" to getProductQuantityInOrder(doc, productId)
                        )
                    }
                )

                // Send to prediction API
                val response = predictionService.predictSingleProduct(requestData)

                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error predicting single product: ${e.message}")
                Result.failure(e)
            }
        }
    }

    /**
     * Helper function to extract product quantity from an order
     */
    private fun getProductQuantityInOrder(orderDoc: com.google.firebase.firestore.DocumentSnapshot, productId: String): Int {
        try {
            val items = orderDoc.get("items") as? List<Map<String, Any>> ?: return 0

            return items.find { it["productId"] == productId }?.let {
                (it["quantity"] as? Number)?.toInt() ?: 0
            } ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting product quantity: ${e.message}")
            return 0
        }
    }
}