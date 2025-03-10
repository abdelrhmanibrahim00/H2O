package com.h2o.store.Screens

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Badge
import androidx.compose.material.BadgedBox
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.h2o.store.Models.CartViewModel
import com.h2o.store.Models.ProductViewModel
import com.h2o.store.Navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(productViewModel: ProductViewModel,cartViewModel: CartViewModel, navController: NavHostController, onCartClick : () -> Unit,
               onHomeClick : () -> Unit, onOrderClick : () -> Unit , onProfileClick: () -> Unit,onHelpClick: () -> Unit,onLogoutClick: () -> Unit
) {
    // Initialize necessary components
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()
   // val navController = rememberNavController()



    // Get current navigation state
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Main screen title
    val title = remember { mutableStateOf("H2O Store") }

    // Remember modal bottom sheet state
    val modalBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden
    )

    ModalBottomSheetLayout(
        sheetState = modalBottomSheetState,
        sheetContent = {
            BottomSheetContent()
        }
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(), // This handles both top and bottom system bars
            scaffoldState = scaffoldState,
            topBar = {
                Surface(
                    modifier = Modifier.statusBarsPadding() // Specific padding for status bar
                ) {
                    TopAppBar(
                        title = { Text(text = title.value) },
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
            },
            bottomBar = {
                Surface(
                    modifier = Modifier.navigationBarsPadding() // Specific padding for navigation bar
                ) {
                    BottomNavigation {
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
                                label = { Text(label) },
                                selectedContentColor = Color.White,
                                unselectedContentColor = Color.Gray
                            )
                        }
                    }
                }
            },
            drawerContent = {
                DrawerContent(
                    navController = navController,
                    scaffoldState = scaffoldState,
                    scope = scope,
                    onProfileClick = { onProfileClick() },
                    onHelpClick = { onHelpClick() },
                    onLogoutClick = { onLogoutClick() }
                )
            }
        ) { paddingValues ->
            MainContent(cartViewModel=cartViewModel,
                productViewModel = productViewModel,
                paddingValues = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding(),
                    start = 16.dp,
                    end = 16.dp
                )
            )
        }
    }
    }

data class ScreenItem(
    val screen: Screen,
    val onClick: () -> Unit,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun DrawerContent(
    navController: NavController,
    scaffoldState: ScaffoldState,
    scope: CoroutineScope ,
    onProfileClick :()-> Unit,
    onHelpClick :()-> Unit,
    onLogoutClick :()-> Unit
) {
    Column(
        Modifier
            .padding(start = 24.dp, end = 24.dp, top = 32.dp)
            .statusBarsPadding()  // Accounts for status bar
    ) {
        Text(
            "Menu",
            style = MaterialTheme.typography.h6,
            modifier = Modifier
                .padding(bottom = 24.dp)
                .padding(start = 8.dp)
        )

        val menuItems = listOf(
            ScreenItem(Screen.Profile,{onProfileClick()},"Profile", Icons.Default.Person) ,
            ScreenItem(Screen.Help,{onHelpClick()},"Help", Icons.Default.Call),
            ScreenItem(Screen.Login,{onLogoutClick()},"Log out", Icons.Default.Logout)
        )

        menuItems.forEach { (screen,onclick,label, icon) ->
            TextButton(
                onClick = {
                    scope.launch {
                        onclick()
                        scaffoldState.drawerState.close()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Icon(
                        icon,
                        contentDescription = label,
                        modifier = Modifier.padding(end = 24.dp)
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.body1
                    )
                }
            }
        }
    }
}

@Composable
fun BottomSheetContent() {
    Column(
        Modifier
            .padding(start = 16.dp, end = 32.dp, top = 24.dp)
            .navigationBarsPadding() // Shifts content above navigation bar
    ) {
        Text(
            "Options",
            style = MaterialTheme.typography.h6,
            modifier = Modifier
                .padding(bottom = 32.dp)
                .padding(start = 8.dp)
        )

        val options = listOf(
            Pair("Settings", Icons.Default.Settings),
            Pair("Share", Icons.Default.Share),
            Pair("Help", Icons.Default.Help)
        )

        options.forEach { (label, icon) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    modifier = Modifier.padding(end = 24.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.body1
                )
            }
        }

        // Add extra padding at bottom to ensure all content is above navigation bar
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun MainContent(cartViewModel: CartViewModel,
    productViewModel: ProductViewModel,
    paddingValues: PaddingValues
) {
    val productState by productViewModel.productState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        when {
            productState.loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            productState.error != null -> {
                Text(
                    text = "Error: ${productState.error}",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
            productState.products.isEmpty() -> {
                Text(
                    text = "No products available",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(productState.products) { product ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = 4.dp
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                // Image
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(product.Image)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = product.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentScale = ContentScale.Fit,
                                    placeholder = rememberVectorPainter(Icons.Default.Image),
                                    error = rememberVectorPainter(Icons.Default.BrokenImage)
                                )

                                Spacer(Modifier.height(16.dp))

                                // Product Name
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.h6,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(Modifier.height(8.dp))

                                // Price and Add to Cart
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        if (product.discount) {
                                            Text(
                                                text = "${product.price_after_discount} EGP",
                                                style = MaterialTheme.typography.body1,
                                                color = MaterialTheme.colors.primary
                                            )
                                            Text(
                                                text = "${product.price} EGP",
                                                style = MaterialTheme.typography.body2,
                                                color = Color.Gray,
                                                textDecoration = TextDecoration.LineThrough
                                            )
                                        } else {
                                            Text(
                                                text = "${product.price} EGP",
                                                style = MaterialTheme.typography.body1
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { cartViewModel.addToCart(product) }
                                    ) {
                                        Icon(
                                            Icons.Default.AddShoppingCart,
                                            contentDescription = "Add to Cart"
                                        )
                                    }
                                }

                                Spacer(Modifier.height(4.dp))

                                // Stock Status
                                Text(
                                    text = "In Stock: ${product.quantity}",
                                    style = MaterialTheme.typography.caption,
                                    color = if (product.stock > 0) Color.Green else Color.Red
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


