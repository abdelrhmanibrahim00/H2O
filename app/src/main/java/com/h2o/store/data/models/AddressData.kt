package com.h2o.store.data.models


data class AddressData(
    val street: String = "",
    val city: String = "",
    val state: String = "",
    val country: String = "",
    val postalCode: String = "",
    val formattedAddress: String = ""
)