package com.h2o.store.components

import androidx.compose.foundation.layout.Arrangement
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
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Badge
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.BadgedBox
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.BottomNavigation
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.ExperimentalMaterialApi
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Icon
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.IconButton
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.MaterialTheme
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.ModalBottomSheetLayout
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.ModalBottomSheetState
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.ModalBottomSheetValue
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Scaffold
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.ScaffoldState
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Text
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.TextButton
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.rememberModalBottomSheetState
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.h2o.store.Navigation.Screen
import com.h2o.store.ViewModels.User.CartViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Main scaffold with drawer, bottom sheet, and bottom navigation
 * Used for main screens in the app
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MainScaffold(
    navController: NavHostController,
    cartViewModel: CartViewModel,
    title: String = "H2O Store",
    onHomeClick: () -> Unit,
    onCartClick: () -> Unit,
    onOrderClick: () -> Unit,
    onProfileClick: () -> Unit,
    onHelpClick: () -> Unit,
    onLogoutClick: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    // Initialize necessary components
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()

    // Get current navigation state
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

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
                AppTopBar(
                    title = title,
                    scaffoldState = scaffoldState,
                    scope = scope,
                    modalBottomSheetState = modalBottomSheetState
                )
            },
            bottomBar = {
                AppBottomBar(
                    navController = navController,
                    currentRoute = currentRoute,
                    cartViewModel = cartViewModel,
                    onHomeClick = onHomeClick,
                    onCartClick = onCartClick,
                    onOrderClick = onOrderClick
                )
            },
            drawerContent = {
                DrawerContent(
                    navController = navController,
                    scaffoldState = scaffoldState,
                    scope = scope,
                    onProfileClick = onProfileClick,
                    onHelpClick = onHelpClick,
                    onLogoutClick = onLogoutClick
                )
            }
        ) { paddingValues ->
            content(paddingValues)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun AppTopBar(
    title: String,
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    modalBottomSheetState: ModalBottomSheetState
) {
    Surface(
        modifier = Modifier.statusBarsPadding() // Specific padding for status bar
    ) {
        TopAppBar(
            title = { Text(text = title) },
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
}

@Composable
private fun AppBottomBar(
    navController: NavController,
    currentRoute: String?,
    cartViewModel: CartViewModel,
    onHomeClick: () -> Unit,
    onCartClick: () -> Unit,
    onOrderClick: () -> Unit
) {
    Surface(
        modifier = Modifier.navigationBarsPadding() // Specific padding for navigation bar
    ) {
        BottomNavigation {
            val navItems = listOf(
                BottomNavItem(Screen.Home, { onHomeClick() }, "Home", Icons.Default.Home),
                BottomNavItem(Screen.Cart, { onCartClick() }, "Cart", Icons.Default.ShoppingCart),
                BottomNavItem(Screen.Orders, { onOrderClick() }, "Orders", Icons.Default.List)
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
}

@Composable
fun DrawerContent(
    navController: NavController,
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    onProfileClick: () -> Unit,
    onHelpClick: () -> Unit,
    onLogoutClick: () -> Unit
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
            DrawerItem(Screen.Profile, { onProfileClick() }, "Profile", Icons.Default.Person),
            DrawerItem(Screen.Help, { onHelpClick() }, "Help", Icons.Default.Call),
            DrawerItem(Screen.Login, { onLogoutClick() }, "Log out", Icons.Default.Logout)
        )

        menuItems.forEach { (screen, onclick, label, icon) ->
            if (label == "Log out") {
                // Use the LogoutButton for the logout action
                LogoutButton(
                    onClick = {
                        scope.launch {
                            scaffoldState.drawerState.close()
                            onLogoutClick()
                        }
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Log out",
                            modifier = Modifier.padding(end = 24.dp)
                        )
                        Text(
                            text = "Log out",
                            style = MaterialTheme.typography.body1
                        )
                    }
                }
            } else {
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

// Data classes for the bottom navigation and drawer items
data class BottomNavItem(
    val screen: Screen,
    val onClick: () -> Unit,
    val label: String,
    val icon: ImageVector,
)

data class DrawerItem(
    val screen: Screen,
    val onClick: () -> Unit,
    val label: String,
    val icon: ImageVector,
)