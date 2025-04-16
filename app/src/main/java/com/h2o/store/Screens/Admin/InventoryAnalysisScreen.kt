package com.h2o.store.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
    viewModel: InventoryAnalysisViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val lastGeneratedAt by viewModel.lastGeneratedAt.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory Optimization") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
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
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Generate AI-driven inventory recommendations based on historical sales data and current stock levels."
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
                        onGenerateAgainClick = { viewModel.generateInventoryReport() }
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(MaterialTheme.colors.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Ready to optimize your inventory",
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onGenerateClick
            ) {
                Text("Generate Report")
            }
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Analyzing inventory data and generating recommendations...",
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SuccessStateContent(
    data: com.h2o.store.data.remote.BulkPredictionResponse,
    onGenerateAgainClick: () -> Unit
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
                    color = Color(0xFF2E7D32)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Generated ${data.predictions.size} recommendations for your inventory."
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Summary text - we're not displaying details here as requested
        Text(
            text = "The AI has analyzed your sales patterns and current inventory levels. " +
                    "Recommendations have been processed and are ready for your review in the full report.",
            color = Color.DarkGray
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onGenerateAgainClick,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Generate New Report")
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
                    color = Color(0xFFB71C1C)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = errorMessage,
                    color = Color.DarkGray
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetryClick
        ) {
            Text("Try Again")
        }
    }
}

// Utility function to format a date nicely
private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
    return formatter.format(date)
}