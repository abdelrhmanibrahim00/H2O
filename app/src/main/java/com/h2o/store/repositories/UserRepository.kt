package com.h2o.store.repositories

import com.google.firebase.firestore.FirebaseFirestore
import com.h2o.store.data.models.AddressData
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    suspend fun getUserAddress(userId: String): AddressData? {
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            val addressMap = userDoc.get("address") as? Map<String, Any>

            if (addressMap != null) {
                AddressData(
                    street = addressMap["street"] as? String ?: "",
                    city = addressMap["city"] as? String ?: "",
                    state = addressMap["state"] as? String ?: "",
                    country = addressMap["country"] as? String ?: "",
                    postalCode = addressMap["postalCode"] as? String ?: "",
                    formattedAddress = addressMap["formattedAddress"] as? String ?: ""
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}