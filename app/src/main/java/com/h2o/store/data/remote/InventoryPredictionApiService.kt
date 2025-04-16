package com.h2o.store.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface for communicating with the inventory prediction API
 */
interface InventoryPredictionApiService {

    @POST("predict")
    suspend fun predictSingleProduct(@Body productData: Map<String, Any>): Response<PredictionResponse>

    @POST("bulk_recommendations")
    suspend fun generateBulkRecommendations(@Body productsData: Map<String, Any>): Response<BulkPredictionResponse>
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