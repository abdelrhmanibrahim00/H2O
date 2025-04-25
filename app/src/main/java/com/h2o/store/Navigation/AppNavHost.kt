package com.h2o.store.Navigation

//import com.h2o.store.ViewModels.Admin.ManageProductsViewModel
import DeliveryHomeScreen
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.core.net.toUri
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
import com.h2o.store.Screens.Admin.AddProductScreen
import com.h2o.store.Screens.Admin.AdminDeliveryConfigScreen
import com.h2o.store.Screens.Admin.AdminDistrictsScreen
import com.h2o.store.Screens.Admin.AdminOrderDetailsScreen
import com.h2o.store.Screens.Admin.EditOrderScreen
import com.h2o.store.Screens.Admin.EditProductScreen
import com.h2o.store.Screens.Admin.EditUserScreen
import com.h2o.store.Screens.Admin.ManageOrdersScreen
import com.h2o.store.Screens.Admin.ManageProductsScreen
import com.h2o.store.Screens.Admin.ManageUsersScreen
import com.h2o.store.Screens.Admin.ProductDetailsScreen
import com.h2o.store.Screens.Admin.UserDetailsScreen
import com.h2o.store.Screens.AdminHomeScreen
import com.h2o.store.Screens.DetailedReportScreen
import com.h2o.store.Screens.EditProfileScreen
import com.h2o.store.Screens.InventoryAnalysisScreen
import com.h2o.store.Screens.LoginScreen
import com.h2o.store.Screens.OrderDeliveryScreen
import com.h2o.store.Screens.ProfileScreen
import com.h2o.store.Screens.SplashScreen
import com.h2o.store.Screens.User.CartScreenWrapper
import com.h2o.store.Screens.User.CheckoutScreenCoordinatorWrapper
import com.h2o.store.Screens.User.HomeScreen
import com.h2o.store.Screens.User.MapScreen
import com.h2o.store.Screens.User.MapScreenMode
import com.h2o.store.Screens.User.OrderDetailsScreen
import com.h2o.store.Screens.User.OrdersScreen
import com.h2o.store.Screens.User.PaymentScreenCoordinatorWrapper
import com.h2o.store.Screens.User.SignUpScreen
import com.h2o.store.Utils.LocationUtils
import com.h2o.store.ViewModels.Admin.AdminViewModel
import com.h2o.store.ViewModels.Admin.ManageOrdersViewModel
import com.h2o.store.ViewModels.Admin.ManageProductsViewModel
import com.h2o.store.ViewModels.Admin.ManageUsersViewModel
import com.h2o.store.ViewModels.Delivery.DeliveryViewModel
import com.h2o.store.ViewModels.DeliveryConfigViewModel
import com.h2o.store.ViewModels.InventoryAnalysisViewModel
import com.h2o.store.ViewModels.Location.LocationViewModel
import com.h2o.store.ViewModels.User.CartViewModel
import com.h2o.store.ViewModels.User.CheckoutCoordinatorViewModel
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
import com.h2o.store.repositories.Admin.OrderRepository
import com.h2o.store.repositories.AuthRepository
import com.h2o.store.repositories.CheckoutRepository
import com.h2o.store.repositories.InventoryPredictionRepository
import com.h2o.store.repositories.LocationRepository
import com.h2o.store.repositories.PaymentRepository
import com.h2o.store.repositories.ProfileRepository
import com.h2o.store.repositories.UserRepository

@SuppressLint("StateFlowValueCalledInComposition")
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

    // Get current user ID with logging
    val firebaseAuth = FirebaseAuth.getInstance()
    val currentUser = firebaseAuth.currentUser
    val currentUserId = currentUser?.uid ?: ""
    Log.d("AppNavHost", "Current user ID: $currentUserId") //
    // Repositories
    val authRepository = AuthRepository()
    val profileRepository = ProfileRepository()
    //user repository
    val userRepository = Graph.userRepository


    // Access repositories via Graph with lazy initialization
    val orderRepository = Graph.orderRepository

    // Product ViewModel
    val productViewModel: ProductViewModel = viewModel(
        viewModelStoreOwner = viewModelStoreOwner,
        factory = ProductViewModel.Factory(Graph.productRepository)
    )


    // Create SignUpViewModel
    val signUpViewModel: SignUpViewModel = viewModel(
        factory = SignUpViewModel.Factory(authRepository),
        viewModelStoreOwner = viewModelStoreOwner
    )

    // Profile ViewModel
    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(profileRepository),
        viewModelStoreOwner = viewModelStoreOwner
    )


    val adminViewModel: AdminViewModel = viewModel(
        factory = AdminViewModel.Factory(
            orderRepository,
            profileRepository,
            Graph.productRepository
        ),
        viewModelStoreOwner = viewModelStoreOwner
    )

    val manageOrdersViewModel: ManageOrdersViewModel = viewModel(
        factory = ManageOrdersViewModel.Factory(
            orderRepository,
            userRepository
        ),
        viewModelStoreOwner = viewModelStoreOwner
    )
    val manageusersViewModel: ManageUsersViewModel = viewModel(
        factory = ManageUsersViewModel.Factory(
            userRepository
        ),
        viewModelStoreOwner = viewModelStoreOwner
    )
    val manageProductViewModel: ManageProductsViewModel = viewModel(
        factory = ManageProductsViewModel.Factory(
            Graph.productRepository,
        ),
        viewModelStoreOwner = viewModelStoreOwner
    )
    // 2. Create a coordinator ViewModel in the AppNavHost function
    val checkoutCoordinatorViewModel: CheckoutCoordinatorViewModel = viewModel(
        factory = CheckoutCoordinatorViewModel.Factory(
            userId = currentUserId,
            checkoutRepository = CheckoutRepository(),
            paymentRepository = PaymentRepository(),
            userRepository = UserRepository()
        ),
        viewModelStoreOwner = viewModelStoreOwner
    )

    val onHelpClick: () -> Unit = {
        // Open phone dialer with the help number
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = "tel:+201228279254".toUri()
        }
        context.startActivity(intent)
    }

    val onLogoutClick: () -> Unit = {
        // Logout the user and navigate to login screen
        authRepository.logoutUser {
            // Navigate first, then clear ViewModels
            navController.navigate(Screen.Login.route)
        }
    }

    // DeliveryConfigViewModel creation
    val deliveryConfigViewModel: DeliveryConfigViewModel = viewModel(
        factory = DeliveryConfigViewModel.Factory(),
        viewModelStoreOwner = viewModelStoreOwner
    )


    // Analytics ViewModel

    val inventoryPredictionRepository = InventoryPredictionRepository()
    val inventoryAnalysisViewModel: InventoryAnalysisViewModel = viewModel(
        factory = InventoryAnalysisViewModel.Factory(inventoryPredictionRepository)
    )

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
                  //  navController.navigate(Screen.Map.route)
                    // From SignUpScreen - for new user location
                    navController.navigate(Screen.Map.createRoute("signup"))
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
                viewModel = signUpViewModel ,
                onBackPressed = {navController.popBackStack()}
            )
        }

        composable(
            route = Screen.Map.route,
            arguments = listOf(
                navArgument("mode") {
                    type = NavType.StringType
                    defaultValue = "signup"
                    nullable = true
                }
            )
        ) { backStackEntry ->
            // Extract the mode parameter
            val mode = backStackEntry.arguments?.getString("mode") ?: "signup"

            // Import the MapScreenMode enum from your MapScreen file
            val mapMode = if (mode == "edit") MapScreenMode.EDIT_PROFILE else MapScreenMode.SIGNUP

            MapScreen(
                locationViewModel = locationViewModel,
                onLocationSelected = { locationData: LocationData, addressData: AddressData ->
                    // Handle differently based on mode
                    if (mode == "signup") {
                        // Update SignUpViewModel
                        signUpViewModel.updateLocationAndAddress(locationData, addressData)
                    } else if (mode == "edit") {
                        // Update profile - using the correct method name
                        profileViewModel.updateLocationAndAddress(locationData, addressData)
                        // After updating the location, you might want to save the changes to the database
                        profileViewModel.updateLocation()
                    }
                    navController.popBackStack()
                },
                onBackPressed = {
                    navController.popBackStack()
                },
                // Use the corrected mapMode variable
                mode = mapMode,
                // Use the correct property name from ProfileViewModel
                initialLocation = if (mode == "edit") profileViewModel.locationData.value else null
            )
        }

        composable(Screen.Home.route) {
            val currentUser1 = FirebaseAuth.getInstance().currentUser
            val userId = currentUser1?.uid ?: ""
            val cartViewModel: CartViewModel = viewModel(factory = CartViewModel.Factory(userId))
            HomeScreen(
                productViewModel = productViewModel,
                navController = navController,
                onHomeClick = { navController.navigate(Screen.Home.route) },
                onCartClick = { navController.navigate(Screen.Cart.route) },
                onOrderClick = { navController.navigate(Screen.Orders.route) },
                // Need to create a user-specific CartViewModel here too
                cartViewModel = cartViewModel,
                onProfileClick = { navController.navigate(Screen.Profile.route) },
                onHelpClick = onHelpClick,
                onLogoutClick = onLogoutClick
            )
        }
        // Cart Screen Composable in NavHost
        composable(Screen.Cart.route) {
            // Use the CartScreenWrapper instead of CartScreen
            val currentUser2 = FirebaseAuth.getInstance().currentUser
            val userId = currentUser2?.uid ?: ""
            val cartViewModel: CartViewModel = viewModel(factory = CartViewModel.Factory(userId))
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

        // Checkout Screen Composable in NavHost
        // 3. Replace the CheckoutScreenWrapper composable with:
        composable(Screen.Checkout.route) {
            // Get current user ID from Firebase Auth
            val currentUser = FirebaseAuth.getInstance().currentUser
            val userId = currentUser?.uid ?: ""

            // Create CartViewModel for this screen
            val cartViewModel: CartViewModel = viewModel(factory = CartViewModel.Factory(userId))

            CheckoutScreenCoordinatorWrapper(
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
                onEditAddress = {
                    navController.navigate(Screen.EditProfile.route)
                },
                onPaymentProcess = { orderId, totalAmount ->
                    // Navigate to payment screen with order ID and amount
                    navController.navigate(
                        Screen.Payment.createRoute(orderId, totalAmount.toFloat(), true)
                    )
                }, cartViewModel = cartViewModel,
                coordinatorViewModel = checkoutCoordinatorViewModel
            )
        }
        // Payment Screen Composable in NavHost
        composable(
            route = Screen.Payment.route,
            arguments = listOf(
                navArgument("orderId") {
                    type = NavType.StringType
                },
                navArgument("totalAmount") {
                    type = NavType.FloatType
                },
                navArgument("hasAddress") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            val totalAmount = backStackEntry.arguments?.getFloat("totalAmount")?.toDouble() ?: 0.0

            PaymentScreenCoordinatorWrapper(
                orderId = orderId,
                totalAmount = totalAmount,
                onPaymentSuccess = {
                    // Navigate to home screen after successful payment
                    navController.navigate(Screen.Home.route) {
                        // Clear back stack up to home to prevent going back to payment screen
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onPaymentFailure = {
                    // Navigate back to checkout screen
                    navController.popBackStack()
                } ,
                onBackClick = {navController.popBackStack()
                } ,
                coordinatorViewModel = checkoutCoordinatorViewModel
            )
        }

        //  Orders Screen
        composable(Screen.Orders.route) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val userId = currentUser?.uid ?: ""

            // Orders ViewModel
            val ordersViewModel: OrdersViewModel = viewModel(
                factory = OrdersViewModel.Factory(userId, orderRepository),
                viewModelStoreOwner = viewModelStoreOwner
            )

            // Cart ViewModel (needed for MainScaffold)
            val cartViewModel: CartViewModel = viewModel(
                factory = CartViewModel.Factory(userId)
            )

            OrdersScreen(
                navController = navController,
                ordersViewModel = ordersViewModel,
                cartViewModel = cartViewModel,
                onBackClick = { /* Not used anymore */ },
                onOrderDetails = { orderId ->
                    navController.navigate(Screen.OrderDetails.createRoute(orderId))
                },
                onHomeClick = { navController.navigate(Screen.Home.route) },
                onCartClick = { navController.navigate(Screen.Cart.route) },
                onOrderClick = { /* Already on Orders screen */ },
                onProfileClick = { navController.navigate(Screen.Profile.route) },
                onHelpClick = onHelpClick,
                onLogoutClick = onLogoutClick
            )
        }



        composable(Screen.Profile.route) {
            ProfileScreen(
                viewModel = profileViewModel,
                navController = navController,
                onEditProfile = {
                    navController.navigate(Screen.EditProfile.route)
                }
            )
        }

        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                viewModel = profileViewModel,
                navController = navController,
                onNavigateToMap = { initialLocation, onLocationSelected ->
                    // Store callback in LocationViewModel or use another approach to pass it
                    // For this example, we'll store it in a simple way
                    locationViewModel.storeLocationCallback(onLocationSelected)

                    // Navigate to map screen with edit_profile mode
                  //  navController.navigate(Screen.Map.route)
                    // From ProfileScreen - for editing existing location
                    navController.navigate(Screen.Map.createRoute("edit"))
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
        ) {
            backStackEntry ->
            // Use the CartScreenWrapper instead of CartScreen
            val currentUser3 = FirebaseAuth.getInstance().currentUser
            val userId = currentUser3?.uid ?: ""
            // Orders ViewModel
            val ordersViewModel: OrdersViewModel = viewModel(
                factory = OrdersViewModel.Factory(userId, orderRepository),
                viewModelStoreOwner = viewModelStoreOwner
            )
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
                    navController.navigate(Screen.ManageProducts.route)
                },
                onManageOrders = {
                    navController.navigate(Screen.ManageOrders.route)
                },
                onManageUsers = {
                    navController.navigate(Screen.ManageUsers.route)
                },
                onViewReports = {
                    navController.navigate(Screen.InventoryAnalysis.route)
                },
                onLogoutClick = onLogoutClick,
                onOrderSelected = { orderId ->
                    navController.navigate(Screen.AdminOrderDetails.createRoute(orderId))
                },
                onManageDeliveryConfig = {
                    navController.navigate(Screen.DeliveryConfig.route)
                }
            )
        }

         // Manage Orders Screen
        composable(Screen.ManageOrders.route) {
            ManageOrdersScreen(
                navController = navController,
                viewModel =manageOrdersViewModel,
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
                viewModel = manageOrdersViewModel,
                orderId = orderId,
                onBackClick = {
                    navController.popBackStack()
                },onEditClick = {orderId->
                    manageOrdersViewModel.resetEditOrderResult()
                    navController.navigate(Screen.EditOrder.createRoute(orderId))}
            )
        }
        // Then add this composable to your NavHost in AppNavHost.kt:
        composable(
            route = Screen.EditOrder.route,
            arguments = listOf(
                navArgument("orderId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""

            EditOrderScreen(
                navController = navController,
                viewModel = manageOrdersViewModel,
                orderId = orderId,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Manage Users Screen
        composable(Screen.ManageUsers.route) {
            ManageUsersScreen(
                navController = navController,
                viewModel = manageusersViewModel,
                onUserDetails = { userId ->
                    navController.navigate(Screen.UserDetails.createRoute(userId))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

// User Details Screen
        composable(
            route = Screen.UserDetails.route,
            arguments = listOf(
                navArgument("userId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""

            UserDetailsScreen(
                navController = navController,
                viewModel = manageusersViewModel,
                userId = userId,
                onEditUser = { id ->
                    navController.navigate(Screen.EditUser.createRoute(id))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

// Edit User Screen
        composable(
            route = Screen.EditUser.route,
            arguments = listOf(
                navArgument("userId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""

            EditUserScreen(
                navController = navController,
                viewModel = manageusersViewModel,
                userId = userId,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }


       // Manage Products Screen
        composable(Screen.ManageProducts.route) {
            ManageProductsScreen(
                navController = navController,
                viewModel = manageProductViewModel,
                onProductDetails = { productId ->
                    navController.navigate(Screen.ProductDetails.createRoute(productId))
                },
                onBackClick = {
                    navController.popBackStack()
                },
                onAddProduct = {
                    // Reset any previous add result state
                    manageProductViewModel.resetAddProductResult()
                    navController.navigate(Screen.AddProduct.route)
                }
            )
        }

// Product Details Screen
        composable(
            route = Screen.ProductDetails.route,
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""

            ProductDetailsScreen(
                navController = navController,
                viewModel = manageProductViewModel,
                productId = productId,
                onEditProduct = { id ->
                    navController.navigate(Screen.EditProduct.createRoute(id))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

// Edit Product Screen
        composable(
            route = Screen.EditProduct.route,
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""

            EditProductScreen(
                navController = navController,
                viewModel = manageProductViewModel,
                productId = productId,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        // Add this to the NavHost composable in AppNavHost.kt, after the ManageProducts composable

// Add Product Screen
        composable(Screen.AddProduct.route) {
            AddProductScreen(
                navController = navController,
                viewModel = manageProductViewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.DeliveryHome.route) {
            // Get current user ID from Firebase Auth
            val currentUser3 = FirebaseAuth.getInstance().currentUser
            val userId = currentUser3?.uid ?: ""

            Log.d("Navigation", "Setting up DeliveryViewModel with userId: $userId")

            // Create the ViewModel with the user ID
            val deliveryViewModel: DeliveryViewModel = viewModel(
                factory = DeliveryViewModel.Factory(
                    orderRepository = OrderRepository(),
                    userId = userId
                )
            )

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
        // Add the OrderDeliveryScreen route
        composable(
            route = "orderDelivery/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) {
            backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            // Get current user ID from Firebase Auth
            val currentUser3 = FirebaseAuth.getInstance().currentUser
            val userId = currentUser3?.uid ?: ""
            // Create the ViewModel
            val deliveryViewModel: DeliveryViewModel = viewModel(
                factory = DeliveryViewModel.Factory(
                    orderRepository = OrderRepository(),
                    userId = userId
                )
            )

            // Render the OrderDeliveryScreen
            OrderDeliveryScreen(
                navController = navController,
                viewModel = deliveryViewModel,
                orderId = orderId
            )
        }

        composable(Screen.InventoryAnalysis.route) {
            InventoryAnalysisScreen(
                navController = navController,
                viewModel = inventoryAnalysisViewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                onViewDetailedReport = {
                    // Navigate to the detailed report screen
                    navController.navigate(Screen.InventoryReportDetail.route)
                }
            )
        }

        composable(Screen.InventoryReportDetail.route) {
            DetailedReportScreen(
                viewModel = inventoryAnalysisViewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }


        // Delivery Config Screen
        composable(Screen.DeliveryConfig.route) {
            AdminDeliveryConfigScreen(
                viewModel = deliveryConfigViewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                onManageDistricts = {
                    navController.navigate(Screen.ManageDistricts.route)
                }
            )
        }

// Manage Districts Screen
        composable(Screen.ManageDistricts.route) {
            AdminDistrictsScreen(
                viewModel = deliveryConfigViewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }


    }
}

