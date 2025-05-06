package com.h2o.store.repositories

import android.content.Context
import android.util.Log
import com.h2o.store.data.User.UserData
import com.h2o.store.data.models.BillingData
import com.h2o.store.data.models.PaymentResult
import com.h2o.store.data.models.StripeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for handling payment operations using Stripe
 */
class PaymentRepository(private val context: Context) {
    private val TAG = "PaymentRepository"

    // Initialize Stripe helper
    private val stripeHelper = StripeHelper(context)

    // Keep these for backward compatibility
    private val INTEGRATION_ID = "stripe_integration_id"
    private val IFRAME_ID = "stripe_iframe_id"

    /**
     * Process payment using Stripe
     * This maintains the same method signature as the original PayMob implementation
     */
    suspend fun processPayment(
        orderId: String,
        totalAmount: Double,
        billingData: BillingData
    ): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting Stripe payment process for order: $orderId")

            // Create payment intent using Stripe helper
            val paymentIntent = stripeHelper.createPaymentIntent(
                orderId = orderId,
                amount = totalAmount,
                billingData = billingData
            )

            // Get checkout URL from Stripe helper
            val checkoutUrl = stripeHelper.getStripeCheckoutUrl(paymentIntent.clientSecret)
            Log.d(TAG, "Created Stripe checkout URL: $checkoutUrl")

            return@withContext checkoutUrl
        } catch (e: Exception) {
            Log.e(TAG, "Error during Stripe payment process: ${e.message}", e)
            throw e
        }
    }

    /**
     * Query transaction status by order ID
     * Note: In Stripe, we typically use payment intent ID rather than order ID
     * This implementation maintains compatibility with existing code
     */
    suspend fun checkTransactionStatus(orderId: String): PaymentResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking transaction status for order: $orderId")

            // In a real implementation, you would need to look up the payment intent ID
            // associated with this order ID from your database
            // For now, we'll use a placeholder implementation

            // This would be replaced with actual lookup logic in your production code
            val mockPaymentIntentId = "pi_" + orderId.replace("-", "")

            return@withContext stripeHelper.checkPaymentStatus(mockPaymentIntentId)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking transaction status: ${e.message}", e)
            return@withContext PaymentResult(
                success = false,
                errorMessage = e.message ?: "Unknown error"
            )
        }
    }

    /**
     * Query transaction status by transaction ID (payment intent ID in Stripe)
     */
    suspend fun checkTransactionStatusById(transactionId: String): PaymentResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking transaction status by ID: $transactionId")

            // Direct call to Stripe helper to check payment status
            return@withContext stripeHelper.checkPaymentStatus(transactionId)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking transaction status: ${e.message}", e)
            return@withContext PaymentResult(
                success = false,
                errorMessage = e.message ?: "Unknown error"
            )
        }
    }

    /**
     * Convert UserData to BillingData for Stripe
     * Maintains the same method signature as before for compatibility
     */
    fun convertAddressToPayMobBilling(userData: UserData): BillingData {
        return stripeHelper.convertUserDataToStripeBillingDetails(userData)
    }

    // We're keeping the method name the same for compatibility,
    // even though we're now using Stripe instead of PayMob
}