package com.h2o.store.Screens.User

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.RadioButton
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.h2o.store.ViewModels.User.CartViewModel
import com.h2o.store.ViewModels.User.CheckoutCoordinatorViewModel
import com.h2o.store.data.Cart.CartItem
import com.h2o.store.data.User.UserData
import kotlinx.coroutines.launch

/**
 * Checkout screen using the coordinator pattern for better data sharing
 * and state management between checkout and payment processes.
 */
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreenWithCoordinator(
    cartViewModel: CartViewModel,
    coordinatorViewModel: CheckoutCoordinatorViewModel,
    onPlaceOrder: () -> Unit,
    onBackClick: () -> Unit,
    onEditAddress: () -> Unit,
    onPaymentProcess: (Double) -> Unit
) {
    // Initialize scaffold state
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()

    // Remember modal bottom sheet state for any additional actions
    val modalBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden
    )

    // Get cart items and total
    val cartItems by cartViewModel.cartItems.collectAsState()
    val totalPrice by cartViewModel.totalPrice.collectAsState()

    // Checkout state from coordinator
    val isProcessing by coordinatorViewModel.isProcessing.collectAsState()
    val orderSuccess by coordinatorViewModel.orderSuccess.collectAsState()
    val error by coordinatorViewModel.error.collectAsState()

    // Address state from coordinator
    val userData by coordinatorViewModel.userData.collectAsState()
    val isAddressLoading by coordinatorViewModel.isUserDataLoading.collectAsState()

    // Monitor payment state
    val paymentState by coordinatorViewModel.paymentState.collectAsState()

    // Monitor for order success and trigger navigation if needed
    LaunchedEffect(orderSuccess) {
        if (orderSuccess) {
            onPlaceOrder()
        }
    }

    // Show errors in snackbar
    LaunchedEffect(error) {
        error?.let {
            scope.launch {
                scaffoldState.snackbarHostState.showSnackbar(it)
            }
        }
    }

    ModalBottomSheetLayout(
        sheetState = modalBottomSheetState,
        sheetContent = {
            // Simple bottom sheet with additional options if needed
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Additional Options", style = MaterialTheme.typography.h6)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Edit Address",
                    modifier = Modifier
                        .padding(8.dp)
                        .selectable(
                            selected = false,
                            onClick = {
                                scope.launch {
                                    modalBottomSheetState.hide()
                                    onEditAddress()
                                }
                            }
                        )
                )
                Text("Apply Coupon", modifier = Modifier.padding(8.dp))
            }
        }
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            scaffoldState = scaffoldState,
            topBar = {
                Surface(
                    modifier = Modifier.statusBarsPadding()
                ) {
                    TopAppBar(
                        title = { Text(text = "Checkout") },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                scope.launch {
                                    if (modalBottomSheetState.isVisible) {
                                        modalBottomSheetState.hide()
                                    } else {
                                        modalBottomSheetState.show()
                                    }
                                }
                            }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                        }
                    )
                }
            }
        ) { paddingValues ->
            CheckoutContent(
                cartItems = cartItems,
                totalPrice = totalPrice.toDouble(),
                isProcessing = isProcessing,
                userData = userData,
                isAddressLoading = isAddressLoading,
                onEditAddress = onEditAddress,
                onPlaceOrder = { paymentMethod ->
                    // Process order through coordinator
                    when (paymentMethod) {
                        "Cash on Delivery" -> {
                            coordinatorViewModel.placeOrder(
                                cartItems = cartItems,
                                totalAmount = totalPrice.toDouble(),
                                paymentMethod = paymentMethod
                            )
                        }
                        "Credit Card" -> {
                            // Check if user has a valid address
                            if (coordinatorViewModel.hasValidAddress()) {
                                // Pass control to payment process
                                onPaymentProcess(totalPrice.toDouble())
                            } else {
                                // Error will be shown via state flow
                                coordinatorViewModel.placeOrder(
                                    cartItems = cartItems,
                                    totalAmount = totalPrice.toDouble(),
                                    paymentMethod = paymentMethod
                                )
                            }
                        }
                        else -> {
                            // Handle other payment methods
                            coordinatorViewModel.placeOrder(
                                cartItems = cartItems,
                                totalAmount = totalPrice.toDouble(),
                                paymentMethod = paymentMethod
                            )
                        }
                    }
                },
                error = error,
                hasValidAddress = coordinatorViewModel.hasValidAddress(),
                paddingValues = paddingValues
            )
        }
    }
}

/**
 * Modified CheckoutContent to work with coordinator pattern
 */
@Composable
fun CheckoutContent(
    cartItems: List<CartItem>,
    totalPrice: Double,
    isProcessing: Boolean,
    userData: UserData?,
    isAddressLoading: Boolean,
    onEditAddress: () -> Unit,
    onPlaceOrder: (String) -> Unit,
    error: String?,
    hasValidAddress: Boolean,
    paddingValues: PaddingValues
) {
    // State for selected payment method
    val paymentOptions = listOf("Cash on Delivery", "Credit Card")
    var selectedPayment by remember { mutableStateOf(paymentOptions[0]) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
    ) {
        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No items to checkout")
            }
        } else {
            // Order summary section
            Text(
                text = "Order Summary",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cartItems) { item ->
                    CheckoutItemCard(cartItem = item)
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Shipping Address Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shipping Address",
                    style = MaterialTheme.typography.h6,
                )

                IconButton(onClick = onEditAddress) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Address"
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 2.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    when {
                        isAddressLoading -> {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.Center)
                            )
                        }
                        userData?.address == null -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "No shipping address found",
                                    color = MaterialTheme.colors.error,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onEditAddress,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text("Add Address")
                                }
                            }
                        }
                        else -> {
                            Column {
                                userData.address.formattedAddress.let {
                                    Text(
                                        text = it.ifEmpty {
                                            "${userData.address.street}, ${userData.address.city}, ${userData.address.state}, ${userData.address.country} ${userData.address.postalCode}"
                                        },
                                        style = MaterialTheme.typography.body1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Payment method section
            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    paymentOptions.forEach { payment ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (payment == selectedPayment),
                                    onClick = { selectedPayment = payment }
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (payment == selectedPayment),
                                onClick = { selectedPayment = payment }
                            )
                            Text(
                                text = payment,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            // Order total and place order button
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Subtotal:",
                            style = MaterialTheme.typography.body1
                        )
                        Text(
                            text = "${totalPrice} EGP",
                            style = MaterialTheme.typography.body1
                        )
                    }

                    // Add delivery fee or any other charges if applicable
                    val deliveryFee = 15.0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Delivery Fee:",
                            style = MaterialTheme.typography.body1
                        )
                        Text(
                            text = "${deliveryFee} EGP",
                            style = MaterialTheme.typography.body1
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total:",
                            style = MaterialTheme.typography.h6,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${totalPrice + deliveryFee} EGP",
                            style = MaterialTheme.typography.h6,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onPlaceOrder(selectedPayment) },
                        enabled = !isProcessing && (hasValidAddress || selectedPayment != "Credit Card"),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Text("Place Order")
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun CheckoutItemCard(
    cartItem: CartItem
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = cartItem.imageUrl,
                contentDescription = cartItem.name,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Fit
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = cartItem.name,
                    style = MaterialTheme.typography.subtitle1
                )
                Text(
                    text = "${cartItem.price} EGP",
                    style = MaterialTheme.typography.body2
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "Qty: ${cartItem.quantity}",
                    style = MaterialTheme.typography.body2
                )
                Text(
                    text = "${cartItem.price * cartItem.quantity} EGP",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}