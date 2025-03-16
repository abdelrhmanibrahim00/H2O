package com.h2o.store.repositories

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.h2o.store.data.User.UserData
import com.h2o.store.data.models.AddressData
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")
    private val TAG = "UserRepository"

    suspend fun getUserData(userId: String): UserData {
        return try {
            Log.d(TAG, "Fetching user data for user ID: $userId")
            val documentSnapshot = usersCollection.document(userId).get().await()

            if (!documentSnapshot.exists()) {
                Log.e(TAG, "User document does not exist for ID: $userId")
                throw Exception("User not found")
            }

            // Extract basic user data
            val name = documentSnapshot.getString("name") ?: ""
            val email = documentSnapshot.getString("email") ?: ""
            val phone = documentSnapshot.getString("phone") ?: ""
            val whatsapp = documentSnapshot.getString("whatsapp")
            val city = documentSnapshot.getString("city") ?: ""
            val district = documentSnapshot.getString("district") ?: ""

            // Extract address data
            val addressMap = documentSnapshot.get("address") as? Map<String, Any>
            val address = if (addressMap != null) {
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

            UserData(
                id = userId,
                name = name,
                email = email,
                phone = phone,
                whatsapp = whatsapp,
                address = address,
                city = city,
                district = district
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user data: ${e.message}")
            throw e
        }
    }

    // For backward compatibility
    suspend fun getUserAddress(userId: String): AddressData? {
        return try {
            val userData = getUserData(userId)
            userData.address
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user address: ${e.message}")
            throw e
        }
    }
}