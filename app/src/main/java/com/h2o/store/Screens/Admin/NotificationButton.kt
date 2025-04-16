package com.h2o.store.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.h2o.store.utils.NotificationPriority
import com.h2o.store.utils.PrioritizedNotification
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

// Define notification colors
private val NotificationYellow = Color(0xFFFFD700) // Gold/Yellow for normal notifications
private val NotificationRed = Color(0xFFFF4D4D)    // Red for urgent notifications

@Composable
fun EnhancedNotificationButton(
    totalCount: Int,
    hasUrgentNotifications: Boolean,
    notifications: List<PrioritizedNotification>,
    onNotificationClick: () -> Unit,
    onNotificationItemClick: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    // Determine icon color based on notification priority
    val iconColor = if (hasUrgentNotifications) NotificationRed else NotificationYellow

    Box {
        IconButton(
            onClick = {
                expanded = true
                onNotificationClick()
            },
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            BadgedBox(
                badge = {
                    if (totalCount > 0) {
                        Badge(
                            containerColor = if (hasUrgentNotifications)
                                NotificationRed else MaterialTheme.colorScheme.error
                        ) {
                            Text(
                                text = if (totalCount > 99) "99+" else totalCount.toString(),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = iconColor
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(320.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    "Notifications",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                Divider()

                if (notifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No new notifications")
                    }
                } else {
                    notifications.take(5).forEach { notification ->
                        PrioritizedNotificationItem(
                            notification = notification,
                            dateFormatter = dateFormatter,
                            currencyFormat = currencyFormat,
                            onClick = {
                                onNotificationItemClick(notification.order.orderId)  // Pass just the orderId
                                expanded = false
                            }
                        )
                    }

                    if (notifications.size > 5) {
                        TextButton(
                            onClick = { expanded = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text("View all notifications")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrioritizedNotificationItem(
    notification: PrioritizedNotification,
    dateFormatter: SimpleDateFormat,
    currencyFormat: NumberFormat,
    onClick: () -> Unit
) {
    val order = notification.order
    val isUrgent = notification.priority == NotificationPriority.URGENT
    val itemColor = if (isUrgent) NotificationRed.copy(alpha = 0.1f) else Color.Transparent

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUrgent) itemColor else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Show warning icon for urgent notifications
                    if (isUrgent) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Urgent",
                            tint = NotificationRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = "Order #${order.orderId.takeLast(5)}: ${order.status}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isUrgent) NotificationRed else MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = order.orderDate?.let { dateFormatter.format(it) } ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${order.items?.size ?: 0} items • ${currencyFormat.format(order.total)}",
                style = MaterialTheme.typography.bodySmall,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )

            // Show reason for urgent notifications
            if (isUrgent && notification.reason.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = notification.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = NotificationRed,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}