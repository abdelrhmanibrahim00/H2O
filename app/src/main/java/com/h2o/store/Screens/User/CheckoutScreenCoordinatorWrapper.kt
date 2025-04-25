package com.h2o.store.Screens.User

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.h2o.store.ViewModels.User.CartViewModel
import com.h2o.store.ViewModels.User.CheckoutCoordinatorViewModel
import java.util.UUID

/**
 * Wrapper composable that accepts required ViewModels as parameters
 * and provides a unified interface for the checkout and payment processes.
 */
@Composable
fun CheckoutScreenCoordinatorWrapper(
    navController: NavController,
    cartViewModel: CartViewModel,
    coordinatorViewModel: CheckoutCoordinatorViewModel,
    onPlaceOrder: () -> Unit,
    onBackClick: () -> Unit,
    onEditAddress: () -> Unit,
    onPaymentProcess: (String, Double) -> Unit
) {
    // Use the unified CheckoutScreen composable
    CheckoutScreenWithCoordinator(
        cartViewModel = cartViewModel,
        coordinatorViewModel = coordinatorViewModel,
        onPlaceOrder = onPlaceOrder,
        onBackClick = onBackClick,
        onEditAddress = onEditAddress,
        onPaymentProcess = { totalAmount ->
            // Generate a temporary order ID for payment tracking
            val tempOrderId = UUID.randomUUID().toString()
            onPaymentProcess(tempOrderId, totalAmount)
        }
    )
}