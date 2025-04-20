package com.h2o.store.Navigation

import androidx.annotation.DrawableRes
import com.h2o.store.R

sealed class Screen(val title: String = "", val route: String) {
    // Existing authentication and setup screens
    object Splash : Screen(route = "splash")
    object SignUp : Screen(route = "signup")
    object Login : Screen(route = "login")
    object Map : Screen(route = "map?mode={mode}") {
        // Add a function to create the route with mode parameter
        fun createRoute(mode: String = "signup") = "map?mode=$mode"
    }
    object Home : Screen(route = "home")
    object Cart : Screen(route = "cart")
    object Profile : Screen(route = "profile")
    object EditProfile : Screen(route = "edit_profile")
    object Help : Screen(route = "help")
    object Orders : Screen(route="orders")
    object OrderDetails : Screen(route="order_details/{orderId}") {
        fun createRoute(orderId: String) = "order_details/$orderId"
    }

    // Role-based screens
    object AdminHome : Screen(route = "admin_home")
    object DeliveryHome : Screen(route = "delivery_home")

    // New checkout screens
    object Checkout : Screen(title = "Checkout", route = "checkout")
    object OrderConfirmation : Screen(title = "Order Confirmation", route = "order_confirmation/{orderId}") {
        fun createRoute(orderId: String) = "order_confirmation/$orderId"
    }

    // Admin management screens
    object ManageOrders : Screen(route="manage_orders_screen")
    object AdminOrderDetails : Screen(route="admin_order_details_screen/{orderId}") {
        fun createRoute(orderId: String): String {
            return "admin_order_details_screen/$orderId"
        }
    }
// Admin edit screen
    object EditOrder : Screen(route ="edit_order_screen/{orderId}") {
        fun createRoute(orderId: String): String {
            return "edit_order_screen/$orderId"
        }
    }

    // User management screens
    object ManageUsers : Screen(route="manage_users_screen")

    object UserDetails : Screen(route="user_details_screen/{userId}") {
        fun createRoute(userId: String): String {
            return "user_details_screen/$userId"
        }
    }

    object EditUser : Screen(route="edit_user_screen/{userId}") {
        fun createRoute(userId: String): String {
            return "edit_user_screen/$userId"
        }
    }

    // Product management screens
    object ManageProducts : Screen(route="manage_products_screen")

    object ProductDetails : Screen(route="product_details_screen/{productId}") {
        fun createRoute(productId: String): String {
            return "product_details_screen/$productId"
        }
    }

    object EditProduct : Screen(route="edit_product_screen/{productId}") {
        fun createRoute(productId: String): String {
            return "edit_product_screen/$productId"
        }
    }
    object AddProduct : Screen(route="add_product_screen")

    // Add the OrderDeliveryScreen here
    object OrderDelivery : Screen(title = "Delivery Details", route = "orderDelivery/{orderId}") {
        fun createRoute(orderId: String) = "orderDelivery/$orderId"
    }

    object InventoryAnalysis : Screen(route="inventory_analysis_screen")
    object InventoryReportDetail : Screen(route="inventory_report_detail")




    // Bottom Navigation Screens
    sealed class BottomScreen(
        val bTitle: String,
        bRoute: String,
        @DrawableRes val icon: Int
    ) : Screen(title = bTitle, route = bRoute) {
        object Home : BottomScreen(
            "Home",
            "home",
            R.drawable.baseline_home_24 // Make sure to add this icon
        )
        object Cart : BottomScreen(
            "Cart",
            "cart",
            R.drawable.baseline_shopping_cart_24 // Make sure to add this icon
        )
        object Categories : BottomScreen(
            "Categories",
            "categories",
            R.drawable.baseline_category_24 // Make sure to add this icon
        )
        object Orders : BottomScreen(
            "Orders", "orders",R.drawable.baseline_checklist_24
        )

    }

    // Drawer Screens
    sealed class DrawerScreen(
        val dTitle: String,
        dRoute: String,
        @DrawableRes val icon: Int
    ) : Screen(title = dTitle, route = dRoute) {
        object Profile : DrawerScreen(
            "Profile",
            "profile",
            R.drawable.baseline_man_24 // Make sure to add this icon
        )
        object Orders : DrawerScreen(
            "Orders",
            "orders",
            R.drawable.baseline_checklist_24 // Make sure to add this icon
        )
        object Addresses : DrawerScreen(
            "Addresses",
            "addresses",
            R.drawable.baseline_location_on_24 // Make sure to add this icon
        )
    }

    // Future admin/driver screens can be added in their own sealed classes
    // sealed class AdminScreen(...) : Screen(...)
    // sealed class DriverScreen(...) : Screen(...)
}

// Companion lists for navigation
val screensInBottom = listOf(
    Screen.BottomScreen.Home,
    Screen.BottomScreen.Orders,
    Screen.BottomScreen.Cart
)

val screensInDrawer = listOf(
    Screen.DrawerScreen.Profile,
    Screen.DrawerScreen.Orders,
    Screen.DrawerScreen.Addresses
)