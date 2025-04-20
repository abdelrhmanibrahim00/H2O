
// InventoryAnalysisScreen.kt
package com.h2o.store.Screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.h2o.store.ViewModels.InventoryAnalysisViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InventoryAnalysisScreen(
    navController: NavController,
    viewModel: InventoryAnalysisViewModel,
    onBackClick: () -> Unit = { navController.popBackStack() },
    onViewDetailedReport: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val lastGeneratedAt by viewModel.lastGeneratedAt.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Title moved to a row with proper alignment
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 48.dp) // Balance with back button
                    ) {
                        Text(
                            "Inventory Optimization",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card with explanation
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "AI-Powered Inventory Optimization",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Generate AI-driven inventory recommendations based on historical sales data and current stock levels.",
                        fontSize = 14.sp
                    )

                    // Last generated timestamp
                    lastGeneratedAt?.let { date ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Last generated: ${formatDate(date)}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Main content based on UI state
            when (uiState) {
                is InventoryAnalysisViewModel.InventoryAnalysisUiState.Initial -> {
                    InitialStateContent(onGenerateClick = { viewModel.generateInventoryReport() })
                }

                is InventoryAnalysisViewModel.InventoryAnalysisUiState.Loading -> {
                    LoadingStateContent()
                }

                is InventoryAnalysisViewModel.InventoryAnalysisUiState.Success -> {
                    val data = (uiState as InventoryAnalysisViewModel.InventoryAnalysisUiState.Success).data
                    SuccessStateContent(
                        data = data,
                        onGenerateAgainClick = { viewModel.generateInventoryReport() },
                        onViewDetailedReportClick = onViewDetailedReport
                    )
                }

                is InventoryAnalysisViewModel.InventoryAnalysisUiState.Error -> {
                    val errorMessage = (uiState as InventoryAnalysisViewModel.InventoryAnalysisUiState.Error).message
                    ErrorStateContent(
                        errorMessage = errorMessage,
                        onRetryClick = { viewModel.generateInventoryReport() }
                    )
                }

                else -> {
                    // This should never happen, but is required for compilation
                    Text("Unknown state")
                }
            }
        }
    }
}

@Composable
private fun InitialStateContent(onGenerateClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Ready to optimize your inventory",
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = onGenerateClick,
            modifier = Modifier
                .fillMaxWidth(0.7f) // Control button width
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                "Generate Report",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun LoadingStateContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Analyzing inventory data and generating recommendations...",
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun SuccessStateContent(
    data: com.h2o.store.data.remote.BulkPredictionResponse,
    onGenerateAgainClick: () -> Unit,
    onViewDetailedReportClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFFE8F5E9),
            elevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Report Generated Successfully",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Generated ${data.predictions.size} recommendations for your inventory.",
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Summary text
        Text(
            text = "The AI has analyzed your sales patterns and current inventory levels. " +
                    "Recommendations have been processed and are ready for your review in the full report.",
            color = Color.DarkGray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // "View Detailed Report" button
        Button(
            onClick = onViewDetailedReportClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                "View Detailed Report",
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // "Generate New Report" button - more consistent with smaller button
        Button(
            onClick = onGenerateAgainClick,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp)
        ) {
            Text(
                "Generate New Report",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun ErrorStateContent(
    errorMessage: String,
    onRetryClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFFFFEBEE),
            elevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Error Generating Report",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB71C1C),
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = Color.DarkGray,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetryClick,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text(
                "Try Again",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

// Utility function to format a date nicely
private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
    return formatter.format(date)
}