package com.h2o.store.repositories

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.h2o.store.data.models.AddressData
import com.h2o.store.data.models.LocationData

class AuthRepository {
    private val firebaseAuth = Firebase.auth
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "AuthRepository" // Add this at the top of your class


    // Add a data class to represent the login result with role information
    data class LoginResult(
        val success: Boolean,
        val errorMessage: String? = null,
        val role: String? = null
    )

    fun loginUser(email: String, password: String, onResult: (LoginResult) -> Unit) {
        Log.d(TAG, "Attempting login with email: $email")

        // First check if a user is already logged in
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null && currentUser.email == email) {
            // User is already logged in, just check their role
            fetchUserRole(currentUser.uid) { exists, role ->
                onResult(LoginResult(true, null, role ?: "user"))
            }
            return
        }

        // Proceed with normal login flow if not already logged in
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user == null) {
                    Log.e(TAG, "Login succeeded but user is null")
                    onResult(LoginResult(false, "Authentication error, please try again"))
                    return@addOnSuccessListener
                }

                // Check if email verification is required
                if (!user.isEmailVerified) {
                    Log.d(TAG, "User email is not verified")

                    // Send a new verification email
                    user.sendEmailVerification()
                        .addOnSuccessListener {
                            Log.d(TAG, "New verification email sent")
                        }

                    // Sign out the user
                    firebaseAuth.signOut()
                    onResult(LoginResult(false, "Please verify your email before logging in. A new verification email has been sent."))
                    return@addOnSuccessListener
                }

                // Check user document and fetch role
                fetchUserRole(user.uid) { exists, role ->
                    if (!exists) {
                        // Create a basic user document if it doesn't exist
                        createBasicUserDocument(user.uid, user.email ?: "")
                        // Default role is "user" for newly created documents
                        onResult(LoginResult(true, null, "user"))
                    } else {
                        // Return the role with the success result
                        onResult(LoginResult(true, null, role))
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Login failed: ${e.message}")

                // Provide more helpful error messages based on the exception
                val errorMessage = when {
                    e.message?.contains("password") == true -> "Incorrect password. Please try again."
                    e.message?.contains("no user record") == true -> "No account found with this email."
                    e.message?.contains("blocked") == true -> "Account temporarily blocked. Try again later or reset your password."
                    else -> "Login failed: ${e.message}"
                }

                onResult(LoginResult(false, errorMessage))
            }
    }

    fun resetPassword(email: String, onResult: (Boolean, String?) -> Unit) {
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                onResult(true, "Password reset email sent. Please check your inbox.")
            }
            .addOnFailureListener { e ->
                onResult(false, "Failed to send reset email: ${e.message}")
            }
    }

    fun signUpUser(
        name: String,
        phone: String,
        email: String,
        password: String,
        whatsapp: String,
        district: String,
        locationData: LocationData,
        addressData: AddressData,
        accountType: String, // Add this parameter
        onResult: (Boolean, String?) -> Unit
    ) {
        // Log authentication state before starting
        if (firebaseAuth.currentUser == null) {
            Log.d(TAG, "No user authenticated when starting sign up")
        }

        // Create the user in Firebase Auth
        Log.d(TAG, "Attempting to create user with email: $email")
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                Log.d(TAG, "User creation successful, uid: ${user?.uid}")

                // Check if user is valid
                if (user == null) {
                    Log.e(TAG, "Auth succeeded but user is null")
                    onResult(false, "Authentication succeeded but user is null")
                    return@addOnSuccessListener
                }

                // Send verification email
                Log.d(TAG, "Sending verification email")
                user.sendEmailVerification()
                    .addOnSuccessListener {
                        Log.d(TAG, "Verification email sent to $email")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Email sending failed: ${e.message}")
                    }

                // Get fresh ID token before writing to Firestore
                Log.d(TAG, "Getting fresh ID token")
                user.getIdToken(true)
                    .addOnSuccessListener { tokenResult ->
                        Log.d(TAG, "Got fresh ID token, proceeding to Firestore write")

                        // Create structured location and address data
                        val locationMap = hashMapOf(
                            "latitude" to locationData.latitude,
                            "longitude" to locationData.longitude
                        )

                        val addressMap = hashMapOf(
                            "street" to addressData.street,
                            "city" to addressData.city.ifEmpty { "Alexandria" }, // Default to Alexandria if empty
                            "state" to addressData.state,
                            "country" to addressData.country,
                            "postalCode" to addressData.postalCode,
                            "formattedAddress" to addressData.formattedAddress
                        )

                        // Prepare user data with structured address
                        val userData = hashMapOf(
                            "name" to name,
                            "phone" to phone,
                            "email" to email,
                            "whatsapp" to whatsapp,
                            "city" to "Alexandria",
                            "district" to district,
                            "location" to locationMap,        // Geolocation data
                            "address" to addressMap,          // Structured address data
                            "created_at" to FieldValue.serverTimestamp(),
                            "Role" to "user" ,
                            "accountType" to accountType // Add this line

                        )

                        Log.d(TAG, "Writing user data to Firestore, uid: ${user.uid}")

                        // Write to Firestore
                        firestore.collection("users").document(user.uid)
                            .set(userData)
                            .addOnSuccessListener {
                                Log.d(TAG, "Firestore write successful")
                                onResult(true, null)
                            }
                            .addOnFailureListener { e ->
                                if (e is FirebaseFirestoreException) {
                                    Log.e(TAG, "Firestore error code: ${e.code}")
                                }
                                Log.e(TAG, "Firestore write failed: ${e.message}")
                                Log.e(TAG, "Full exception: $e")

                                // Try a test write to a different collection
                                val testData = hashMapOf("test" to "value")
                                Log.d(TAG, "Attempting test write to different collection")
                                firestore.collection("test_collection").document(user.uid)
                                    .set(testData)
                                    .addOnSuccessListener {
                                        Log.d(TAG, "Test write successful, main write still failed")
                                    }
                                    .addOnFailureListener { testError ->
                                        Log.e(TAG, "Test write also failed: ${testError.message}")
                                    }

                                onResult(false, "Firestore write failed: ${e.message}")
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to get fresh ID token: ${e.message}")
                        onResult(false, "Failed to get authentication token: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Auth creation failed: ${e.message}")
                onResult(false, "Authentication failed: ${e.message}")
            }
    }

    // Enhance the logout method in AuthRepository.kt:
    fun logoutUser(onComplete: () -> Unit) {
        try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                Log.d(TAG, "No user to log out")
                onComplete()
                return
            }

            Log.d(TAG, "Logging out user: ${currentUser.email}")

            // Clear any cached data first
            // Add memory cleanup here if needed

            firebaseAuth.signOut()
            Log.d(TAG, "User logged out successfully")

            // Ensure we wait a moment before completing to allow Firebase to process
            // the signout request completely
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                onComplete()
            }, 300) // Small delay to ensure Firebase completes the process
        } catch (e: Exception) {
            Log.e(TAG, "Error during logout: ${e.message}")
            onComplete()
        }
    }

    // New helper method to check if a user document exists
    private fun checkUserDocument(userId: String, onResult: (Boolean) -> Unit) {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                Log.d(TAG, "User document check - exists: ${document.exists()}")
                onResult(document.exists())
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking user document: ${e.message}")
                // If we can't check, assume it exists to be safe
                onResult(true)
            }
    }

    // New method to fetch the user's role
    private fun fetchUserRole(userId: String, onResult: (Boolean, String?) -> Unit) {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Extract the role from the document, defaulting to "user" if not found
                    val role = document.getString("Role") ?: "user"
                    Log.d(TAG, "User document exists, role: $role")
                    onResult(true, role)
                } else {
                    Log.d(TAG, "User document does not exist")
                    onResult(false, null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking user document: ${e.message}")
                // If we can't check, assume it's a user to be safe
                onResult(false, "user")
            }
    }

    // New helper method to create a basic user document if it doesn't exist
    private fun createBasicUserDocument(userId: String, email: String) {
        val userData = hashMapOf(
            "email" to email,
            "name" to "",
            "phone" to "",
            "whatsapp" to "",
            "city" to "",
            "district" to "",
            "created_at" to FieldValue.serverTimestamp(),
            "Role" to "user" , // Default role is "user"
            "accountType" to "Home"  // Add default account type

        )

        Log.d(TAG, "Creating basic user document for ID: $userId")
        firestore.collection("users").document(userId)
            .set(userData)
            .addOnSuccessListener {
                Log.d(TAG, "Created basic user document for user: $userId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to create user document: ${e.message}")
            }
    }
}