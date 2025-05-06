package com.h2o.store.data.models


/**
 * PayMob API authentication request model
 */
data class PaymobAuthRequest(
    val api_key: String
)

/**
 * PayMob API authentication response model
 */
data class PaymobAuthResponse(
    val token: String,
    val profile: Profile
) {
    data class Profile(
        val id: Int,
        val user: User,
        val created_at: String,
        val active: Boolean,
        val profile_type: String,
        val phones: List<String>,
        val company_emails: List<String>,
        val company_name: String,
        val state: String,
        val country: String,
        val city: String,
        val postal_code: String,
        val street: String
    ) {
        data class User(
            val id: Int,
            val username: String,
            val first_name: String,
            val last_name: String,
            val date_joined: String,
            val email: String,
            val is_active: Boolean,
            val is_staff: Boolean,
            val is_superuser: Boolean
        )
    }
}

/**
 * PayMob Order Registration request model
 */
data class OrderRegistrationRequest(
    val auth_token: String,
    val delivery_needed: Boolean = false,
    val amount_cents: Int,
    val currency: String = "EGP",
    val merchant_order_id: String,
    val items: List<OrderItem> = listOf()
)

data class OrderItem(
    val name: String,
    val amount_cents: Int,
    val description: String,
    val quantity: Int
)

/**
 * PayMob Order Registration response model
 */
data class OrderRegistrationResponse(
    val id: Long,
    val created_at: String,
    val delivery_needed: Boolean,
    val merchant: Merchant,
    val amount_cents: Int,
    val shipping_data: ShippingData?,
    val currency: String,
    val is_payment_locked: Boolean,
    val is_return: Boolean,
    val is_cancel: Boolean,
    val is_returned: Boolean,
    val is_canceled: Boolean,
    val merchant_order_id: String,
    val wallet_notification: Any?,
    val paid_amount_cents: Int,
    val notify_user_with_email: Boolean,
    val items: List<OrderItem>,
    val order_url: String,
    val commission_fees: Int,
    val delivery_fees_cents: Int
) {
    data class Merchant(
        val id: Int,
        val created_at: String,
        val phones: List<String>,
        val company_emails: List<String>,
        val company_name: String,
        val state: String,
        val country: String,
        val city: String,
        val postal_code: String,
        val street: String
    )

    data class ShippingData(
        val id: Int,
        val first_name: String,
        val last_name: String,
        val street: String,
        val building: String,
        val floor: String,
        val apartment: String,
        val city: String,
        val state: String,
        val country: String,
        val email: String,
        val phone_number: String,
        val postal_code: String,
        val extra_description: String,
        val shipping_method: String,
        val order_id: Int,
        val order: Int
    )
}

/**
 * PayMob Payment Key request model
 */
data class PaymentKeyRequest(
    val auth_token: String,
    val amount_cents: Int,
    val expiration: Int = 3600,
    val order_id: Long,
    val billing_data: BillingData,
    val currency: String = "EGP",
    val integration_id: Int,
    val lock_order_when_paid: Boolean = false
)

data class BillingData(
    val apartment: String = "NA",
    val email: String,
    val floor: String = "NA",
    val first_name: String,
    val street: String,
    val building: String = "NA",
    val phone_number: String,
    val shipping_method: String = "NA",
    val postal_code: String = "NA",
    val city: String,
    val country: String,
    val last_name: String,
    val state: String
)

/**
 * PayMob Payment Key response model
 */
data class PaymentKeyResponse(
    val token: String
)

/**
 * Transaction status model
 */
data class TransactionStatusResponse(
    val id: Long,
    val pending: Boolean,
    val amount_cents: Int,
    val success: Boolean,
    val is_auth: Boolean,
    val is_capture: Boolean,
    val is_standalone_payment: Boolean,
    val is_voided: Boolean,
    val is_refunded: Boolean,
    val is_3d_secure: Boolean,
    val integration_id: Int,
    val profile_id: Int,
    val has_parent_transaction: Boolean,
    val order: Order,
    val created_at: String,
    val currency: String,
    val source_data: SourceData,
    val api_source: String,
    val is_void: Boolean,
    val is_refund: Boolean,
    val error_occured: Boolean,
    val is_live: Boolean,
    val other_endpoint_reference: String?,
    val refunded_amount_cents: Int?,
    val source_id: Int,
    val is_captured: Boolean,
    val captured_amount: Int?,
    val merchant_staff_tag: String?,
    val updated_at: String,
    val owner: Int,
    val parent_transaction: Any?
) {
    data class Order(
        val id: Long
    )

    data class SourceData(
        val type: String,
        val pan: String,
        val sub_type: String
    )
}

/**
 * Model for transaction query
 */
data class TransactionQueryRequest(
    val order_id: String
)

/**
 * Model to handle payment results from PayMob
 */
data class PaymentResult(
    val success: Boolean,
    val orderId: String? = null,
    val transactionId: String? = null,
    val amount: Double? = null,
    val errorMessage: String? = null
)