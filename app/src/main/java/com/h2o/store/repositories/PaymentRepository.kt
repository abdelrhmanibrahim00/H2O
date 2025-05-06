package com.h2o.store.repositories

import android.util.Log
import com.h2o.store.data.models.BillingData
import com.h2o.store.data.models.OrderRegistrationRequest
import com.h2o.store.data.models.OrderRegistrationResponse
import com.h2o.store.data.models.PayMobConstants
import com.h2o.store.data.models.PaymentKeyRequest
import com.h2o.store.data.models.PaymentKeyResponse
import com.h2o.store.data.models.PaymentResult
import com.h2o.store.data.models.PaymobAuthRequest
import com.h2o.store.data.models.PaymobAuthResponse
import com.h2o.store.data.models.TransactionStatusResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

class PaymentRepository {
    private val TAG = "PaymentRepository"

    // Use username/password for authentication instead of API key
    private val USERNAME = "YOUR_USERNAME" // Replace with your PayMob username
    private val PASSWORD = "YOUR_PASSWORD" // Replace with your PayMob password

    private val INTEGRATION_ID = PayMobConstants.CARD_INTEGRATION_ID
    private val IFRAME_ID = PayMobConstants.IFRAME_ID

    // Create OkHttpClient with logging
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(PayMobConstants.Endpoints.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val payMobService = retrofit.create(PayMobService::class.java)

    /**
     * Process payment using PayMob with direct API calls
     */
    suspend fun processPayment(
        orderId: String,
        totalAmount: Double,
        billingData: BillingData
    ): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting payment process for order: $orderId")

            // Step 1: Authenticate with username/password instead of API key
            val authResponse = payMobService.authenticateWithCredentials(
                mapOf(
                    "username" to USERNAME,
                    "password" to PASSWORD
                )
            )
            val authToken = authResponse.token
            Log.d(TAG, "Authentication successful with credentials")

            // Step 2: Register Order
            val amountCents = (totalAmount * 100).toInt()
            val orderRegistrationResponse = payMobService.registerOrder(
                OrderRegistrationRequest(
                    auth_token = authToken,
                    amount_cents = amountCents,
                    currency = "EGP",
                    merchant_order_id = orderId,
                    items = listOf()
                )
            )
            val payMobOrderId = orderRegistrationResponse.id
            Log.d(TAG, "Order registered with PayMob ID: $payMobOrderId")

            // Step 3: Generate Payment Key
            val paymentKeyResponse = payMobService.generatePaymentKey(
                PaymentKeyRequest(
                    auth_token = authToken,
                    amount_cents = amountCents,
                    currency = "EGP",
                    order_id = payMobOrderId,
                    billing_data = billingData,
                    integration_id = INTEGRATION_ID,
                    lock_order_when_paid = false
                )
            )
            val paymentToken = paymentKeyResponse.token
            Log.d(TAG, "Payment key generated: $paymentToken")

            // Create iframe URL with correct format
            val iframeUrl = "${PayMobConstants.Endpoints.IFRAME_URL}$IFRAME_ID?payment_token=$paymentToken"
            Log.d(TAG, "Created iframe URL: $iframeUrl")

            return@withContext iframeUrl
        } catch (e: Exception) {
            Log.e(TAG, "Error during payment process: ${e.message}", e)
            throw e
        }
    }

    /**
     * Query transaction status by order ID
     */
    suspend fun checkTransactionStatus(orderId: String): PaymentResult = withContext(Dispatchers.IO) {
        try {
            // First authenticate
            val authResponse = payMobService.authenticateWithCredentials(
                mapOf(
                    "username" to USERNAME,
                    "password" to PASSWORD
                )
            )
            val authToken = authResponse.token

            // Then check transaction status
            val transactionResponse = payMobService.checkTransactionStatusByOrderId(
                mapOf("order_id" to orderId),
                "Bearer $authToken"
            )

            return@withContext PaymentResult(
                success = transactionResponse.success,
                transactionId = transactionResponse.id.toString(),
                orderId = transactionResponse.order.id.toString(),
                amount = transactionResponse.amount_cents / 100.0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking transaction status: ${e.message}", e)
            return@withContext PaymentResult(
                success = false,
                errorMessage = e.message ?: "Unknown error"
            )
        }
    }

    /**
     * Query transaction status by transaction ID
     */
    suspend fun checkTransactionStatusById(transactionId: String): PaymentResult = withContext(Dispatchers.IO) {
        try {
            // First authenticate
            val authResponse = payMobService.authenticateWithCredentials(
                mapOf(
                    "username" to USERNAME,
                    "password" to PASSWORD
                )
            )
            val authToken = authResponse.token

            // Then check transaction status
            val transactionResponse = payMobService.checkTransactionStatusById(
                transactionId,
                "Bearer $authToken"
            )

            return@withContext PaymentResult(
                success = transactionResponse.success,
                transactionId = transactionResponse.id.toString(),
                orderId = transactionResponse.order.id.toString(),
                amount = transactionResponse.amount_cents / 100.0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking transaction status: ${e.message}", e)
            return@withContext PaymentResult(
                success = false,
                errorMessage = e.message ?: "Unknown error"
            )
        }
    }

    /**
     * Convert AddressData to BillingData for PayMob
     */
    fun convertAddressToPayMobBilling(
        userData: com.h2o.store.data.User.UserData
    ): BillingData {
        val address = userData.address ?: throw IllegalArgumentException("User address is required for billing")

        return BillingData(
            email = userData.email,
            first_name = userData.name.split(" ").firstOrNull() ?: userData.name,
            last_name = userData.name.split(" ").drop(1).joinToString(" ").ifEmpty { userData.name },
            phone_number = userData.phone,
            street = address.street,
            city = address.city,
            country = address.country,
            state = address.state,
            postal_code = address.postalCode.ifEmpty { "NA" },
            apartment = "NA",
            floor = "NA",
            building = "NA",
            shipping_method = "NA"
        )
    }
}

/**
 * PayMob API Service interface
 */
interface PayMobService {
    @POST("auth/tokens")
    suspend fun authenticate(@Body request: PaymobAuthRequest): PaymobAuthResponse

    @POST("auth/tokens")
    suspend fun authenticateWithCredentials(@Body credentials: Map<String, String>): PaymobAuthResponse

    @POST("ecommerce/orders")
    suspend fun registerOrder(@Body request: OrderRegistrationRequest): OrderRegistrationResponse

    @POST("acceptance/payment_keys")
    suspend fun generatePaymentKey(@Body request: PaymentKeyRequest): PaymentKeyResponse

    @POST("ecommerce/orders/transaction_inquiry")
    suspend fun checkTransactionStatusByOrderId(
        @Body request: Map<String, String>,
        @Header("Authorization") authToken: String
    ): TransactionStatusResponse

    @GET("acceptance/transactions/{id}")
    suspend fun checkTransactionStatusById(
        @Path("id") transactionId: String,
        @Header("Authorization") authToken: String
    ): TransactionStatusResponse
}