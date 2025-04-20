package com.h2o.store.Screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * A custom scroll indicator that shows the user's position in a scrollable list.
 * @param itemCount The total number of items in the list
 * @param visibleItemsCount Optional - number of visible items in the viewport
 * @param currentIndex Optional - current index being viewed (for controlled behavior)
 */
@Composable
fun ScrollIndicator(
    itemCount: Int,
    visibleItemsCount: Int = 3, // Default: shows about 3 items at once
    currentIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    if (itemCount <= visibleItemsCount) return // No need for indicator if all items visible

    val density = LocalDensity.current
    var heightPx by remember { mutableStateOf(0f) }
    var isVisible by remember { mutableStateOf(true) }

    // Auto-hide logic after a delay
    LaunchedEffect(Unit) {
        delay(3000)
        isVisible = false
    }

    // Calculate indicator position and size
    val indicatorHeight = (visibleItemsCount.toFloat() / itemCount.toFloat()) * 100f
    val indicatorPosition = (currentIndex.toFloat() / itemCount.toFloat()) * 100f

    // Animate transparency
    val alpha by animateFloatAsState(if (isVisible) 0.8f else 0.0f, label = "alpha")

    Box(
        modifier = modifier
            .padding(end = 2.dp)
            .fillMaxHeight()
            .width(4.dp)
            .onGloballyPositioned { coordinates ->
                heightPx = coordinates.size.height.toFloat()
            }
    ) {
        // Background track
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .alpha(0.2f)
                .align(Alignment.CenterEnd)
                .background(
                    color = Color.Gray,
                    shape = RoundedCornerShape(1.dp)
                )
        )

        // The indicator itself
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = with(density) { (heightPx * indicatorPosition / 100f).toDp() })
                .height(with(density) { (heightPx * indicatorHeight / 100f).toDp() })
                .width(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colors.primary)
                .alpha(alpha)
        )
    }
}