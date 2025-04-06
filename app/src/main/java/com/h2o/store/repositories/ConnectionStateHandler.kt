package com.h2o.store.repositories

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// New class in a new file: ConnectionStateHandler.kt
class ConnectionStateHandler(private val context: Context) {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Unknown)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private var firestoreListener: ListenerRegistration? = null

    fun startMonitoring() {
        // Monitor local device connectivity
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Network is available, but we need to check Firestore connectivity
                checkFirestoreConnectivity()
            }

            override fun onLost(network: Network) {
                _connectionState.value = ConnectionState.Offline
            }
        }

        // Register the callback
        val networkRequest = NetworkRequest.Builder().build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        // Initial check
        checkFirestoreConnectivity()
    }

    private fun checkFirestoreConnectivity() {
        // Use Firestore's offline capabilities to determine connection state
        firestoreListener?.remove()

        firestoreListener = FirebaseFirestore.getInstance()
            .document(".info/connected")  // Change to document() instead of collection()
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("ConnectionState", "Firestore connection check failed: ${error.message}")
                    _connectionState.value = ConnectionState.Error(error)
                    return@addSnapshotListener
                }

                // The connected field is a boolean value in the document
                val connected = snapshot?.exists() == true && snapshot.getBoolean("connected") == true
                _connectionState.value = if (connected) {
                    ConnectionState.Online
                } else {
                    ConnectionState.Offline
                }
            }
    }

    fun stopMonitoring() {
        firestoreListener?.remove()
    }

    sealed class ConnectionState {
        object Unknown : ConnectionState()
        object Online : ConnectionState()
        object Offline : ConnectionState()
        data class Error(val exception: Exception) : ConnectionState()
    }
}