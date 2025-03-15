package com.h2o.store.repositories


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

    suspend fun placeOrder(
        userId: String,
        cartItems: List<CartItem>,
        subtotal: Double,
        paymentMethod: String,
        address: AddressData
    ): Boolean {
        return try {
            // Convert cart items to order items
            val orderItems = cartItems.map { cartItem ->
                OrderItem(
                    productId = cartItem.productId,
                    productName = cartItem.productName,
                    productImage = cartItem.productImage,
                    quantity = cartItem.quantity,
                    price = (if (cartItem.priceAfterDiscount > 0) cartItem.priceAfterDiscount else cartItem.productPrice).toDouble()
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

            // Save order to Firestore using suspendCoroutine and let Firebase generate a unique ID
            suspendCoroutine<Boolean> { continuation ->
                // Let Firebase generate a unique document ID
                val newDocRef = ordersCollection.document()

                // Create a new order object with the Firebase-generated ID
                val orderWithId = order.copy(orderId = newDocRef.id)

                // Save the order with the Firebase-generated ID
                newDocRef.set(orderWithId)
                    .addOnSuccessListener {
                        continuation.resume(true)
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }
            }
        } catch (e: Exception) {
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
}