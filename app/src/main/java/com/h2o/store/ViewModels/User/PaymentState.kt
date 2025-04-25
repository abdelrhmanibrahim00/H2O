package com.h2o.store.ViewModels.User

/**
 * Represents the different states of the payment process
 */
sealed class PaymentState {
    /**
     * Initial state before payment process begins
     */
    object Initial : PaymentState()

    /**
     * Loading state while payment is being processed
     */
    object Loading : PaymentState()

    /**
     * Error state when payment process fails
     */
    data class Error(val message: String) : PaymentState()

    /**
     * Ready state when payment URL is generated and ready to display
     */
    data class Ready(val paymentUrl: String) : PaymentState()

    /**
     * Success state when payment is completed successfully
     */
    data class Success(val transactionId: String) : PaymentState()

    /**
     * State indicating user needs to add an address before proceeding with payment
     */
    object AddressRequired : PaymentState()
}