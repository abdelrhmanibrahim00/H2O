package com.h2o.store.Navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.h2o.store.Graph.Graph
import com.h2o.store.Screens.Admin.AdminOrderDetailsScreen
import com.h2o.store.Screens.Admin.ManageOrdersScreen
import com.h2o.store.Screens.AdminHomeScreen
import com.h2o.store.Screens.DeliveryHomeScreen
import com.h2o.store.Screens.EditProfileScreen
import com.h2o.store.Screens.LoginScreen
import com.h2o.store.Screens.ProfileScreen
import com.h2o.store.Screens.SplashScreen
import com.h2o.store.Screens.User.CartScreenWrapper
import com.h2o.store.Screens.User.CheckoutScreenWrapper
import com.h2o.store.Screens.User.HomeScreen
import com.h2o.store.Screens.User.MapScreen
import com.h2o.store.Screens.User.OrderDetailsScreen
import com.h2o.store.Screens.User.OrdersScreen
import com.h2o.store.Screens.User.SignUpScreen
import com.h2o.store.Utils.LocationUtils
import com.h2o.store.ViewModels.Admin.AdminViewModel
import com.h2o.store.ViewModels.Admin.ManageOrdersViewModel
import com.h2o.store.ViewModels.Location.LocationViewModel
import com.h2o.store.ViewModels.User.CartViewModel
import com.h2o.store.ViewModels.User.DeliveryViewModel
import com.h2o.store.ViewModels.User.OrdersViewModel
import com.h2o.store.ViewModels.User.ProductViewModel
import com.h2o.store.ViewModels.User.ProfileViewModel
import com.h2o.store.ViewModels.User.SignUpViewModel
import com.h2o.store.data.models.AddressData
import com.h2o.store.data.models.LocationData
import com.h2o.store.data.remote.RetrofitClient
import com.h2o.store.domain.usecases.GetAddressFromCoordinates
import com.h2o.store.domain.usecases.GetCoordinatesFromAddress
import com.h2o.store.domain.usecases.GetPlacePredictions
import com.h2o.store.repositories.AuthRepository
import com.h2o.store.repositories.LocationRepository
import com.h2o.store.repositories.ProfileRepository

@Composable
fun AppNavHost(navController: NavHostController, context: Context) {

    // Location related initialization
    val locationUtils = LocationUtils(context)
    val apiService = RetrofitClient.create()
    val locationRepository = LocationRepository(apiService)
    val getAddressFromCoordinates = GetAddressFromCoordinates(locationRepository)
    val getCoordinatesFromAddress = GetCoordinatesFromAddress(locationRepository)
    val getPlacePredictions = GetPlacePredictions(locationRepository)
    val locationViewModel = LocationViewModel(
        getAddressFromCoordinates = getAddressFromCoordinates,
        getCoordinatesFromAddress = getCoordinatesFromAddress,
        getPlacePredictions = getPlacePredictions
    )

    // Shared ViewModel store owner
    val viewModelStoreOwner = navController.currentBackStackEntry?.destination?.parent as? ViewModelStoreOwner
        ?: LocalViewModelStoreOwner.current!!

    // Get current user ID
// Get current user ID with logging
    val firebaseAuth = FirebaseAuth.getInstance()
    val currentUser = firebaseAuth.currentUser
    val currentUserId = currentUser?.uid ?: ""
    Log.d("AppNavHost", "Current user ID: $currentUserId") //
    // Repositories
    val authRepository = AuthRepository()
    val userRepository = ProfileRepository()

    // Access repositories via Graph with lazy initialization
    val orderRepository = Graph.orderRepository

    // Product ViewModel
    val productViewModel: ProductViewModel = viewModel(viewModelStoreOwner = viewModelStoreOwner)

    // Create SignUpViewModel
    val signUpViewModel: SignUpViewModel = viewModel(
        factory = SignUpViewModel.Factory(authRepository),
        viewModelStoreOwner = viewModelStoreOwner
    )

    // Profile ViewModel
    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(userRepository),
        viewModelStoreOwner = viewModelStoreOwner
    )

    // Orders ViewModel
    val ordersViewModel: OrdersViewModel = viewModel(
        factory = OrdersViewModel.Factory(currentUserId, orderRepository),
        viewModelStoreOwner = viewModelStoreOwner
    )
    val adminViewModel: AdminViewModel = viewModel(
        factory = AdminViewModel.Factory(
            orderRepository,
            userRepository,
            Graph.productRepository
        ),
        viewModelStoreOwner = viewModelStoreOwner
    )
    // Initialize delivery view model for delivery personnel
    val deliveryViewModel: DeliveryViewModel = viewModel(
        factory = DeliveryViewModel.Factory(orderRepository),
        viewModelStoreOwner = viewModelStoreOwner
    )
    // Create user-specific CartViewModel using the Factory
    val cartViewModel: CartViewModel = viewModel(
        factory = CartViewModel.Factory(currentUserId)
    )

    val onHelpClick: () -> Unit = {
        // Open phone dialer with the help number
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:+201228279254")
        }
        context.startActivity(intent)
    }

    val onLogoutClick: () -> Unit = {
        // Logout the user and navigate to login screen
        authRepository.logoutUser {
            // This callback runs after logout is complete
            navController.navigate(Screen.Login.route) {
                // Clear the back stack so user can't go back after logout
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController) {
                navController.navigate(Screen.Login.route)
            }
        }

        composable(
            route = Screen.Login.route + "?email={email}",
            arguments = listOf(
                navArgument("email") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            // Extract email from navigation arguments
            val email = backStackEntry.arguments?.getString("email")

            LoginScreen(
                navController = navController,
                onLoginSuccess = { role ->
                    // Navigate based on user role
                    when (role.lowercase()) {
                        "admin" -> navController.navigate(Screen.AdminHome.route)
                        "delivery" -> navController.navigate(Screen.DeliveryHome.route)
                        else -> navController.navigate(Screen.Home.route)
                    }
                },
                onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                prefilledEmail = email
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                navController = navController,
                context = context,
                OnNavigateToMap = {
                    navController.navigate(Screen.Map.route)
                },
                locationUtils = locationUtils,
                onSignUpSuccess = {
                    // Navigate to login screen after signup and pass the email
                    navController.navigate(Screen.Login.route + "?email=${signUpViewModel.email.value}") {
                        // Remove SignUp screen from back stack
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                },
                locationViewModel = locationViewModel,
                viewModel = signUpViewModel
            )
        }

        composable(Screen.Map.route) {
            MapScreen(
                locationViewModel = locationViewModel,
                onLocationSelected = { locationData: LocationData, addressData: AddressData ->
                    // Update the SignUpViewModel with selected location data
                    signUpViewModel.updateLocationAndAddress(locationData, addressData)
                    navController.popBackStack()
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                productViewModel = productViewModel,
                navController = navController,
                onHomeClick = { navController.navigate(Screen.Home.route) },
                onCartClick = { navController.navigate(Screen.Cart.route) },
                onOrderClick = { navController.navigate(Screen.Orders.route) },
                // Need to create a user-specific CartViewModel here too
                cartViewModel = viewModel(factory = CartViewModel.Factory(currentUserId)),
                onProfileClick = { navController.navigate(Screen.Profile.route) },
                onHelpClick = onHelpClick,
                onLogoutClick = onLogoutClick
            )
        }

        composable(Screen.Cart.route) {
            // Use the CartScreenWrapper instead of CartScreen
            CartScreenWrapper(
                navController = navController,
                onHomeClick = { navController.navigate(Screen.Home.route) },
                onCartClick = {},
                onOrderClick = { navController.navigate(Screen.Orders.route) },
                onCheckout = {
                    // Navigate to checkout screen instead of login
                    navController.navigate(Screen.Checkout.route)
                },
                onProfileClick = { navController.navigate(Screen.Profile.route) },
                onHelpClick = onHelpClick,
                onLogoutClick = onLogoutClick ,
                cartViewModel = cartViewModel
            )
        }

        // Add the checkout screen route
        composable(Screen.Checkout.route) {
            CheckoutScreenWrapper(
                navController = navController,
                onPlaceOrder = {
                    // After successful order placement, navigate back to home
                    navController.navigate(Screen.Home.route) {
                        // Clear the back stack up to Home to prevent going back to checkout
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onBackClick = {
                    // Go back to cart when back button is pressed
                    navController.popBackStack()
                },
                onEditAddress = {}
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                viewModel = profileViewModel,
                navController = navController,
                onEditProfile = { navController.navigate(Screen.EditProfile.route) }
            )
        }

        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                viewModel = profileViewModel,
                navController = navController
            )
        }

        // Add Orders Screen route
        composable(Screen.Orders.route) {
            OrdersScreen(
                navController = navController,
                ordersViewModel = ordersViewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                onOrderDetails = { orderId ->
                    navController.navigate(Screen.OrderDetails.createRoute(orderId))
                }
            )
        }

        // Add Order Details Screen route with parameter
        composable(
            route = Screen.OrderDetails.route,
            arguments = listOf(
                navArgument("orderId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""

            OrderDetailsScreen(
                navController = navController,
                ordersViewModel = ordersViewModel,
                orderId = orderId,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Admin Home Screen
        composable(Screen.AdminHome.route) {
            AdminHomeScreen(
                navController = navController,
                adminViewModel = adminViewModel,
                onManageProducts = {
                    // This would navigate to a product management screen when implemented
                },
                onManageOrders = {
                    navController.navigate(Screen.ManageOrders.route)
                },
                onManageUsers = {
                    // This would navigate to a user management screen when implemented
                },
                onViewReports = {
                    // This would navigate to a reporting screen when implemented
                },
                onLogoutClick = onLogoutClick
            )
        }

// Manage Orders Screen
        composable(Screen.ManageOrders.route) {
            ManageOrdersScreen(
                navController = navController,
                viewModel = viewModel(
                    factory = ManageOrdersViewModel.Factory(
                        orderRepository = orderRepository
                    )
                ),
                onOrderDetails = { orderId ->
                    navController.navigate(Screen.AdminOrderDetails.createRoute(orderId))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

// Admin Order Details Screen
        composable(
            route = Screen.AdminOrderDetails.route,
            arguments = listOf(
                navArgument("orderId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""

            AdminOrderDetailsScreen(
                navController = navController,
                viewModel = viewModel(
                    factory = ManageOrdersViewModel.Factory(
                        orderRepository = orderRepository
                    )
                ),
                orderId = orderId,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Delivery Home Screen
        composable(Screen.DeliveryHome.route) {
            DeliveryHomeScreen(
                navController = navController,
                viewModel = deliveryViewModel,
                onOrderSelected = { orderId ->
                    navController.navigate(Screen.OrderDetails.createRoute(orderId))
                },
                onProfileClick = {
                    navController.navigate(Screen.Profile.route)
                },
                onLogoutClick = onLogoutClick
            )
        }
    }
}