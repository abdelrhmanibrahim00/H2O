package com.h2o.store.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PeriodFilterButtons(
    selectedPeriod: String,
    onWeekSelected: () -> Unit,
    onMonthSelected: () -> Unit,
    onCustomPeriodSelected: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        // This Week button
        if (selectedPeriod == "week") {
            Button(
                onClick = onWeekSelected,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("This Week")
            }
        } else {
            OutlinedButton(
                onClick = onWeekSelected,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text("This Week")
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // This Month button
        if (selectedPeriod == "month") {
            Button(
                onClick = onMonthSelected,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("This Month")
            }
        } else {
            OutlinedButton(
                onClick = onMonthSelected,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text("This Month")
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Custom Period button
        if (selectedPeriod == "custom") {
            Button(
                onClick = onCustomPeriodSelected,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Period")
            }
        } else {
            OutlinedButton(
                onClick = onCustomPeriodSelected,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text("Period")
            }
        }
    }
}

@Composable
fun DateRangeDisplay(startDate: Date?, endDate: Date?) {
    if (startDate != null && endDate != null) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        Text(
            text = "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}