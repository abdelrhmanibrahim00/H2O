package com.h2o.store.repositories.Admin

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.h2o.store.data.Orders.Order
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

class OrderRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val ordersCollection = firestore.collection("orders")
    private val TAG = "OrderRepository"

    // NEW METHOD: Get orders by date range
    fun getOrdersByDateRangeFlow(startDate: Date, endDate: Date): Flow<List<Order>> = callbackFlow {
        Log.d(TAG, "Starting real-time listener for orders between dates: $startDate and $endDate")

        // Convert Java Date to Firestore Timestamp
        val startTimestamp = Timestamp(startDate)
        val endTimestamp = Timestamp(endDate)

        // Query orders between dates and sort by orderDate descending
        val query = ordersCollection
            .whereGreaterThanOrEqualTo("orderDate", startTimestamp)
            .whereLessThanOrEqualTo("orderDate", endTimestamp)
            .orderBy("orderDate", Query.Direction.DESCENDING)

        // Set up the snapshot listener
        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening for date-filtered order updates: ${error.message}")
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

                    Log.d(
                        TAG,
                        "Real-time update: ${orders.size} orders between $startDate and $endDate"
                    )
                    trySend(orders)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing snapshot: ${e.message}")
                }
            }
        }

        // Clean up the listener when the flow is cancelled
        awaitClose {
            Log.d(TAG, "Removing date-filtered orders listener")
            listenerRegistration.remove()
        }
    }

    // EXISTING METHOD: Get orders as a real-time Flow for a specific user
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

    // EXISTING METHOD: Update order with delivery person
    suspend fun updateOrderWithDeliveryPerson(
        orderId: String,
        status: String,
        deliveryPersonId: String,
        deliveryPersonName: String
    ): Boolean {
        return try {
            val orderRef = ordersCollection.document(orderId)

            // Update both status and delivery person
            orderRef.update(
                mapOf(
                    "status" to status,
                    "deliveryPersonId" to deliveryPersonId,
                    "deliveryPersonName" to deliveryPersonName,
                    "lastUpdated" to com.google.firebase.Timestamp.now()
                )
            ).await()

            Log.d(
                TAG,
                "Order $orderId updated with status $status and delivery person $deliveryPersonId"
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating order with delivery person: ${e.message}")
            false
        }
    }

    // EXISTING METHOD: Get ALL orders
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

    // EXISTING METHOD: Update order information
    suspend fun updateOrder(order: Order): Boolean {
        return try {
            Log.d(TAG, "Updating order: ${order.orderId}")

            // Create a map of fields to update
            val updates = mapOf(
                "status" to order.status,
                "paymentMethod" to order.paymentMethod,
                "deliveryFee" to order.deliveryFee,
                "subtotal" to order.subtotal,
                "total" to order.total,
                "estimatedDelivery" to order.estimatedDelivery
            )

            // Handle deliveryAddress separately since it's a nested object
            val addressUpdates = order.deliveryAddress?.let {
                mapOf(
                    "deliveryAddress.street" to it.street,
                    "deliveryAddress.city" to it.city,
                    "deliveryAddress.state" to it.state,
                    "deliveryAddress.country" to it.country,
                    "deliveryAddress.postalCode" to it.postalCode,
                    "deliveryAddress.formattedAddress" to it.formattedAddress
                )
            } ?: mapOf()

            // Combine all updates
            val allUpdates = updates + addressUpdates

            // Perform the update
            ordersCollection.document(order.orderId)
                .update(allUpdates)
                .await()

            // Log success
            Log.d(TAG, "Order successfully updated: ${order.orderId}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating order: ${e.message}")
            Log.e(TAG, "Stack trace: ", e)
            false
        }
    }

    // EXISTING METHOD: Get orders by status
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

    // EXISTING METHOD: Get a single order by ID
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

    // EXISTING METHOD: Cancel an order
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

    // EXISTING METHOD: Update order status
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

    // EXISTING METHOD: Assign delivery personnel to an order
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

    // EXISTING METHOD: Get orders filtered by both delivery person and status
    fun getOrdersByDeliveryPersonAndStatusFlow(
        deliveryPersonId: String,
        status: String
    ): Flow<List<Order>> = callbackFlow {
        Log.d(
            TAG,
            "Starting real-time listener for orders: delivery person=$deliveryPersonId, status=$status"
        )

        // Query orders by both deliveryPersonId and status
        val query = ordersCollection
            .whereEqualTo("deliveryPersonId", deliveryPersonId)
            .whereEqualTo("status", status)
            .orderBy("orderDate", Query.Direction.DESCENDING)

        // Set up the snapshot listener
        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening for filtered orders: ${error.message}")
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

                    Log.d(
                        TAG,
                        "Real-time update: ${orders.size} orders for delivery person $deliveryPersonId with status $status"
                    )
                    trySend(orders)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing snapshot: ${e.message}")
                }
            }
        }

        // Clean up the listener when the flow is cancelled
        awaitClose {
            Log.d(TAG, "Removing filtered orders listener")
            listenerRegistration.remove()
        }
    }

    // EXISTING METHOD: Get orders for a specific delivery person
    fun getOrdersByDeliveryPersonFlow(deliveryPersonId: String): Flow<List<Order>> = callbackFlow {
        Log.d(
            TAG,
            "Starting real-time listener for orders assigned to delivery person: $deliveryPersonId"
        )

        // Query orders by deliveryPersonId and sort by orderDate descending
        val query = ordersCollection
            .whereEqualTo("deliveryPersonId", deliveryPersonId)
            .orderBy("orderDate", Query.Direction.DESCENDING)

        // Set up the snapshot listener
        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening for delivery person orders: ${error.message}")
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

                    Log.d(
                        TAG,
                        "Real-time update: ${orders.size} orders for delivery person $deliveryPersonId"
                    )
                    trySend(orders)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing snapshot: ${e.message}")
                }
            }
        }

        // Clean up the listener when the flow is cancelled
        awaitClose {
            Log.d(TAG, "Removing delivery person orders listener")
            listenerRegistration.remove()
        }
    }

    // Add these new methods to your OrderRepository class


    // Get all pending and processing orders (always fetch all of these)
    fun getActiveOrdersFlow(): Flow<List<Order>> = callbackFlow {
        Log.d(TAG, "Starting real-time listener for active orders (Pending and Processing)")

        val activeStatuses = listOf("Pending", "Processing")

        val query = ordersCollection
            .whereIn("status", activeStatuses)
            .orderBy("orderDate", Query.Direction.DESCENDING)

        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening for active orders: ${error.message}")
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

                    Log.d(TAG, "Real-time update: ${orders.size} active orders")
                    trySend(orders)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing snapshot: ${e.message}")
                }
            }
        }

        awaitClose {
            Log.d(TAG, "Removing active orders listener")
            listenerRegistration.remove()
        }
    }

    // Get completed orders (delivered or cancelled) with pagination
    fun getCompletedOrdersPaginatedFlow(
        status: String,
        limit: Long = 20,
        lastDocumentSnapshot: DocumentSnapshot? = null
    ): Flow<PaginatedResult<Order>> = callbackFlow {
        Log.d(TAG, "Starting paginated query for orders with status: $status, limit: $limit")

        var query = ordersCollection
            .whereEqualTo("status", status)
            .orderBy("orderDate", Query.Direction.DESCENDING)
            .limit(limit)

        // Apply pagination if we have a last document
        if (lastDocumentSnapshot != null) {
            query = query.startAfter(lastDocumentSnapshot)
        }

        try {
            val querySnapshot = query.get().await()

            val orders = querySnapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(Order::class.java)?.copy(orderId = document.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting document ${document.id}: ${e.message}")
                    null
                }
            }

            // Determine if there are more results available
            val hasMore = orders.size >= limit

            // Get the last document for pagination
            val lastDocument = if (orders.isNotEmpty()) querySnapshot.documents.last() else null

            Log.d(TAG, "Paginated query returned ${orders.size} $status orders")

            // Send the paginated result
            trySend(PaginatedResult(orders, hasMore, lastDocument))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching paginated orders: ${e.message}")
            trySend(PaginatedResult(emptyList(), false, null))
        }

        awaitClose { }
    }

    // Get recent completed orders (for initial load)
    fun getRecentCompletedOrdersFlow(
        status: String,
        daysAgo: Int = 30,
        limit: Long = 20
    ): Flow<List<Order>> = callbackFlow {
        Log.d(TAG, "Fetching recent $status orders from last $daysAgo days")

        // Calculate date threshold (e.g., 30 days ago)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        val dateThreshold = Timestamp(calendar.time)

        val query = ordersCollection
            .whereEqualTo("status", status)
            .whereGreaterThanOrEqualTo("orderDate", dateThreshold)
            .orderBy("orderDate", Query.Direction.DESCENDING)
            .limit(limit)

        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening for recent $status orders: ${error.message}")
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

                    Log.d(TAG, "Real-time update: ${orders.size} recent $status orders")
                    trySend(orders)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing snapshot: ${e.message}")
                }
            }
        }

        awaitClose {
            Log.d(TAG, "Removing recent $status orders listener")
            listenerRegistration.remove()
        }
    }

    // Main pagination method to be used across all order queries
    // Main pagination method to be used across all order queries
    // Modify this method to handle the missing indexes
    fun getPaginatedOrdersFlow(
        status: String? = null,
        userId: String? = null,
        deliveryPersonId: String? = null,
        startDate: Date? = null,
        endDate: Date? = null,
        limit: Long = 20,
        lastDocumentSnapshot: DocumentSnapshot? = null
    ): Flow<PaginatedResult<Order>> = callbackFlow {
        Log.d(TAG, "Starting paginated query with parameters: status=$status, userId=$userId, limit=$limit")

        try {
            // Start with the base collection
            var baseQuery: Query = ordersCollection

            // Apply filters conditionally
            if (status != null) {
                baseQuery = baseQuery.whereEqualTo("status", status)
            }

            if (userId != null) {
                baseQuery = baseQuery.whereEqualTo("userId", userId)
            }

            if (deliveryPersonId != null) {
                baseQuery = baseQuery.whereEqualTo("deliveryPersonId", deliveryPersonId)
            }

            // Always sort by orderDate descending
            baseQuery = baseQuery.orderBy("orderDate", Query.Direction.DESCENDING)

            // Apply limit
            baseQuery = baseQuery.limit(limit)

            // Apply pagination if lastDocumentSnapshot provided
            if (lastDocumentSnapshot != null) {
                baseQuery = baseQuery.startAfter(lastDocumentSnapshot)
            }

            val querySnapshot = baseQuery.get().await()

            val orders = querySnapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(Order::class.java)?.copy(orderId = document.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting document ${document.id}: ${e.message}")
                    null
                }
            }

            // Determine if there are more results available
            val hasMore = orders.size >= limit

            // Get the last document for pagination
            val lastDocument = if (orders.isNotEmpty()) querySnapshot.documents.last() else null

            Log.d(TAG, "Paginated query returned ${orders.size} orders")

            // Send the paginated result
            trySend(PaginatedResult(orders, hasMore, lastDocument, LoadState.DONE))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching paginated orders: ${e.message}")
            trySend(PaginatedResult(emptyList(), false, null, LoadState.ERROR))
        }

        awaitClose { }
    }



    // Add LoadState enum to the repository
    enum class LoadState {
        LOADING, DONE, ERROR
    }

    // Enhanced PaginatedResult class
    data class PaginatedResult<T>(
        val items: List<T>,
        val hasMore: Boolean,
        val lastDocumentSnapshot: DocumentSnapshot?,
        val loadState: LoadState = LoadState.DONE
    )

    // New method for paginated queries
    fun getOrdersPaginated(
        status: String? = null,
        limit: Long = 20,
        lastDocumentSnapshot: DocumentSnapshot? = null
    ): Flow<PaginatedResult<Order>> = callbackFlow {
        Log.d(TAG, "Starting paginated query: status=$status, limit=$limit")

        try {
            // Start with base query
            var baseQuery: Query = ordersCollection

            // Apply status filter if provided
            if (status != null) {
                baseQuery = baseQuery.whereEqualTo("status", status)
            }

            // Always sort by orderDate descending
            baseQuery = baseQuery.orderBy("orderDate", Query.Direction.DESCENDING)

            // Apply limit
            baseQuery = baseQuery.limit(limit)

            // Apply pagination if we have a last document
            if (lastDocumentSnapshot != null) {
                baseQuery = baseQuery.startAfter(lastDocumentSnapshot)
            }

            // Execute the query (NOT a listener)
            val querySnapshot = baseQuery.get().await()

            val orders = querySnapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(Order::class.java)?.copy(orderId = document.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting document ${document.id}: ${e.message}")
                    null
                }
            }

            // Determine if there are more results
            val hasMore = orders.size >= limit

            // Get the last document for pagination
            val lastDocument = if (orders.isNotEmpty()) querySnapshot.documents.last() else null

            Log.d(TAG, "Paginated query returned ${orders.size} orders")

            // Send the paginated result
            trySend(PaginatedResult(orders, hasMore, lastDocument, LoadState.DONE))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching paginated orders: ${e.message}")
            trySend(PaginatedResult(emptyList(), false, null, LoadState.ERROR))
        }

        awaitClose { }
    }
}