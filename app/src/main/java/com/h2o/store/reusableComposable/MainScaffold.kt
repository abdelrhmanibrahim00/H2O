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
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Converted to use Material3 components
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Get current navigation state
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // State for bottom sheet
    var showBottomSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // Apply the system UI controllers to match theme
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    DrawerContent(
                        navController = navController,
                        drawerState = drawerState,
                        scope = scope,
                        onProfileClick = onProfileClick,
                        onHelpClick = onHelpClick,
                        onLogoutClick = onLogoutClick
                    )
                }
            }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    AppTopBar(
                        title = title,
                        drawerState = drawerState,
                        scope = scope,
                        onShowBottomSheet = { showBottomSheet = true }
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
                containerColor = MaterialTheme.colorScheme.background
            ) { paddingValues ->
                content(paddingValues)

                // Bottom sheet
                if (showBottomSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showBottomSheet = false },
                        sheetState = bottomSheetState
                    ) {
                        BottomSheetContent()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    title: String,
    drawerState: DrawerState,
    scope: CoroutineScope,
    onShowBottomSheet: () -> Unit
) {
    // Add modifier.statusBarsPadding() to properly align with status bar
    TopAppBar(
        title = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onPrimary
            )
        },
        navigationIcon = {
            IconButton(onClick = {
                scope.launch { drawerState.open() }
            }) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        actions = {
            IconButton(onClick = { onShowBottomSheet() }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
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
        modifier = Modifier.navigationBarsPadding(), // Specific padding for navigation bar
        color = MaterialTheme.colorScheme.primary
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            val navItems = listOf(
                BottomNavItem(Screen.Home, { onHomeClick() }, "Home", Icons.Default.Home),
                BottomNavItem(Screen.Cart, { onCartClick() }, "Cart", Icons.Default.ShoppingCart),
                BottomNavItem(Screen.Orders, { onOrderClick() }, "Orders", Icons.Default.List)
            )

            navItems.forEach { (screen, onClick, label, icon) ->
                NavigationBarItem(
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
                                            Text(text = cartItemCount.size.toString())
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
                    colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                        selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                        unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerContent(
    navController: NavController,
    drawerState: DrawerState,
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
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .padding(bottom = 24.dp)
                .padding(start = 8.dp)
        )

        // Profile button
        TextButton(
            onClick = {
                scope.launch {
                    onProfileClick()
                    drawerState.close()
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
                    Icons.Default.Person,
                    contentDescription = "Profile",
                    modifier = Modifier.padding(end = 24.dp)
                )
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // Help button
        TextButton(
            onClick = {
                scope.launch {
                    onHelpClick()
                    drawerState.close()
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
                    Icons.Default.Call,
                    contentDescription = "Help",
                    modifier = Modifier.padding(end = 24.dp)
                )
                Text(
                    text = "Help",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // Logout button
        TextButton(
            onClick = {
                scope.launch {
                    drawerState.close()
                    onLogoutClick()
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
                    Icons.Default.Logout,
                    contentDescription = "Log out",
                    modifier = Modifier.padding(end = 24.dp)
                )
                Text(
                    text = "Log out",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun BottomSheetContent() {
    Column(
        Modifier
            .padding(start = 16.dp, end = 32.dp, top = 24.dp, bottom = 36.dp)
            .navigationBarsPadding() // Shifts content above navigation bar
    ) {
        Text(
            "Options",
            style = MaterialTheme.typography.headlineMedium,
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
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // Add extra padding at bottom to ensure all content is above navigation bar
        Spacer(modifier = Modifier.height(48.dp))
    }
}

// Data class for the bottom navigation items
data class BottomNavItem(
    val screen: Screen,
    val onClick: () -> Unit,
    val label: String,
    val icon: ImageVector,
)