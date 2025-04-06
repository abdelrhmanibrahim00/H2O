package com.h2o.store.repositories

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.h2o.store.data.User.UserData
import com.h2o.store.data.models.AddressData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

    // Get all users as a Flow (for admin purposes)
    fun getAllUsersFlow(): Flow<List<UserData>> = callbackFlow {
        Log.d(TAG, "Starting real-time listener for ALL users")

        // Query all users and sort by name
        val query = usersCollection
            .orderBy("name", Query.Direction.ASCENDING)

        // Set up the snapshot listener
        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening for user updates: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    val users = snapshot.documents.mapNotNull { document ->
                        try {
                            val userId = document.id
                            val name = document.getString("name") ?: ""
                            val email = document.getString("email") ?: ""
                            val phone = document.getString("phone") ?: ""
                            val whatsapp = document.getString("whatsapp")
                            val city = document.getString("city") ?: ""
                            val district = document.getString("district") ?: ""
                            val role = document.getString("Role") ?: "user"

                            // Extract address data
                            val addressMap = document.get("address") as? Map<String, Any>
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
                                district = district,
                                role = role
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error converting document ${document.id}: ${e.message}")
                            null
                        }
                    }

                    Log.d(TAG, "Real-time update: ${users.size} total users")
                    trySend(users)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing snapshot: ${e.message}")
                }
            }
        }

        // Clean up the listener when the flow is cancelled
        awaitClose {
            Log.d(TAG, "Removing all users listener")
            listenerRegistration.remove()
        }
    }
    // Add this method to UserRepository.kt
    fun getDeliveryPersonnelFlow(): Flow<List<UserData>> = callbackFlow {
        Log.d(TAG, "Starting real-time listener for delivery personnel")

        // Query users with Role = delivery
        val query = usersCollection
            .whereEqualTo("Role", "delivery")
            .orderBy("name", Query.Direction.ASCENDING)

        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening for delivery personnel: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    val deliveryPersonnel = snapshot.documents.mapNotNull { document ->
                        try {
                            val userId = document.id
                            val name = document.getString("name") ?: ""
                            // You can add other fields if needed

                            UserData(
                                id = userId,
                                name = name,
                                email = document.getString("email") ?: "",
                                phone = document.getString("phone") ?: "",
                                role = "delivery"
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error converting document ${document.id}: ${e.message}")
                            null
                        }
                    }

                    Log.d(TAG, "Real-time update: ${deliveryPersonnel.size} delivery personnel")
                    trySend(deliveryPersonnel)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing snapshot: ${e.message}")
                }
            }
        }

        awaitClose {
            Log.d(TAG, "Removing delivery personnel listener")
            listenerRegistration.remove()
        }
    }

    // Update user information
    suspend fun updateUser(userData: UserData): Boolean {
        return try {
            Log.d(TAG, "Updating user: ${userData.id}")

            // Create a map of basic fields to update
            val updates = mapOf(
                "name" to userData.name,
                "phone" to userData.phone,
                "email" to userData.email,
                "whatsapp" to userData.whatsapp,
                "city" to userData.city,
                "district" to userData.district,
                "Role" to userData.role
            )

            // Handle address separately since it's a nested object
            val addressUpdates = userData.address?.let {
                mapOf(
                    "address.street" to it.street,
                    "address.city" to it.city,
                    "address.state" to it.state,
                    "address.country" to it.country,
                    "address.postalCode" to it.postalCode,
                    "address.formattedAddress" to it.formattedAddress
                )
            } ?: mapOf()

            // Combine updates
            val allUpdates = updates + addressUpdates

            // Perform the update
            usersCollection.document(userData.id)
                .update(allUpdates)
                .await()

            // Log success
            Log.d(TAG, "User successfully updated: ${userData.id}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user: ${e.message}")
            Log.e(TAG, "Stack trace: ", e)
            false
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