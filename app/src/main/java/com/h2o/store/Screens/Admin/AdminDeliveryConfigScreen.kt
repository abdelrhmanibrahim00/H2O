package com.h2o.store.Screens.Admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.h2o.store.ViewModels.DeliveryConfigViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDeliveryConfigScreen(
    viewModel: DeliveryConfigViewModel,
    onBackClick: () -> Unit,
    onManageDistricts: () -> Unit
) {
    val config by viewModel.deliveryConfig.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val actionResult by viewModel.actionCompleted.collectAsState()

    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Form state
    var standardFee by remember { mutableStateOf("0.00") }
    var marketThreshold by remember { mutableStateOf("0.00") }
    var homeThreshold by remember { mutableStateOf("0.00") }

    // Initialize form values when config is loaded
    LaunchedEffect(config) {
        config?.let {
            standardFee = String.format("%.2f", it.standardDeliveryFee)
            marketThreshold = String.format("%.2f", it.freeDeliveryThresholdMarket)
            homeThreshold = String.format("%.2f", it.freeDeliveryThresholdHome)
        }
    }

    // Handle action result
    LaunchedEffect(actionResult) {
        actionResult?.let {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = it.action
                )
            }
            viewModel.resetActionResult()
        }
    }

    // Input validation
    fun isValidInput(): Boolean {
        return try {
            standardFee.replace(",", ".").toDouble() >= 0 &&
                    marketThreshold.replace(",", ".").toDouble() >= 0 &&
                    homeThreshold.replace(",", ".").toDouble() >= 0
        } catch (e: Exception) {
            false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Delivery Configuration") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading && config == null) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Delivery Fee Configuration Card
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Delivery Fee Settings",
                                style = MaterialTheme.typography.titleLarge
                            )

                            Divider()

                            // Standard Delivery Fee
                            OutlinedTextField(
                                value = standardFee,
                                onValueChange = { standardFee = it },
                                label = { Text("Standard Delivery Fee") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Market Account Threshold
                            OutlinedTextField(
                                value = marketThreshold,
                                onValueChange = { marketThreshold = it },
                                label = { Text("Market Account Free Delivery Threshold") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Home Account Threshold
                            OutlinedTextField(
                                value = homeThreshold,
                                onValueChange = { homeThreshold = it },
                                label = { Text("Home Account Free Delivery Threshold") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Save button
                    Button(
                        onClick = {
                            if (isValidInput()) {
                                viewModel.updateDeliveryConfig(
                                    standardDeliveryFee = standardFee.replace(",", ".").toDouble(),
                                    freeDeliveryThresholdMarket = marketThreshold.replace(",", ".").toDouble(),
                                    freeDeliveryThresholdHome = homeThreshold.replace(",", ".").toDouble()
                                )
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Please enter valid values")
                                }
                            }
                        },
                        enabled = !isLoading && isValidInput(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Changes")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Manage Districts Button
                    Button(
                        onClick = onManageDistricts,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Manage Delivery Districts")
                    }

                    // Refresh Button
                    Button(
                        onClick = { viewModel.refreshData() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Refresh Data")
                    }
                }
            }

            // Show loading indicator for operations
            if (isLoading && config != null) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}