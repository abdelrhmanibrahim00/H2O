package com.h2o.store.repositories

import android.util.Log
import com.h2o.store.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

class PaymentRepository {
    private val TAG = "PaymentRepository"
    private val API_KEY = PayMobConstants.API_KEY
    private val INTEGRATION_ID = PayMobConstants.CARD_INTEGRATION_ID
    private val IFRAME_ID = PayMobConstants.IFRAME_ID

    private val retrofit = Retrofit.Builder()
        .baseUrl(PayMobConstants.Endpoints.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val payMobService = retrofit.create(PayMobService::class.java)

    /**
     * Complete PayMob payment process:
     * 1. Authenticate with PayMob
     * 2. Register the order
     * 3. Generate payment key
     *
     * @param orderItems List of order items
     * @param totalAmount Total amount in EGP
     * @param billingData Customer billing information
     * @return iframe URL for payment
     */
    suspend fun processPayment(
        orderId: String,
        totalAmount: Double,
        billingData: BillingData
    ): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting payment process for order: $orderId")

            // Step 1: Authentication
            val authResponse = authenticate()
            val authToken = authResponse.token
            Log.d(TAG, "Authentication successful, token obtained")

            // Step 2: Register Order
            val amountCents = (totalAmount * 100).toInt()
            val orderRegistrationResponse = registerOrder(
                authToken = authToken,
                amountCents = amountCents,
                merchantOrderId = orderId
            )
            val payMobOrderId = orderRegistrationResponse.id
            Log.d(TAG, "Order registered with PayMob ID: $payMobOrderId")

            // Step 3: Generate Payment Key
            val paymentKeyResponse = generatePaymentKey(
                authToken = authToken,
                amountCents = amountCents,
                orderId = payMobOrderId,
                billingData = billingData
            )
            val paymentToken = paymentKeyResponse.token
            Log.d(TAG, "Payment key generated successfully")

            // Create iframe URL
            val iframeUrl = "${PayMobConstants.Endpoints.IFRAME_URL}$IFRAME_ID?payment_token=$paymentToken"
            Log.d(TAG, "Created iframe URL: $iframeUrl")

            return@withContext iframeUrl
        } catch (e: Exception) {
            Log.e(TAG, "Error during payment process: ${e.message}", e)
            throw e
        }
    }

    /**
     * Step 1: Authenticate with PayMob API
     */
    private suspend fun authenticate(): PaymobAuthResponse {
        return payMobService.authenticate(PaymobAuthRequest(api_key = API_KEY))
    }

    /**
     * Step 2: Register the order with PayMob
     */
    private suspend fun registerOrder(
        authToken: String,
        amountCents: Int,
        merchantOrderId: String,
        items: List<OrderItem> = listOf()
    ): OrderRegistrationResponse {
        return payMobService.registerOrder(
            OrderRegistrationRequest(
                auth_token = authToken,
                amount_cents = amountCents,
                currency = "EGP",
                merchant_order_id = merchantOrderId,
                items = items
            )
        )
    }

    /**
     * Step 3: Generate payment key
     */
    private suspend fun generatePaymentKey(
        authToken: String,
        amountCents: Int,
        orderId: Long,
        billingData: BillingData
    ): PaymentKeyResponse {
        return payMobService.generatePaymentKey(
            PaymentKeyRequest(
                auth_token = authToken,
                amount_cents = amountCents,
                currency = "EGP",
                order_id = orderId,
                billing_data = billingData,
                integration_id = INTEGRATION_ID,
                lock_order_when_paid = false
            )
        )
    }

    /**
     * Query transaction status
     */
    suspend fun checkTransactionStatus(transactionId: String): PaymentResult = withContext(Dispatchers.IO) {
        try {
            // In a real implementation, you would call the transaction status API
            // For now, return a mock success response
            return@withContext PaymentResult(
                success = true,
                transactionId = transactionId,
                orderId = "",  // You would get this from the API response
                amount = 0.0   // You would get this from the API response
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
            // Default values for required fields that might be empty
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

    @POST("ecommerce/orders")
    suspend fun registerOrder(@Body request: OrderRegistrationRequest): OrderRegistrationResponse

    @POST("acceptance/payment_keys")
    suspend fun generatePaymentKey(@Body request: PaymentKeyRequest): PaymentKeyResponse
}