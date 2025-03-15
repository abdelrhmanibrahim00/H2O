package com.h2o.store.repositories

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.h2o.store.data.Orders.Order
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class OrderRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val ordersCollection = firestore.collection("orders")
    private val TAG = "OrderRepository"

    // Get orders as a real-time Flow
    fun getOrdersFlow(userId: String): Flow<List<Order>> = callbackFlow {
        Log.d(TAG, "Starting real-time orders listener for user: $userId")

        // Query orders by userId and sort by orderDate descending
        val query = ordersCollection
            .whereEqualTo("userId", userId)
            .orderBy("orderDate", Query.Direction.DESCENDING)

        // Set up the snapshot listener
        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening for order updates: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    val orders = snapshot.documents.mapNotNull { document ->
                        try {
                            document.toObject(Order::class.java)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error converting document ${document.id}: ${e.message}")
                            null
                        }
                    }

                    Log.d(TAG, "Real-time update: ${orders.size} orders for user $userId")
                    trySend(orders)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing snapshot: ${e.message}")
                }
            }
        }

        // Clean up the listener when the flow is cancelled
        awaitClose {
            Log.d(TAG, "Removing orders listener")
            listenerRegistration.remove()
        }
    }

    // Keep the one-time fetch for order details if needed
    suspend fun getOrderDetails(orderId: String): Order? {
        return try {
            Log.d(TAG, "Fetching details for order: $orderId")
            val document = ordersCollection.document(orderId).get().await()
            document.toObject(Order::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching order details: ${e.message}")
            throw e
        }
    }

    // This can also be converted to a real-time approach if needed
    suspend fun cancelOrder(orderId: String): Boolean {
        return try {
            Log.d(TAG, "Cancelling order: $orderId")
            ordersCollection.document(orderId)
                .update("status", "Cancelled")
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling order: ${e.message}")
            false
        }
    }
}