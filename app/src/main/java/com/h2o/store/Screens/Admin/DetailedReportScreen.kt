// DetailedReportScreen.kt
package com.h2o.store.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.h2o.store.ViewModels.InventoryAnalysisViewModel
import com.h2o.store.data.remote.PredictionResponse

@Composable
fun DetailedReportScreen(
    viewModel: InventoryAnalysisViewModel,
    onBackClick: () -> Unit
) {
    // Get predictions from the viewModel
    val predictionData by viewModel.predictionData.collectAsState()
    val predictions = predictionData?.predictions ?: emptyList()

    // Create a LazyListState to track scrolling
    val listState = rememberLazyListState()

    // Calculate current item index for scroll indicator
    val currentIndex by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Centered title with proper spacing
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 48.dp) // Balance with back button
                    ) {
                        Text(
                            "Inventory Report Details",
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = Color.White,
                elevation = 4.dp
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // Header
                Text(
                    text = "AI-Generated Inventory Recommendations",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "${predictions.size} products analyzed",
                    color = MaterialTheme.colors.primary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // List of predictions with scroll state
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(predictions) { prediction ->
                        PredictionCard(prediction)
                    }
                    // Add bottom padding
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // Add scroll indicator to the right side of the screen
            if (predictions.isNotEmpty()) {
                ScrollIndicator(
                    itemCount = predictions.size,
                    visibleItemsCount = 3, // Adjust based on your screen size
                    currentIndex = currentIndex,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                )
            }
        }
    }
}

@Composable
fun PredictionCard(prediction: PredictionResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Product ID
            Text(
                text = "Product ID: ${prediction.productId}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Divider(
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Recommendation information
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "Recommended Stock: ${prediction.recommendedStock}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = prediction.message,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "Confidence: ${(prediction.confidence * 100).toInt()}%",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }
    }
}

