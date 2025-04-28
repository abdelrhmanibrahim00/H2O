package com.h2o.store.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A custom scroll indicator that shows the user's position in a scrollable list.
 * @param itemCount The total number of items in the list
 * @param visibleItemsCount Optional - number of visible items in the viewport
 * @param currentIndex Optional - current index being viewed (for controlled behavior)
 */
@Composable
fun ScrollIndicator(
    itemCount: Int,
    visibleItemsCount: Int,
    currentIndex: Int,
    modifier: Modifier = Modifier
) {
    if (itemCount <= visibleItemsCount) return

    val height = 180.dp
    val indicatorHeight = (height * visibleItemsCount) / itemCount

    // Calculate offset directly in Dp units
    val scrollProgress = currentIndex.toFloat() / (itemCount - visibleItemsCount).coerceAtLeast(1)
    val indicatorOffset = (height - indicatorHeight) * scrollProgress

    Box(
        modifier = modifier
            .height(height)
            .width(4.dp)
            .background(Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(indicatorHeight)
                .offset(y = indicatorOffset)
                .background(MaterialTheme.colors.primary, RoundedCornerShape(2.dp))
        )
    }
}