package com.h2o.store.Screens.User

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.h2o.store.ViewModels.User.CheckoutCoordinatorViewModel
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Wrapper composable that accepts coordinator ViewModel passed from AppNavHost
 */
@Composable
fun PaymentScreenCoordinatorWrapper(
    coordinatorViewModel: CheckoutCoordinatorViewModel,
    orderId: String,
    totalAmount: Double,
    onBackClick: () -> Unit,
    onPaymentSuccess: () -> Unit,
    onPaymentFailure: () -> Unit
) {
    // Use the PaymentScreen with coordinator
    PaymentScreenWithCoordinator(
        orderId = orderId,
        totalAmount = totalAmount,
        coordinatorViewModel = coordinatorViewModel,
        onBackClick = onBackClick,
        onPaymentSuccess = onPaymentSuccess,
        onPaymentFailure = onPaymentFailure
    )
}

/**
 * Payment screen that uses the coordinator ViewModel for state management
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreenWithCoordinator(
    orderId: String,
    totalAmount: Double,
    coordinatorViewModel: CheckoutCoordinatorViewModel,
    onBackClick: () -> Unit,
    onPaymentSuccess: () -> Unit,
    onPaymentFailure: () -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()
    val TAG = "PaymentScreen"

    // Observe ViewModel states
    val paymentState by coordinatorViewModel.paymentState.collectAsState()
    val paymentUrl by coordinatorViewModel.paymentUrl.collectAsState()
    val error by coordinatorViewModel.error.collectAsState()
    val paymentResult by coordinatorViewModel.paymentResult.collectAsState()
    val isProcessing by coordinatorViewModel.isProcessing.collectAsState()

    // Track if WebView is loaded
    var isWebViewLoaded by remember { mutableStateOf(false) }

    // Handle error state
    LaunchedEffect(error) {
        error?.let {
            scope.launch {
                scaffoldState.snackbarHostState.showSnackbar(it)
            }
        }
    }

    // Handle payment result
    LaunchedEffect(paymentResult) {
        paymentResult?.let {
            if (it.success) {
                onPaymentSuccess()
            } else {
                onPaymentFailure()
            }
        }
    }

    // Initiate payment when screen is first composed
    // Only do this if paymentUrl is null to avoid re-triggering
    LaunchedEffect(Unit) {
        if (paymentUrl == null) {
            // Use the coordinator to initiate payment
            coordinatorViewModel.initiatePayment(orderId, totalAmount, emptyList())
        }
    }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = { Text("Payment") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isProcessing && !isWebViewLoaded) {
                // Show loading indicator while payment URL is being generated
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (!isProcessing && paymentUrl == null && error != null) {
                // Show error message and retry button
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = error ?: "Unknown error occurred",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            coordinatorViewModel.initiatePayment(orderId, totalAmount, emptyList())
                        }
                    ) {
                        Text("Retry Payment")
                    }
                }
            } else if (paymentUrl != null) {
                // Show payment WebView
                PaymentWebView(
                    paymentUrl = paymentUrl!!,
                    onPageStarted = { isWebViewLoaded = false },
                    onPageFinished = { isWebViewLoaded = true },
                    onPaymentResult = { success, transactionId, errorMsg ->
                        if (success) {
                            coordinatorViewModel.handlePaymentSuccess(transactionId!!)
                        } else {
                            coordinatorViewModel.handlePaymentFailure(errorMsg)
                        }
                    }
                )

                // Show loading indicator on top of WebView while it's loading
                if (!isWebViewLoaded) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PaymentWebView(
    paymentUrl: String,
    onPageStarted: () -> Unit,
    onPageFinished: () -> Unit,
    onPaymentResult: (success: Boolean, transactionId: String?, errorMsg: String?) -> Unit
) {
    val TAG = "PaymentWebView"

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // Configure WebView settings
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadsImagesAutomatically = true

                // Set WebView client
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        Log.d(TAG, "Page started loading: $url")
                        onPageStarted()

                        // Check if redirected to success or error URLs
                        checkPaymentResult(url)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d(TAG, "Page finished loading: $url")
                        onPageFinished()

                        // Also check on page finished
                        checkPaymentResult(url)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        Log.e(TAG, "WebView error: ${error?.description}")
                        onPaymentResult(false, null, "Payment error: ${error?.description}")
                    }

                    private fun checkPaymentResult(url: String?) {
                        url ?: return

                        Log.d(TAG, "Checking URL for payment result: $url")

                        // PayMob success URL parsing (assumes you've configured success URL in PayMob dashboard)
                        if (url.contains("success=true") || url.contains("success.html")) {
                            // Extract transaction ID from URL if present
                            val transactionId = extractTransactionId(url)
                            Log.d(TAG, "Payment success detected, transaction ID: $transactionId")
                            onPaymentResult(true, transactionId ?: "unknown", null)
                        }
                        // PayMob failure URL parsing
                        else if (url.contains("success=false") || url.contains("failure.html")) {
                            Log.d(TAG, "Payment failure detected")
                            val errorMsg = extractErrorMessage(url)
                            onPaymentResult(false, null, errorMsg ?: "Payment failed")
                        }
                    }

                    private fun extractTransactionId(url: String): String? {
                        return try {
                            // Extract transaction ID from PayMob response URL
                            // Format may be like: ?success=true&trx_id=12345
                            val regex = Regex("[?&]trx_id=([^&]+)")
                            val matchResult = regex.find(url)
                            val value = matchResult?.groups?.get(1)?.value
                            URLDecoder.decode(value, StandardCharsets.UTF_8.name())
                        } catch (e: Exception) {
                            Log.e(TAG, "Error extracting transaction ID: ${e.message}", e)
                            null
                        }
                    }

                    private fun extractErrorMessage(url: String): String? {
                        return try {
                            // Extract error message if present
                            val regex = Regex("[?&]error=([^&]+)")
                            val matchResult = regex.find(url)
                            val value = matchResult?.groups?.get(1)?.value
                            URLDecoder.decode(value, StandardCharsets.UTF_8.name())
                        } catch (e: Exception) {
                            Log.e(TAG, "Error extracting error message: ${e.message}", e)
                            null
                        }
                    }
                }

                // Load the payment URL
                loadUrl(paymentUrl)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}