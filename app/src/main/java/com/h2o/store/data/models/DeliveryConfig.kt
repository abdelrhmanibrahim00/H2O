package com.h2o.store.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

/**
 * Model representing global delivery configuration settings
 */
data class DeliveryConfig(
    @DocumentId
    val id: String = "global_config",

    @PropertyName("standardDeliveryFee")
    val standardDeliveryFee: Double = 0.0,

    @PropertyName("freeDeliveryThresholdMarket")
    val freeDeliveryThresholdMarket: Double = 0.0,

    @PropertyName("freeDeliveryThresholdHome")
    val freeDeliveryThresholdHome: Double = 0.0,

    @ServerTimestamp
    @PropertyName("lastUpdated")
    val lastUpdated: Timestamp? = null
)

/**
 * Model representing a delivery district
 */
/**
 * Model representing a delivery district
 */
data class District(
    @DocumentId
    val id: String = "",

    @PropertyName("name")
    val name: String = "",

    @ServerTimestamp
    @PropertyName("addedAt")
    val addedAt: Timestamp? = null
)