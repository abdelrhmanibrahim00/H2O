package com.h2o.store.ViewModels.User

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.h2o.store.data.Cart.CartItem
import com.h2o.store.data.User.UserData
import com.h2o.store.data.models.PaymentResult
import com.h2o.store.repositories.CheckoutRepository
import com.h2o.store.repositories.PaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PaymentViewModel(
    private val userId: String,
    private val orderId: String,
    private val cartItems: List<CartItem>,
    private val totalAmount: Double,
    private val hasAddress: Boolean,
    private val userData: UserData
) : ViewModel() {
    private val TAG = "PaymentViewModel"

    private val paymentRepository = PaymentRepository()
    private val checkoutRepository = CheckoutRepository()

    // Create CartViewModel using Factory instead of direct constructor
    private val cartViewModel = CartViewModel.Factory(userId).create(CartViewModel::class.java)

    // Payment processing state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _paymentUrl = MutableStateFlow<String?>(null)
    val paymentUrl: StateFlow<String?> = _paymentUrl.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _paymentResult = MutableStateFlow<PaymentResult?>(null)
    val paymentResult: StateFlow<PaymentResult?> = _paymentResult.asStateFlow()

    /**
     * Initiate the payment process
     */
    fun initiatePayment() {
        // Check if the user has an address before proceeding
        if (!hasAddress || userData.address == null) {
            _error.value = "Shipping address is required to proceed with payment"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                Log.d(TAG, "Initiating payment for order: $orderId")

                // Convert user data to PayMob billing data
                val billingData = paymentRepository.convertAddressToPayMobBilling(userData)

                // Process payment with PayMob
                val paymentIframeUrl = paymentRepository.processPayment(
                    orderId = orderId,
                    totalAmount = totalAmount,
                    billingData = billingData
                )

                _paymentUrl.value = paymentIframeUrl
                Log.d(TAG, "Payment URL generated: $paymentIframeUrl")
            } catch (e: Exception) {
                Log.e(TAG, "Payment initiation failed: ${e.message}", e)
                _error.value = "Failed to initiate payment: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Handle payment successful completion
     */
    fun handlePaymentSuccess(transactionId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                Log.d(TAG, "Payment successful for transaction: $transactionId")

                // Check transaction status
                val result = paymentRepository.checkTransactionStatus(transactionId)
                _paymentResult.value = result

                if (result.success) {
                    // Save order to Firebase
                    val savedToFirebase = saveOrderToFirebase("Credit Card")

                    if (savedToFirebase) {
                        // Clear cart on successful order
                        cartViewModel.clearCart()
                    } else {
                        _error.value = "Payment was successful but order couldn't be saved"
                    }
                } else {
                    _error.value = "Payment verification failed: ${result.errorMessage}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing payment success: ${e.message}", e)
                _error.value = "Error finalizing payment: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Handle payment cancellation or failure
     */
    fun handlePaymentFailure(errorMessage: String?) {
        Log.d(TAG, "Payment failed: $errorMessage")
        _error.value = errorMessage ?: "Payment was not completed"
        _paymentResult.value = PaymentResult(
            success = false,
            errorMessage = errorMessage ?: "Payment was cancelled or failed"
        )
    }

    /**
     * Save order to Firebase after successful payment
     */
    private suspend fun saveOrderToFirebase(paymentMethod: String): Boolean {
        return try {
            val address = userData.address ?: throw IllegalArgumentException("Address is required")

            checkoutRepository.placeOrder(
                userId = userId,
                cartItems = cartItems,
                subtotal = totalAmount,
                paymentMethod = paymentMethod,
                address = address,
                userName = userData.name,
                userPhone = userData.phone,
                userEmail = userData.email
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save order to Firebase: ${e.message}", e)
            false
        }
    }

    /**
     * Factory to create PaymentViewModel with required parameters
     */
    class Factory(
        private val userId: String,
        private val orderId: String,
        private val cartItems: List<CartItem>,
        private val totalAmount: Double,
        private val hasAddress: Boolean,
        private val userData: UserData
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PaymentViewModel::class.java)) {
                return PaymentViewModel(
                    userId = userId,
                    orderId = orderId,
                    cartItems = cartItems,
                    totalAmount = totalAmount,
                    hasAddress = hasAddress,
                    userData = userData
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}