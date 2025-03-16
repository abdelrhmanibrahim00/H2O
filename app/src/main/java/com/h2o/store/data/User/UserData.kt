package com.h2o.store.data.User

import com.h2o.store.data.models.AddressData


data class UserData(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val whatsapp: String? = null,
    val address: AddressData? = null,
    val city: String = "",
    val district: String = ""
)