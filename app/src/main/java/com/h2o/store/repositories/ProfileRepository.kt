package com.h2o.store.repositories

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.h2o.store.data.models.AddressData
import com.h2o.store.data.models.LocationData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val whatsapp: String = "",
    val district: String = "",
    val city: String = "",
    // Added location and address fields
    val location: LocationData? = null,
    val address: AddressData? = null
    // Add any other user fields you need
)

class ProfileRepository {
    private val firebaseAuth = Firebase.auth
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "UserRepository"

    // Check if user is authenticated
    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    // Sign out user
    fun signOut() {
        firebaseAuth.signOut()
    }

    // Force token refresh
    suspend fun refreshUserToken(): Boolean {
        val user = firebaseAuth.currentUser ?: return false
        return try {
            user.getIdToken(true).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh token: ${e.message}")
            false
        }
    }

    // Get current logged in user ID with debug logging
    private fun getCurrentUserId(): String? {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "getCurrentUserId: No user is currently signed in")
            return null
        }

        Log.d(TAG, "getCurrentUserId: User is signed in with ID: ${currentUser.uid}")
        return currentUser.uid
    }

    // Get user data as a Flow with token refresh
    fun getUserData(): Flow<Result<User>> = flow {
        try {
            // First check if the user is logged in at all
            val currentUser = firebaseAuth.currentUser

            if (currentUser == null) {
                Log.e(TAG, "getUserData: No user is currently signed in")
                emit(Result.failure(Exception("User not logged in")))
                return@flow
            }

            try {
                // Force token refresh before accessing Firestore
                val tokenResult = currentUser.getIdToken(true).await()
                Log.d(TAG, "Token refreshed successfully: ${tokenResult.token?.take(10)}...")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh token: ${e.message}")
                // If token refresh fails with an auth error, the user might need to re-authenticate
                if (e.message?.contains("auth", ignoreCase = true) == true) {
                    emit(Result.failure(Exception("Authentication expired. Please login again.")))
                    return@flow
                }
                // Otherwise continue anyway, but log the error
            }

            val userId = currentUser.uid
            Log.d(TAG, "Fetching user data for ID: $userId")

            // Add a test to verify collection access
            try {
                val testDoc = firestore.collection("users").limit(1).get().await()
                Log.d(TAG, "Test access to users collection successful: ${testDoc.size()} documents")
            } catch (e: Exception) {
                Log.e(TAG, "Test access to users collection failed: ${e.message}")
                if (e.message?.contains("permission") == true) {
                    emit(Result.failure(Exception("You don't have permission to access your profile")))
                    return@flow
                }
                // Continue anyway, but log the error
            }

            val snapshot = firestore.collection("users").document(userId).get().await()

            if (snapshot.exists()) {
                Log.d(TAG, "User document found with data: ${snapshot.data}")

                // Extract user data manually to handle potential missing fields
                val name = snapshot.getString("name") ?: ""
                val email = snapshot.getString("email") ?: currentUser.email ?: ""
                val phone = snapshot.getString("phone") ?: ""
                val whatsapp = snapshot.getString("whatsapp") ?: ""
                val district = snapshot.getString("district") ?: ""
                val city = snapshot.getString("city") ?: ""

                // Extract location data if available
                var location: LocationData? = null
                val locationMap = snapshot.get("location") as? Map<String, Any>
                if (locationMap != null) {
                    val latitude = locationMap["latitude"] as? Double
                    val longitude = locationMap["longitude"] as? Double
                    if (latitude != null && longitude != null) {
                        location = LocationData(latitude, longitude)
                    }
                }

                // Extract address data if available
                var address: AddressData? = null
                val addressMap = snapshot.get("address") as? Map<String, Any>
                if (addressMap != null) {
                    address = AddressData(
                        street = addressMap["street"] as? String ?: "",
                        city = addressMap["city"] as? String ?: "",
                        state = addressMap["state"] as? String ?: "",
                        country = addressMap["country"] as? String ?: "",
                        postalCode = addressMap["postalCode"] as? String ?: "",
                        formattedAddress = addressMap["formattedAddress"] as? String ?: ""
                    )
                }

                val user = User(
                    id = userId,
                    name = name,
                    email = email,
                    phone = phone,
                    whatsapp = whatsapp,
                    district = district,
                    city = city,
                    location = location,
                    address = address
                )

                emit(Result.success(user))
            } else {
                Log.e(TAG, "User document not found for ID: $userId")
                // Create a default user document if it doesn't exist
                val defaultUser = User(
                    id = userId,
                    name = currentUser.displayName ?: "",
                    email = currentUser.email ?: "",
                    phone = "",
                    whatsapp = "",
                    district = "",
                    city = ""
                )

                try {
                    // Try to create a user document for first-time users
                    firestore.collection("users").document(userId)
                        .set(defaultUser).await()
                    emit(Result.success(defaultUser))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create default user document: ${e.message}")
                    emit(Result.failure(Exception("Could not create user profile")))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user data: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    // Update user data with location and address
    suspend fun updateUserData(
        name: String? = null,
        phone: String? = null,
        whatsapp: String? = null,
        district: String? = null,
        city: String? = null,
        location: LocationData? = null,
        address: AddressData? = null,
        onResult: (Boolean, String?) -> Unit
    ) {
        try {
            val currentUser = firebaseAuth.currentUser

            if (currentUser == null) {
                Log.e(TAG, "updateUserData: No user is currently signed in")
                onResult(false, "User not logged in")
                return
            }

            try {
                // Force token refresh before updating
                currentUser.getIdToken(true).await()
                Log.d(TAG, "Token refreshed successfully for update operation")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh token for update: ${e.message}")
                if (e.message?.contains("auth", ignoreCase = true) == true) {
                    onResult(false, "Authentication expired. Please login again.")
                    return
                }
                // Continue anyway, but log the error
            }

            val userId = currentUser.uid

            // Create update map with only non-null fields
            val updateData = HashMap<String, Any>()
            name?.let { updateData["name"] = it }
            phone?.let { updateData["phone"] = it }
            whatsapp?.let { updateData["whatsapp"] = it }
            district?.let { updateData["district"] = it }
            city?.let { updateData["city"] = it }

            // Add location data if provided
            location?.let {
                val locationMap = mapOf(
                    "latitude" to it.latitude,
                    "longitude" to it.longitude
                )
                updateData["location"] = locationMap
            }

            // Add address data if provided
            address?.let {
                val addressMap = mapOf(
                    "street" to it.street,
                    "city" to it.city,
                    "state" to it.state,
                    "country" to it.country,
                    "postalCode" to it.postalCode,
                    "formattedAddress" to it.formattedAddress
                )
                updateData["address"] = addressMap
            }

            if (updateData.isEmpty()) {
                Log.d(TAG, "No data to update")
                onResult(true, "No changes to save")
                return
            }

            Log.d(TAG, "Updating user data for ID: $userId with data: $updateData")
            firestore.collection("users").document(userId)
                .update(updateData)
                .addOnSuccessListener {
                    Log.d(TAG, "User data updated successfully")
                    onResult(true, null)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to update user data: ${e.message}")
                    onResult(false, "Failed to update profile: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception updating user data: ${e.message}")
            onResult(false, "Error: ${e.message}")
        }
    }
}