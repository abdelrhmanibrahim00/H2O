package com.h2o.store.repositories.Admin

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

    // Get orders as a real-time Flow for a specific user
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
                            document.toObject(Order::class.java)?.copy(orderId = document.id)
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

    // Get ALL orders (for admin and delivery personnel)
    fun getAllOrdersFlow(): Flow<List<Order>> = callbackFlow {
        Log.d(TAG, "Starting real-time listener for ALL orders")

        // Query all orders and sort by orderDate descending
        val query = ordersCollection
            .orderBy("orderDate", Query.Direction.DESCENDING)

        // Set up the snapshot listener
        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening for all order updates: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    val orders = snapshot.documents.mapNotNull { document ->
                        try {
                            document.toObject(Order::class.java)?.copy(orderId = document.id)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error converting document ${document.id}: ${e.message}")
                            null
                        }
                    }

                    Log.d(TAG, "Real-time update: ${orders.size} total orders")
                    trySend(orders)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing snapshot: ${e.message}")
                }
            }
        }

        // Clean up the listener when the flow is cancelled
        awaitClose {
            Log.d(TAG, "Removing all orders listener")
            listenerRegistration.remove()
        }
    }

    // Get orders by status (for filtering in the delivery dashboard)
    fun getOrdersByStatusFlow(status: String): Flow<List<Order>> = callbackFlow {
        Log.d(TAG, "Starting real-time listener for orders with status: $status")

        val query = ordersCollection
            .whereEqualTo("status", status)
            .orderBy("orderDate", Query.Direction.DESCENDING)

        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening for status-filtered order updates: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    val orders = snapshot.documents.mapNotNull { document ->
                        try {
                            document.toObject(Order::class.java)?.copy(orderId = document.id)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error converting document ${document.id}: ${e.message}")
                            null
                        }
                    }

                    Log.d(TAG, "Real-time update: ${orders.size} orders with status $status")
                    trySend(orders)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing snapshot: ${e.message}")
                }
            }
        }

        awaitClose {
            Log.d(TAG, "Removing status-filtered orders listener")
            listenerRegistration.remove()
        }
    }

    // Get a single order by ID
    suspend fun getOrderDetails(orderId: String): Order? {
        return try {
            Log.d(TAG, "Fetching details for order: $orderId")
            val document = ordersCollection.document(orderId).get().await()
            document.toObject(Order::class.java)?.copy(orderId = document.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching order details: ${e.message}")
            throw e
        }
    }

    // Cancel an order
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

    // Update order status (for delivery personnel)
    suspend fun updateOrderStatus(orderId: String, newStatus: String): Boolean {
        return try {
            Log.d(TAG, "Updating order $orderId status to: $newStatus")
            ordersCollection.document(orderId)
                .update("status", newStatus)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating order status: ${e.message}")
            false
        }
    }

    // Assign delivery personnel to an order
    suspend fun assignDeliveryPersonnel(orderId: String, deliveryPersonnelId: String): Boolean {
        return try {
            Log.d(TAG, "Assigning delivery personnel $deliveryPersonnelId to order $orderId")
            ordersCollection.document(orderId)
                .update(
                    mapOf(
                        "deliveryPersonnelId" to deliveryPersonnelId,
                        "status" to "Assigned" // Optional: update status automatically
                    )
                )
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error assigning delivery personnel: ${e.message}")
            false
        }
    }
}