package com.h2o.store.utils

import com.h2o.store.data.Orders.Order
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

enum class NotificationPriority {
    NORMAL,
    URGENT
}

data class PrioritizedNotification(
    val order: Order,
    val priority: NotificationPriority,
    val reason: String
)

object NotificationHelper {

    fun classifyOrderNotification(order: Order): PrioritizedNotification {
        // Check for cancellation
        if (order.status == "Cancelled") {
            return PrioritizedNotification(
                order = order,
                priority = NotificationPriority.URGENT,
                reason = "Order cancelled"
            )
        }

        // Check for pending too long (> 2 days)
        if (order.status == "Pending") {
            val daysPending = getDaysBetween(order.orderDate, Date())
            if (daysPending > 2) {
                return PrioritizedNotification(
                    order = order,
                    priority = NotificationPriority.URGENT,
                    reason = "Pending for $daysPending days"
                )
            }
        }

        // Check for not delivered after 3 days
        if (order.status != "Delivered" && order.status != "Cancelled") {
            val daysActive = getDaysBetween(order.orderDate, Date())
            if (daysActive > 3) {
                return PrioritizedNotification(
                    order = order,
                    priority = NotificationPriority.URGENT,
                    reason = "Not delivered after $daysActive days"
                )
            }
        }

        // Default to normal priority
        return PrioritizedNotification(
            order = order,
            priority = NotificationPriority.NORMAL,
            reason = ""
        )
    }

    private fun getDaysBetween(startDate: Date?, endDate: Date?): Long {
        if (startDate == null || endDate == null) return 0

        val diffInMillis = endDate.time - startDate.time
        return TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS)
    }

    fun isWithinLastWeek(date: Date?): Boolean {
        if (date == null) return false

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val weekAgo = calendar.time

        return date.after(weekAgo)
    }
}