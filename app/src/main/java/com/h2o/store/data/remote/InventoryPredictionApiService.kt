package com.h2o.store.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface for communicating with the inventory prediction API
 */
interface InventoryPredictionApiService {

    @POST("predict")
    suspend fun predictSingleProduct(@Body productData: ProductData): Response<PredictionResponse>

    @POST("bulk_recommendations")
    suspend fun generateBulkRecommendations(@Body request: BulkPredictionRequest): Response<BulkPredictionResponse>
}

/**
 * Response model for prediction results
 */
data class PredictionResponse(
    val productId: String,
    val recommendedStock: Int,
    val confidence: Double,
    val message: String
)

/**
 * Response model for bulk predictions
 */
data class BulkPredictionResponse(
    val predictions: List<PredictionResponse>,
    val generatedAt: String,
    val status: String
)

/**
 * Request model for bulk prediction API
 */
data class BulkPredictionRequest(
    val products: List<ProductData>,
    val orders: List<OrderData>
)

/**
 * Product data model for prediction
 */
data class ProductData(
    val productId: String,
    val name: String,
    val category: String,
    val currentStock: Long,
    val price: Double,
    val lastRestocked: Long
)

/**
 * Order data model for prediction
 */
data class OrderData(
    val orderId: String,
    val timestamp: Long,
    val items: List<OrderItemData>
)

/**
 * Order item data model
 */
data class OrderItemData(
    val productId: String,
    val quantity: Int,
    val price: Double
)