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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.h2o.store.ViewModels.User.CartViewModel
import com.h2o.store.ViewModels.User.CheckoutViewModel
import com.h2o.store.data.Cart.CartItem
import com.h2o.store.data.User.UserData
import kotlinx.coroutines.launch

// Wrapper composable that initializes the CartViewModel with the current user ID
@Composable
fun CheckoutScreenWrapper(
    navController: NavController,
    onPlaceOrder: () -> Unit,
    onBackClick: () -> Unit,
    onEditAddress: () -> Unit
) {
    // Get current user ID from Firebase Auth
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid ?: ""

    // Create user-specific CartViewModel using the Factory
    val cartViewModel: CartViewModel = viewModel(
        factory = CartViewModel.Factory(userId)
    )

    // Create CheckoutViewModel as well
    val checkoutViewModel: CheckoutViewModel = viewModel(
        factory = CheckoutViewModel.Factory(userId)
    )

    // Use the CheckoutScreen composable
    CheckoutScreen(
        navController = navController,
        cartViewModel = cartViewModel,
        checkoutViewModel = checkoutViewModel,
        onPlaceOrder = onPlaceOrder,
        onBackClick = onBackClick,
        onEditAddress = onEditAddress
    )
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    navController: NavController,
    cartViewModel: CartViewModel,
    checkoutViewModel: CheckoutViewModel,
    onPlaceOrder: () -> Unit,
    onBackClick: () -> Unit,
    onEditAddress: () -> Unit
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

    // Checkout state
    val isProcessing by checkoutViewModel.isProcessing.collectAsState()
    val orderSuccess by checkoutViewModel.orderSuccess.collectAsState()
    val error by checkoutViewModel.error.collectAsState()

    // Address state
    val userAddress by checkoutViewModel.userData.collectAsState()
    val isAddressLoading by checkoutViewModel.isUserDataLoading.collectAsState()

    // Monitor for order success and trigger navigation if needed
    LaunchedEffect(orderSuccess) {
        if (orderSuccess) {
            onPlaceOrder()
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
                userAddress = userAddress,
                isAddressLoading = isAddressLoading,
                onEditAddress = onEditAddress,
                onPlaceOrder = { paymentMethod ->
                    checkoutViewModel.placeOrder(
                        cartItems = cartItems,
                        totalAmount = totalPrice.toDouble(),
                        paymentMethod = paymentMethod
                    )
                },
                error = error,
                paddingValues = paddingValues
            )
        }
    }
}

@Composable
fun CheckoutContent(
    cartItems: List<CartItem>,
    totalPrice: Double,
    isProcessing: Boolean,
    userAddress: UserData?,
    isAddressLoading: Boolean,
    onEditAddress: () -> Unit,
    onPlaceOrder: (String) -> Unit,
    error: String?,
    paddingValues: PaddingValues
) {
    // State for selected payment method
    val paymentOptions = listOf("Cash on Delivery", "Credit Card", "Digital Wallet")
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
                        userAddress == null -> {
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
                                userAddress.address?.formattedAddress?.let {
                                    Text(
                                        text = it.ifEmpty {
                                            "${userAddress.address.street}, ${userAddress.address.city}, ${userAddress.address.state}, ${userAddress.address.country} ${userAddress.address.postalCode}"
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

            // Error message if any
            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

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
                        enabled = !isProcessing && userAddress != null,
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