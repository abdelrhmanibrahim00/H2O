package com.h2o.store.repositories

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.h2o.store.data.remote.BulkPredictionRequest
import com.h2o.store.data.remote.BulkPredictionResponse
import com.h2o.store.data.remote.OrderData
import com.h2o.store.data.remote.OrderItemData
import com.h2o.store.data.remote.PredictionResponse
import com.h2o.store.data.remote.PredictionRetrofitClient
import com.h2o.store.data.remote.ProductData
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

                // Use "products" collection instead of "inventory"
                val productsCollection = db.collection("products").get().await()
                val ordersCollection = db.collection("orders").get().await()

                Log.d(TAG, "Fetched ${productsCollection.size()} products and ${ordersCollection.size()} orders")

                // If no products, return error
                if (productsCollection.isEmpty) {
                    Log.e(TAG, "No products found in 'products' collection - please check your Firebase database")
                    return@withContext Result.failure(Exception("No products found in database. Cannot generate predictions."))
                }

                // Map Firebase documents to our data models
                val products = productsCollection.documents.mapNotNull { doc ->
                    try {
                        ProductData(
                            productId = doc.id,
                            name = doc.getString("name") ?: "Unknown",
                            category = doc.getString("category") ?: "Uncategorized",
                            currentStock = doc.getLong("quantity") ?: 0L,
                            price = doc.getDouble("price") ?: 0.0,
                            lastRestocked = doc.getTimestamp("lastRestocked")?.toDate()?.time ?: Date().time
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error mapping product ${doc.id}: ${e.message}")
                        null
                    }
                }

                val orders = ordersCollection.documents.mapNotNull { doc ->
                    try {
                        // Map items from order document
                        val orderItems = (doc.get("items") as? List<Map<String, Any>>)?.mapNotNull { item ->
                            try {
                                OrderItemData(
                                    productId = item["productId"] as? String ?: "",
                                    quantity = (item["quantity"] as? Number)?.toInt() ?: 0,
                                    price = (item["price"] as? Number)?.toDouble() ?: 0.0
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Error mapping order item: ${e.message}")
                                null
                            }
                        } ?: emptyList()

                        OrderData(
                            orderId = doc.id,
                            timestamp = doc.getTimestamp("orderDate")?.toDate()?.time ?: Date().time,
                            items = orderItems
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error mapping order ${doc.id}: ${e.message}")
                        null
                    }
                }

                // Create request object using our data models
                val request = BulkPredictionRequest(
                    products = products,
                    orders = orders
                )

                Log.d(TAG, "Sending data to prediction API")

                // Send to prediction API
                val response = predictionService.generateBulkRecommendations(request)

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

                // Use "products" collection instead of "inventory"
                val productDoc = db.collection("products").document(productId).get().await()

                if (!productDoc.exists()) {
                    return@withContext Result.failure(Exception("Product not found"))
                }

                // Create product data model
                val productData = ProductData(
                    productId = productId,
                    name = productDoc.getString("name") ?: "Unknown",
                    category = productDoc.getString("category") ?: "Uncategorized",
                    currentStock = productDoc.getLong("quantity") ?: 0L,
                    price = productDoc.getDouble("price") ?: 0.0,
                    lastRestocked = productDoc.getTimestamp("lastRestocked")?.toDate()?.time ?: Date().time
                )

                // Send to prediction API
                val response = predictionService.predictSingleProduct(productData)

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
}