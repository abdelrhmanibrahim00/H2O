package com.h2o.store.components

import android.app.DatePickerDialog
import android.content.Context
import android.widget.DatePicker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar
import java.util.Date

class DateRangePickerManager(private val context: Context) {
    private val calendar = Calendar.getInstance()

    // Function to show date picker for start date
    fun showStartDatePicker(
        initialDate: Date? = null,
        onDateSelected: (Date) -> Unit
    ) {
        calendar.time = initialDate ?: Date()

        DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                calendar.set(year, month, dayOfMonth, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                onDateSelected(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Function to show date picker for end date
    fun showEndDatePicker(
        startDate: Date,
        initialDate: Date? = null,
        onDateSelected: (Date) -> Unit
    ) {
        calendar.time = initialDate ?: Date()

        val endDatePicker = DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                calendar.set(year, month, dayOfMonth, 23, 59, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                onDateSelected(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set minimum date to ensure end date is not before start date
        val startCalendar = Calendar.getInstance()
        startCalendar.time = startDate
        endDatePicker.datePicker.minDate = startCalendar.timeInMillis

        endDatePicker.show()
    }
}

@Composable
fun rememberDateRangePickerManager(): DateRangePickerManager {
    val context = LocalContext.current
    return remember { DateRangePickerManager(context) }
}