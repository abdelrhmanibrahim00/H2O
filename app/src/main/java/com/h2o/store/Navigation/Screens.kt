package com.h2o.store.Navigation

import androidx.annotation.DrawableRes
import com.h2o.store.R

sealed class Screen(val title: String = "", val route: String) {
    // Existing authentication and setup screens
    object Splash : Screen(route = "splash")
    object SignUp : Screen(route = "signup")
    object Login : Screen(route = "login")
    object Map : Screen(route = "map")
    object Home : Screen(route = "home")
    object Cart : Screen(route = "cart")
    object Orders : Screen(route = "order")
    object Profile : Screen(route = "profile")
    object EditProfile : Screen(route = "edit_profile")
    object Help : Screen(route = "help")

    // New checkout screens
    object Checkout : Screen(title = "Checkout", route = "checkout")
    object OrderConfirmation : Screen(title = "Order Confirmation", route = "order_confirmation/{orderId}") {
        fun createRoute(orderId: String) = "order_confirmation/$orderId"
    }




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