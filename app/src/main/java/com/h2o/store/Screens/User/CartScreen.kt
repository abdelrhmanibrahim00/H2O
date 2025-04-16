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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.h2o.store.ViewModels.User.CartViewModel
import com.h2o.store.components.MainScaffold
import com.h2o.store.data.Cart.CartItem

// Wrapper composable that initializes the CartViewModel with the current user ID
@Composable
fun CartScreenWrapper(
    navController: NavHostController,
    onCheckout: () -> Unit,
    onCartClick: () -> Unit,
    onHomeClick: () -> Unit,
    onOrderClick: () -> Unit,
    onProfileClick: () -> Unit,
    onHelpClick: () -> Unit,
    onLogoutClick: () -> Unit,
    cartViewModel: CartViewModel
) {
    // Use the existing CartScreen composable with the user-specific ViewModel
    CartScreen(
        navController = navController,
        cartViewModel = cartViewModel,
        onCheckout = onCheckout,
        onCartClick = onCartClick,
        onHomeClick = onHomeClick,
        onOrderClick = onOrderClick,
        onProfileClick = onProfileClick,
        onHelpClick = onHelpClick,
        onLogoutClick = onLogoutClick
    )
}

// The modified CartScreen that uses MainScaffold
@Composable
fun CartScreen(
    navController: NavHostController,
    cartViewModel: CartViewModel,
    onCheckout: () -> Unit,
    onCartClick: () -> Unit,
    onHomeClick: () -> Unit,
    onOrderClick: () -> Unit,
    onProfileClick: () -> Unit,
    onHelpClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    // Use MainScaffold instead of creating a new Scaffold
    MainScaffold(
        navController = navController,
        cartViewModel = cartViewModel,
        title = "Cart",
        onHomeClick = onHomeClick,
        onCartClick = onCartClick,
        onOrderClick = onOrderClick,
        onProfileClick = onProfileClick,
        onHelpClick = onHelpClick,
        onLogoutClick = onLogoutClick
    ) { paddingValues ->
        // Your existing cart content here, with proper padding
        CartContent(
            viewModel = cartViewModel,
            onCheckout = onCheckout,
            paddingValues = paddingValues
        )
    }
}

@Composable
fun CartContent(
    viewModel: CartViewModel,
    onCheckout: () -> Unit,
    paddingValues: PaddingValues
) {
    val cartItems by viewModel.cartItems.collectAsState()
    val totalPrice by viewModel.totalPrice.collectAsState()

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
                Text("Your cart is empty")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cartItems) { item ->
                    CartItemCard(
                        cartItem = item,
                        onQuantityChange = { newQuantity ->
                            viewModel.updateQuantity(item, newQuantity)
                        },
                        onRemove = {
                            viewModel.removeFromCart(item)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Total: ${totalPrice} EGP",
                        style = MaterialTheme.typography.h6
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onCheckout,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Proceed to Checkout")
                    }
                }
            }
        }
    }
}

@Composable
private fun CartItemCard(
    cartItem: CartItem,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = cartItem.imageUrl,
                contentDescription = cartItem.name,
                modifier = Modifier
                    .size(80.dp)
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
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onQuantityChange(cartItem.quantity - 1) }) {
                        Icon(Icons.Default.Remove, "Decrease")
                    }
                    Text(
                        text = cartItem.quantity.toString(),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(onClick = { onQuantityChange(cartItem.quantity + 1) }) {
                        Icon(Icons.Default.Add, "Increase")
                    }
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, "Remove")
                }
            }
        }
    }
}