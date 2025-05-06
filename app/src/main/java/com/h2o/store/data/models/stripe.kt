package com.h2o.store.data.models

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.h2o.store.data.User.UserData
import com.stripe.android.PaymentConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * First, let's define the missing data models
 */

// PaymentResult model - matching the one used with PayMob
data class PaymentResult(
    val success: Boolean,
    val transactionId: String? = null,
    val orderId: String? = null,
    val amount: Double? = null,
    val errorMessage: String? = null
)

// BillingData model - matching the one used with PayMob
data class BillingData(
    val email: String,
    val first_name: String,
    val last_name: String,
    val phone_number: String,
    val street: String,
    val city: String,
    val country: String,
    val state: String,
    val postal_code: String,
    val apartment: String,
    val floor: String,
    val building: String,
    val shipping_method: String
)

// UserData model with address field
data class UserData(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val address: Address?
)

// Address model for user data
data class Address(
    val street: String,
    val city: String,
    val state: String,
    val country: String,
    val postalCode: String
)

/**
 * Helper class for Stripe payment processing
 */
class StripeHelper(private val context: Context) {
    private val TAG = "StripeHelper"

    // Your Stripe publishable key
    private val PUBLISHABLE_KEY = "pk_test_51RHXPD4KMJOi3cpv9VSYzh8K8oIBXO47ZFwu7Yu5UYVXgN78bPcQJEPwfde9VKupKLK7rVrY8va3z4FIiqWwGzbB00AzRXBUXk"

    // Initialize Stripe SDK
    init {
        PaymentConfiguration.init(context, PUBLISHABLE_KEY)
    }

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
        .baseUrl("http://139.13.51.99:7860/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val stripeService = retrofit.create(StripeService::class.java)

    /**
     * Create a payment intent on your backend
     */
    suspend fun createPaymentIntent(
        orderId: String,
        amount: Double,
        currency: String = "EGP",
        billingData: BillingData
    ): PaymentIntentResponse = withContext(Dispatchers.IO) {
        try {
            val amountInCents = (amount * 100).toLong()
            val request = CreatePaymentIntentRequest(
                amount = amountInCents,
                currency = currency,
                orderId = orderId,
                customerEmail = billingData.email,
                customerName = "${billingData.first_name} ${billingData.last_name}",
                description = "Order #$orderId"
            )

            val response = stripeService.createPaymentIntent(request)

            // Use the session URL if available
            if (response.url != null) {
                return@withContext PaymentIntentResponse(
                    id = response.id,
                    clientSecret = response.url, // Use the URL as the clientSecret
                    amount = response.amount,
                    currency = response.currency,
                    status = response.status,
                    metadata = response.metadata,
                    url = response.url
                )
            }

            return@withContext response
        } catch (e: Exception) {
            Log.e(TAG, "Error creating payment intent: ${e.message}", e)
            throw e
        }
    }

    /**
     * Get Stripe checkout URL
     */
    fun getStripeCheckoutUrl(clientSecret: String): String {
        // If the clientSecret is actually a URL (from checkout.sessions.create)
        if (clientSecret.startsWith("http")) {
            return clientSecret
        }
        // Otherwise, it's a real client secret and we use the payment element URL
        return "https://checkout.stripe.com/c/pay/$clientSecret"
    }

    /**
     * Open Stripe checkout URL in device's browser instead of WebView
     */
    fun openStripeCheckout(context: Context, checkoutUrl: String) {
        Log.d(TAG, "Opening Stripe checkout in browser: $checkoutUrl")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(checkoutUrl))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Check payment status
     */
    suspend fun checkPaymentStatus(
        paymentIntentId: String
    ): PaymentResult = withContext(Dispatchers.IO) {
        try {
            val response = stripeService.retrievePaymentIntent(
                RetrievePaymentIntentRequest(paymentIntentId = paymentIntentId)
            )

            return@withContext PaymentResult(
                success = response.status == "succeeded",
                transactionId = response.id,
                orderId = response.metadata?.orderId ?: "",
                amount = response.amount / 100.0,
                errorMessage = if (response.status != "succeeded") "Payment not successful: ${response.status}" else null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking payment status: ${e.message}", e)
            return@withContext PaymentResult(
                success = false,
                errorMessage = e.message ?: "Unknown error"
            )
        }
    }

    /**
     * Convert UserData to Stripe BillingDetails
     */
    fun convertUserDataToStripeBillingDetails(userData: UserData): BillingData {
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

// Data models for Stripe API
data class CreatePaymentIntentRequest(
    val amount: Long,
    val currency: String,
    val orderId: String,
    val customerEmail: String,
    val customerName: String,
    val description: String
)

data class RetrievePaymentIntentRequest(
    val paymentIntentId: String
)

data class PaymentIntentResponse(
    val id: String,
    val clientSecret: String,
    val amount: Long,
    val currency: String,
    val status: String,
    val metadata: PaymentIntentMetadata?,
    val url: String? = null // Add URL field
)

// Represents Stripe payment status
data class StripePaymentStatus(
    val id: String,
    val status: String,
    val amount: Long,
    val currency: String,
    val clientSecret: String?
)

// Payment Intent confirmation parameters
data class StripeConfirmParams(
    val paymentMethodId: String,
    val paymentIntentId: String
)

// Response from backend on confirm
data class StripeConfirmResponse(
    val success: Boolean,
    val paymentIntentId: String,
    val requiresAction: Boolean,
    val clientSecret: String?
)

data class PaymentIntentMetadata(
    val orderId: String
)

// Stripe API Service interface
interface StripeService {
    @POST("create-payment-intent")
    suspend fun createPaymentIntent(@Body request: CreatePaymentIntentRequest): PaymentIntentResponse

    @POST("retrieve-payment-intent")
    suspend fun retrievePaymentIntent(@Body request: RetrievePaymentIntentRequest): PaymentIntentResponse
}