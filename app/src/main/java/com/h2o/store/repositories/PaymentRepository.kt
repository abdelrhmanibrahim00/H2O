package com.h2o.store.repositories

import com.h2o.store.data.models.BillingData
import com.h2o.store.data.models.OrderItem
import com.h2o.store.data.models.OrderRegistrationRequest
import com.h2o.store.data.models.OrderRegistrationResponse
import com.h2o.store.data.models.PaymentKeyRequest
import com.h2o.store.data.models.PaymentKeyResponse
import com.h2o.store.data.models.PaymobAuthRequest
import com.h2o.store.data.models.PaymobAuthResponse
import com.h2o.store.data.models.TransactionQueryRequest
import com.h2o.store.data.models.TransactionStatusResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Repository for handling payments through PayMob
 */
class PaymentRepository {

    companion object {
        // PayMob API constants
        private const val BASE_URL = "https://accept.paymob.com/api/"
        private const val IFRAME_URL = "https://accept.paymob.com/api/acceptance/iframes/"

        // PayMob integration IDs
        private const val CARD_INTEGRATION_ID = 158 // Your card integration ID
        private const val CASH_INTEGRATION_ID = 159 // Your cash on delivery integration ID

        // PayMob iframe IDs
        private const val CARD_IFRAME_ID = "123456" // Your card iframe ID

        // PayMob API key
        private const val API_KEY = "ZXlKaGJHY2lPaUpJVXpVeE1pSXNJblI1Y0NJNklrcFhWQ0o5LmV5SmpiR0Z6Y3lJNklrMWxjbU5vWVc1MElpd2ljSEp2Wm1sc1pWOXdheUk2TVRBek9EZzJOU3dpYm1GdFpTSTZJbWx1YVhScFlXd2lmUS5kcGJPYi0yMkljYmRVOUtoX3NlOE5kTm50ZDhiY2FqY2tDZC00QXhhYjhycnMyalhtUlpRVFBHUXo1RXlmSXY2R1dSbmtEYU5jUjMwbkxFbnYzMWRaQQ=="
    }

    private val apiService: PaymobApiService

    init {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(PaymobApiService::class.java)
    }

    /**
     * Initiates a payment request for cards
     */
    suspend fun initiateCardPayment(
        amount: Double,
        userId: String,
        userEmail: String,
        firstName: String,
        lastName: String,
        phone: String,
        address: String,
        city: String,
        country: String,
        state: String,
        items: List<OrderItem>
    ): Result<String> {
        return try {
            // Step 1: Authentication
            val authResponse = authenticate()
            val authToken = authResponse.token

            // Step 2: Order Registration
            val orderAmount = (amount * 100).toInt() // Amount in cents
            val merchantOrderId = generateMerchantOrderId(userId)
            val orderResponse = registerOrder(
                authToken,
                orderAmount,
                merchantOrderId,
                items
            )

            // Step 3: Payment Key Request
            val billingData = BillingData(
                email = userEmail,
                first_name = firstName,
                last_name = lastName,
                phone_number = phone,
                street = address,
                city = city,
                country = country,
                state = state
            )

            val paymentKeyResponse = getPaymentKey(
                authToken = authToken,
                orderId = orderResponse.id,
                amountCents = orderAmount,
                billingData = billingData,
                integrationId = CARD_INTEGRATION_ID
            )

            // Step 4: Construct iframe URL for webview
            val iframeUrl = "$IFRAME_URL$CARD_IFRAME_ID?payment_token=${paymentKeyResponse.token}"
            Result.success(iframeUrl)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Places a cash on delivery order
     */
    suspend fun placeCashOnDeliveryOrder(
        amount: Double,
        userId: String,
        userEmail: String,
        firstName: String,
        lastName: String,
        phone: String,
        address: String,
        city: String,
        country: String,
        state: String,
        items: List<OrderItem>
    ): Result<Long> {
        return try {
            // Step 1: Authentication
            val authResponse = authenticate()
            val authToken = authResponse.token

            // Step 2: Order Registration
            val orderAmount = (amount * 100).toInt() // Amount in cents
            val merchantOrderId = generateMerchantOrderId(userId)
            val orderResponse = registerOrder(
                authToken,
                orderAmount,
                merchantOrderId,
                items
            )

            // Return the order ID for reference
            Result.success(orderResponse.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Authenticates with the PayMob API
     */
    private suspend fun authenticate(): PaymobAuthResponse {
        val request = PaymobAuthRequest(api_key = API_KEY)
        return withContext(Dispatchers.IO) {
            val response = apiService.authenticate(request)
            if (response.isSuccessful) {
                response.body() ?: throw Exception("Authentication failed: Empty response")
            } else {
                throw Exception("Authentication failed: ${response.errorBody()?.string()}")
            }
        }
    }

    /**
     * Registers an order with PayMob
     */
    private suspend fun registerOrder(
        authToken: String,
        amountCents: Int,
        merchantOrderId: String,
        items: List<OrderItem>
    ): OrderRegistrationResponse {
        val request = OrderRegistrationRequest(
            auth_token = authToken,
            amount_cents = amountCents,
            currency = "EGP",
            delivery_needed = false,
            merchant_order_id = merchantOrderId,
            items = items
        )

        return withContext(Dispatchers.IO) {
            val response = apiService.registerOrder(request)
            if (response.isSuccessful) {
                response.body() ?: throw Exception("Order registration failed: Empty response")
            } else {
                throw Exception("Order registration failed: ${response.errorBody()?.string()}")
            }
        }
    }

    /**
     * Gets a payment key from PayMob
     */
    private suspend fun getPaymentKey(
        authToken: String,
        orderId: Long,
        amountCents: Int,
        billingData: BillingData,
        integrationId: Int
    ): PaymentKeyResponse {
        val request = PaymentKeyRequest(
            auth_token = authToken,
            amount_cents = amountCents,
            order_id = orderId,
            billing_data = billingData,
            currency = "EGP",
            integration_id = integrationId,
            lock_order_when_paid = false
        )

        return withContext(Dispatchers.IO) {
            val response = apiService.getPaymentKey(request)
            if (response.isSuccessful) {
                response.body() ?: throw Exception("Payment key generation failed: Empty response")
            } else {
                throw Exception("Payment key generation failed: ${response.errorBody()?.string()}")
            }
        }
    }

    /**
     * Checks the status of a transaction
     */
    suspend fun checkTransactionStatus(orderId: String): Result<TransactionStatusResponse> {
        return try {
            // Step 1: Authentication
            val authResponse = authenticate()
            val authToken = authResponse.token

            // Step 2: Query transaction
            val request = TransactionQueryRequest(order_id = orderId)

            withContext(Dispatchers.IO) {
                val response = apiService.queryTransaction(
                    "Bearer ${authToken}",
                    request
                )

                if (response.isSuccessful) {
                    Result.success(response.body() ?: throw Exception("Transaction query failed: Empty response"))
                } else {
                    Result.failure(Exception("Transaction query failed: ${response.errorBody()?.string()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generates a unique merchant order ID
     */
    private fun generateMerchantOrderId(userId: String): String {
        val randomPart = UUID.randomUUID().toString().substring(0, 8)
        return "h2o-$userId-$randomPart"
    }

    /**
     * PayMob API service interface
     */
    interface PaymobApiService {
        @POST("auth/tokens")
        suspend fun authenticate(@Body request: PaymobAuthRequest): Response<PaymobAuthResponse>

        @POST("ecommerce/orders")
        suspend fun registerOrder(@Body request: OrderRegistrationRequest): Response<OrderRegistrationResponse>

        @POST("acceptance/payment_keys")
        suspend fun getPaymentKey(@Body request: PaymentKeyRequest): Response<PaymentKeyResponse>

        @POST("ecommerce/orders/transaction_inquiry")
        suspend fun queryTransaction(
            @Header("Authorization") authorization: String,
            @Body request: TransactionQueryRequest
        ): Response<TransactionStatusResponse>
    }
}