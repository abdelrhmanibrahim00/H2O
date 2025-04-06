package com.h2o.store.repositories

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.h2o.store.data.Cart.CartItem
import com.h2o.store.data.Orders.Order
import com.h2o.store.data.Orders.OrderItem
import com.h2o.store.data.models.AddressData
import kotlinx.coroutines.tasks.await
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class CheckoutRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val ordersCollection = firestore.collection("orders")
    private val TAG = "CheckoutRepository"

    suspend fun placeOrder(
        userId: String,
        cartItems: List<CartItem>,
        subtotal: Double,
        paymentMethod: String,
        address: AddressData,
        userName: String = "",
        userPhone: String = "",
        userEmail: String = ""
    ): Boolean {
        return try {
            Log.d(TAG, "Placing order for user: $userId, items: ${cartItems.size}")

            // Convert cart items to order items
            val orderItems = cartItems.map { cartItem ->
                OrderItem(
                    productId = cartItem.productId,
                    productName = cartItem.name,
                    productDescription = cartItem.description,
                    productImage = cartItem.imageUrl,
                    quantity = cartItem.quantity,
                    price = if (cartItem.onSale) {
                        cartItem.price * (1 - cartItem.discountPercentage / 100)
                    } else {
                        cartItem.price
                    },
                    originalPrice = cartItem.price,
                    discountPercentage = cartItem.discountPercentage,
                    category = cartItem.category,
                    stock = cartItem.stock,
                    brand = cartItem.brand,
                    onSale = cartItem.onSale,
                    featured = cartItem.featured,
                    rating = cartItem.rating
                )
            }

            // Calculate delivery fee and total
            val deliveryFee = 15.0

            // Create new order without the orderId (will be set after Firebase generates it)
            val order = Order(
                orderId = "", // This will be set after Firebase generates the ID
                userId = userId,
                items = orderItems,
                subtotal = subtotal,
                deliveryFee = deliveryFee,
                total = subtotal + deliveryFee,
                status = "Pending",
                paymentMethod = paymentMethod,
                orderDate = Date(),
                estimatedDelivery = calculateEstimatedDelivery(),
                deliveryAddress = address
            )

            // Additional user info to store with the order
            val additionalInfo = mapOf(
                "userName" to userName,
                "userPhone" to userPhone,
                "userEmail" to userEmail
            )

            // Save order to Firestore using suspendCoroutine and let Firebase generate a unique ID
            suspendCoroutine<Boolean> { continuation ->
                // Let Firebase generate a unique document ID
                val newDocRef = ordersCollection.document()

                // Create a new order object with the Firebase-generated ID
                val orderWithId = order.copy(orderId = newDocRef.id)

                // Prepare the data to save, combining the order with additional user info
                val orderData = orderWithId.toMap() + additionalInfo

                // Save the order with the Firebase-generated ID and additional user info
                newDocRef.set(orderData)
                    .addOnSuccessListener {
                        Log.d(TAG, "Order successfully placed with ID: ${newDocRef.id}")
                        continuation.resume(true)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error placing order: ${e.message}")
                        continuation.resumeWithException(e)
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in placeOrder: ${e.message}")
            throw e
        }
    }

    suspend fun getOrdersByUserId(userId: String): List<Order> {
        return try {
            val snapshot = ordersCollection.whereEqualTo("userId", userId).get().await()
            snapshot.documents.mapNotNull { document ->
                document.toObject(Order::class.java)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getOrderById(orderId: String): Order? {
        return try {
            val document = ordersCollection.document(orderId).get().await()
            document.toObject(Order::class.java)
        } catch (e: Exception) {
            throw e
        }
    }

    private fun calculateEstimatedDelivery(): Date {
        val calendar = java.util.Calendar.getInstance()
        calendar.time = Date()
        calendar.add(java.util.Calendar.DAY_OF_MONTH, 3)
        return calendar.time
    }

    // Extension function to convert Order to Map for Firestore
    private fun Order.toMap(): Map<String, Any?> {
        return mapOf(
            "orderId" to orderId,
            "userId" to userId,
            "items" to items,
            "subtotal" to subtotal,
            "deliveryFee" to deliveryFee,
            "total" to total,
            "status" to status,
            "paymentMethod" to paymentMethod,
            "orderDate" to orderDate,
            "estimatedDelivery" to estimatedDelivery,
            "deliveryAddress" to deliveryAddress
        )
    }
}