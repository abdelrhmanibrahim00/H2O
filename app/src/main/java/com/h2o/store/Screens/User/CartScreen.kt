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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Badge
import androidx.compose.material.BadgedBox
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.h2o.store.Navigation.Screen
import com.h2o.store.ViewModels.User.CartViewModel
import com.h2o.store.data.Cart.CartItem
import kotlinx.coroutines.launch

// Wrapper composable that initializes the CartViewModel with the current user ID
@Composable
fun CartScreenWrapper(
    navController: NavController,
    onCheckout: () -> Unit,
    onCartClick: () -> Unit,
    onHomeClick: () -> Unit,
    onOrderClick: () -> Unit,
    onProfileClick: () -> Unit,
    onHelpClick: () -> Unit,
    onLogoutClick: () -> Unit ,
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

// The original CartScreen with no changes to its implementation
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    navController:NavController,
    cartViewModel: CartViewModel,
    onCheckout: () -> Unit,
    onCartClick : () -> Unit,
    onHomeClick : () -> Unit,
    onOrderClick : () -> Unit,
    onProfileClick: () -> Unit,
    onHelpClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    // Initialize same components as HomeScreen
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val currentRoute = navBackStackEntry?.destination?.route

    // Remember modal bottom sheet state
    val modalBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden
    )

    ModalBottomSheetLayout(
        sheetState = modalBottomSheetState,
        sheetContent = {
            BottomSheetContent()  // Reuse the same BottomSheetContent from HomeScreen
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
                        title = { Text(text = "Cart") },
                        navigationIcon = {
                            IconButton(onClick = {
                                scope.launch { scaffoldState.drawerState.open() }
                            }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
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
            } , bottomBar = {
                Surface(
                    modifier = Modifier.navigationBarsPadding() // Specific padding for navigation bar
                ){ BottomNavigation {

                    val navItems = listOf(
                        ScreenItem(Screen.Home, { onHomeClick() }, "Home", Icons.Default.Home),
                        ScreenItem(Screen.Cart,{onCartClick()}, "Cart", Icons.Default.ShoppingCart),
                        ScreenItem(Screen.Orders,{onOrderClick()}, "Orders", Icons.Default.List)
                    )


                    navItems.forEach { (screen, onClick, label, icon) ->
                        BottomNavigationItem(
                            selected = currentRoute == screen.route,
                            onClick = { onClick() },
                            icon = {
                                if (screen.route == Screen.Cart.route) {
                                    // For cart icon, add badge
                                    BadgedBox(
                                        badge = {
                                            val cartItemCount by cartViewModel.cartItems.collectAsState()
                                            if (cartItemCount.isNotEmpty()) {
                                                Badge {
                                                    Text(
                                                        text = cartItemCount.size.toString(),
                                                        style = MaterialTheme.typography.caption
                                                    )
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(icon, contentDescription = label)
                                    }
                                } else {
                                    // For other icons, no badge
                                    Icon(icon, contentDescription = label)
                                }
                            },
                            label = { androidx.compose.material.Text(label) },
                            selectedContentColor = Color.White,
                            unselectedContentColor = Color.Gray
                        )
                    }
                }
                }},
            drawerContent = {
                DrawerContent(  // Reuse DrawerContent from HomeScreen
                    navController = navController,
                    scaffoldState = scaffoldState,
                    scope = scope,
                    onProfileClick = { onProfileClick() },
                    onHelpClick = { onHelpClick() },
                    onLogoutClick = { onLogoutClick() }
                )
            }
        ) { paddingValues ->
            // Your existing cart content here, with proper padding
            CartContent(
                viewModel = cartViewModel,
                onCheckout = onCheckout,
                paddingValues = paddingValues
            )
        }
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
