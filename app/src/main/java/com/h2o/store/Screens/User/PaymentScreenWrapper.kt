package com.h2o.store.Screens.User

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JsResult
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
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
 * Updated to work with Stripe
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
    val TAG = "StripePaymentScreen"

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
                StripePaymentWebView(
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
/**
 * WebView specifically configured for Stripe payment flow
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StripePaymentWebView(
    paymentUrl: String,
    onPageStarted: () -> Unit,
    onPageFinished: () -> Unit,
    onPaymentResult: (success: Boolean, transactionId: String?, errorMsg: String?) -> Unit
) {
    val TAG = "StripePaymentWebView"

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // Enhanced WebView settings for Stripe Checkout
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    javaScriptCanOpenWindowsAutomatically = true
                    loadsImagesAutomatically = true
                    // Allow mixed content (http resources on https pages)
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    // Enable database
                    databaseEnabled = true
                    // Allow file access and content access
                    allowFileAccess = true
                    allowContentAccess = true
                    // Set cache mode
                    cacheMode = WebSettings.LOAD_DEFAULT
                    // Set UA to more desktop-like
                    userAgentString = settings.userAgentString.replace("Mobile", "eliboM").replace("Android", "diordnA")
                }

                // Set WebView client with enhanced error handling
                webViewClient = object : WebViewClient() {
                    @SuppressLint("WebViewClientOnReceivedSslError")
                    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                        // Proceed despite SSL errors (only for development)
                        handler?.proceed()
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        // Don't override URL loading in WebView
                        return false
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        Log.d(TAG, "Page started loading: $url")
                        onPageStarted()

                        // Check if redirected to success or error URLs
                        url?.let { checkStripePaymentResult(it) }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d(TAG, "Page finished loading: $url")
                        onPageFinished()

                        // Also check on page finished
                        url?.let { checkStripePaymentResult(it) }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        Log.e(TAG, "WebView error: ${error?.description}")

                        // Only report fatal errors, not resource loading errors
                        if (request?.isForMainFrame == true) {
                            onPaymentResult(false, null, "Payment error: ${error?.description}")
                        }
                    }

                    private fun checkStripePaymentResult(url: String) {
                        Log.d(TAG, "Checking URL for Stripe payment result: $url")

                        // Success detection
                        if (url.contains("success") || url.contains("checkout/complete")) {
                            // Extract payment or session ID
                            val paymentId = extractStripePaymentId(url) ?: extractStripeSessionId(url)
                            Log.d(TAG, "Stripe payment success detected, ID: $paymentId")
                            onPaymentResult(true, paymentId, null)
                        }
                        // Cancelation detection
                        else if (url.contains("cancel") || url.contains("checkout/canceled")) {
                            Log.d(TAG, "Stripe payment cancelation detected")
                            onPaymentResult(false, null, "Payment was canceled")
                        }
                    }

                    private fun extractStripePaymentId(url: String): String? {
                        return try {
                            // Match payment_intent parameter (pi_...)
                            val regex = Regex("[?&]payment_intent=([^&]+)")
                            val matchResult = regex.find(url)
                            matchResult?.groups?.get(1)?.value
                        } catch (e: Exception) {
                            Log.e(TAG, "Error extracting payment ID: ${e.message}", e)
                            null
                        }
                    }

                    private fun extractStripeSessionId(url: String): String? {
                        return try {
                            // Match session_id parameter (cs_...)
                            val regex = Regex("[?&]session_id=([^&]+)")
                            val matchResult = regex.find(url)
                            matchResult?.groups?.get(1)?.value
                        } catch (e: Exception) {
                            Log.e(TAG, "Error extracting session ID: ${e.message}", e)
                            null
                        }
                    }
                }

                // Set WebChromeClient to handle JavaScript dialogs, etc.
                webChromeClient = object : WebChromeClient() {
                    override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                        Log.d(TAG, "JS Alert: $message")
                        result?.confirm()
                        return true
                    }

                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        Log.d(TAG, "Console: ${consoleMessage?.message()}")
                        return true
                    }
                }

                // Load the payment URL
                Log.d(TAG, "Loading payment URL: $paymentUrl")
                loadUrl(paymentUrl)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}