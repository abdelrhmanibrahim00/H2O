package com.h2o.store.ViewModels.User

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.h2o.store.data.models.PaymentResult
import com.h2o.store.data.Cart.CartItem
import com.h2o.store.data.User.UserData
import com.h2o.store.repositories.CheckoutRepository
import com.h2o.store.repositories.PaymentRepository
import com.h2o.store.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Coordinator ViewModel that coordinates between checkout and payment processes.
 * It manages the state transitions and data sharing between these two processes.
 */
class CheckoutCoordinatorViewModel(
    private val userId: String,
    private val checkoutRepository: CheckoutRepository,
    internal val paymentRepository: PaymentRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    // Payment states
    sealed class PaymentState {
        object Initial : PaymentState()
        object Loading : PaymentState()
        data class Error(val message: String) : PaymentState()
        data class Ready(val paymentUrl: String) : PaymentState()
        data class Success(val transactionId: String) : PaymentState()
        object AddressRequired : PaymentState()
    }

    // Checkout state
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _orderSuccess = MutableStateFlow(false)
    val orderSuccess: StateFlow<Boolean> = _orderSuccess.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // User data
    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData.asStateFlow()

    private val _isUserDataLoading = MutableStateFlow(true)
    val isUserDataLoading: StateFlow<Boolean> = _isUserDataLoading.asStateFlow()

    // Payment specific state
    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Initial)
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()

    private val _paymentUrl = MutableStateFlow<String?>(null)
    val paymentUrl: StateFlow<String?> = _paymentUrl.asStateFlow()

    private val _paymentResult = MutableStateFlow<PaymentResult?>(null)
    val paymentResult: StateFlow<PaymentResult?> = _paymentResult.asStateFlow()

    // Cart reference for clearing
    private val cartViewModel = CartViewModel.Factory(userId).create(CartViewModel::class.java)

    init {
        // Fetch user data immediately
        fetchUserData()
    }

    // Fetch user data including address
    private fun fetchUserData() {
        viewModelScope.launch {
            _isUserDataLoading.value = true
            try {
                val data = userRepository.getUserData(userId)
                _userData.value = data
            } catch (e: Exception) {
                _error.value = "Failed to load user data: ${e.message}"
            } finally {
                _isUserDataLoading.value = false
            }
        }
    }

    // Check if user has a valid address
    fun hasValidAddress(): Boolean {
        val userData = _userData.value
        return userData?.address != null &&
                userData.address.street.isNotEmpty() &&
                userData.address.city.isNotEmpty()
    }

    // Process order with selected payment method
    fun placeOrder(
        cartItems: List<CartItem>,
        totalAmount: Double,
        paymentMethod: String
    ) {
        if (cartItems.isEmpty()) {
            _error.value = "Your cart is empty"
            return
        }

        // For credit card payments, check address and redirect to payment
        if (paymentMethod == "Credit Card") {
            if (!hasValidAddress()) {
                _error.value = "Please add a shipping address before proceeding to payment"
                _paymentState.value = PaymentState.AddressRequired
                return
            }

            // Generate a temporary order ID for payment tracking
            val tempOrderId = UUID.randomUUID().toString()

            // Initiate payment process
            initiatePayment(tempOrderId, totalAmount, cartItems)
            return
        }

        // For other payment methods (e.g., Cash on Delivery)
        processNonCardPayment(cartItems, totalAmount, paymentMethod)
    }

    // Handle Cash on Delivery or other non-card payments
    private fun processNonCardPayment(
        cartItems: List<CartItem>,
        totalAmount: Double,
        paymentMethod: String
    ) {
        val userData = _userData.value
        if (userData == null) {
            _error.value = "User data not available"
            return
        }

        val address = userData.address
        if (address == null) {
            _error.value = "No address available for delivery"
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            _error.value = null

            try {
                // Place order directly with the repository
                val success = checkoutRepository.placeOrder(
                    userId = userId,
                    cartItems = cartItems,
                    subtotal = totalAmount,
                    paymentMethod = paymentMethod,
                    address = address,
                    userName = userData.name,
                    userPhone = userData.phone,
                    userEmail = userData.email
                )

                if (success) {
                    // Clear cart after successful order
                    cartViewModel.clearCart()
                    _orderSuccess.value = true
                } else {
                    _error.value = "Failed to place order"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An unknown error occurred"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    // Initiate payment process
     fun initiatePayment(
        orderId: String,
        totalAmount: Double,
        cartItems: List<CartItem>
    ) {
        viewModelScope.launch {
            _paymentState.value = PaymentState.Loading
            _error.value = null

            try {
                val userData = _userData.value ?: throw IllegalStateException("User data not available")
                val billingData = paymentRepository.convertAddressToPayMobBilling(userData)

                // Generate payment URL
                val paymentIframeUrl = paymentRepository.processPayment(
                    orderId = orderId,
                    totalAmount = totalAmount,
                    billingData = billingData
                )

                _paymentUrl.value = paymentIframeUrl
                _paymentState.value = PaymentState.Ready(paymentIframeUrl)
            } catch (e: Exception) {
                _error.value = "Failed to initiate payment: ${e.message}"
                _paymentState.value = PaymentState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // Handle payment success
    // Inside CheckoutCoordinatorViewModel.kt

    /**
     * Handle payment success - Stripe version
     */
    fun handlePaymentSuccess(transactionId: String) {
        viewModelScope.launch {
            try {
                // Verify transaction with payment gateway
                val result = paymentRepository.checkTransactionStatus(transactionId)
                _paymentResult.value = result

                if (result.success) {
                    // Get cart items and complete order
                    val cartItems = cartViewModel.cartItems.value
                    val userData = userData.value

                    if (userData != null && userData.address != null) {
                        val success = checkoutRepository.placeOrder(
                            userId = userId,
                            cartItems = cartItems,
                            subtotal = cartViewModel.totalPrice.value.toDouble(),
                            paymentMethod = "Credit Card",
                            address = userData.address,
                            userName = userData.name,
                            userPhone = userData.phone,
                            userEmail = userData.email
                        )

                        if (success) {
                            // Clear cart and set order success
                            cartViewModel.clearCart()
                            _orderSuccess.value = true
                            _paymentState.value = PaymentState.Success(transactionId)
                        } else {
                            _error.value = "Payment was successful but order couldn't be saved"
                        }
                    } else {
                        _error.value = "User data or address not available"
                    }
                } else {
                    _error.value = "Payment verification failed: ${result.errorMessage}"
                }
            } catch (e: Exception) {
                _error.value = "Error finalizing payment: ${e.message}"
            }
        }
    }

    // Handle payment failure
    fun handlePaymentFailure(errorMessage: String?) {
        _error.value = errorMessage ?: "Payment was not completed"
        _paymentResult.value = PaymentResult(
            success = false,
            errorMessage = errorMessage ?: "Payment was cancelled or failed"
        )
        _paymentState.value = PaymentState.Error(errorMessage ?: "Payment failed")
    }

    // Reset payment state
    fun resetPayment() {
        _paymentState.value = PaymentState.Initial
        _paymentUrl.value = null
        _paymentResult.value = null
        _error.value = null
    }

    /**
     * Initiate payment process
     */
    fun CheckoutCoordinatorViewModel.initiatePayment(
        orderId: String,
        totalAmount: Double,
        cartItems: List<CartItem>
    ) {
        viewModelScope.launch {
            _paymentState.value = PaymentState.Loading
            _error.value = null

            try {
                val userData = userData.value ?: throw IllegalStateException("User data not available")

                if (userData.address == null) {
                    _error.value = "Shipping address is required to proceed with payment"
                    _paymentState.value = PaymentState.AddressRequired
                    return@launch
                }

                val billingData = paymentRepository.convertAddressToPayMobBilling(userData)

                // Generate payment URL
                val paymentIframeUrl = paymentRepository.processPayment(
                    orderId = orderId,
                    totalAmount = totalAmount,
                    billingData = billingData
                )

                _paymentUrl.value = paymentIframeUrl
                _paymentState.value = PaymentState.Ready(paymentIframeUrl)
            } catch (e: Exception) {
                _error.value = "Failed to initiate payment: ${e.message}"
                _paymentState.value = PaymentState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Handle payment success
     */
    fun CheckoutCoordinatorViewModel.handlePaymentSuccess(transactionId: String) {
        viewModelScope.launch {
            try {
                // Verify transaction with payment gateway
                val result = paymentRepository.checkTransactionStatus(transactionId)
                _paymentResult.value = result

                if (result.success) {
                    // Get cart items and complete order
                    val cartItems = cartViewModel.cartItems.value
                    val userData = userData.value

                    if (userData != null && userData.address != null) {
                        val success = checkoutRepository.placeOrder(
                            userId = userId,
                            cartItems = cartItems,
                            subtotal = cartViewModel.totalPrice.value.toDouble(),
                            paymentMethod = "Credit Card",
                            address = userData.address,
                            userName = userData.name,
                            userPhone = userData.phone,
                            userEmail = userData.email
                        )

                        if (success) {
                            // Clear cart and set order success
                            cartViewModel.clearCart()
                            _orderSuccess.value = true
                            _paymentState.value = PaymentState.Success(transactionId)
                        } else {
                            _error.value = "Payment was successful but order couldn't be saved"
                        }
                    } else {
                        _error.value = "User data or address not available"
                    }
                } else {
                    _error.value = "Payment verification failed: ${result.errorMessage}"
                }
            } catch (e: Exception) {
                _error.value = "Error finalizing payment: ${e.message}"
            }
        }
    }

    /**
     * Handle payment failure
     */
    fun CheckoutCoordinatorViewModel.handlePaymentFailure(errorMessage: String?) {
        _error.value = errorMessage ?: "Payment was not completed"
        _paymentResult.value = PaymentResult(
            success = false,
            errorMessage = errorMessage ?: "Payment was cancelled or failed"
        )
        _paymentState.value = PaymentState.Error(errorMessage ?: "Payment failed")
    }

    // Factory for creating the ViewModel with required dependencies
    class Factory(
        private val userId: String,
        private val checkoutRepository: CheckoutRepository,
        private val paymentRepository: PaymentRepository,
        private val userRepository: UserRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CheckoutCoordinatorViewModel::class.java)) {
                return CheckoutCoordinatorViewModel(
                    userId = userId,
                    checkoutRepository = checkoutRepository,
                    paymentRepository = paymentRepository,
                    userRepository = userRepository
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}