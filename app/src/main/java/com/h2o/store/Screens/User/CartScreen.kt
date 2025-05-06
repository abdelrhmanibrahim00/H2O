package com.h2o.store.Screens.User

// Remove M2 MaterialTheme if using M3 components predominantly
// import androidx.compose.material.MaterialTheme
// Use Material 3 imports for components you intend to be M3
// import androidx.compose.ui.graphics.vector.rememberVectorPainter // Can use if needed
import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.h2o.store.R
import com.h2o.store.ViewModels.User.CartViewModel
import com.h2o.store.components.MainScaffold
import com.h2o.store.data.Cart.CartItem

/**
 * Wrapper composable that initializes the CartViewModel with the current user ID
 */
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

/**
 * The main Cart Screen that uses MainScaffold
 */
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
    MainScaffold( // Assuming MainScaffold handles M2/M3 consistency or uses M3 Scaffold
        navController = navController,
        cartViewModel = cartViewModel,
        title = stringResource(R.string.cart_title),
        onHomeClick = onHomeClick,
        onCartClick = onCartClick,
        onOrderClick = onOrderClick,
        onProfileClick = onProfileClick,
        onHelpClick = onHelpClick,
        onLogoutClick = onLogoutClick
    ) { paddingValues ->
        CartContent(
            viewModel = cartViewModel,
            onCheckout = onCheckout,
            paddingValues = paddingValues,
            onHomeClick = onHomeClick
        )
    }
}
/**
 * Cart content that displays cart items or empty state
 */
@Composable
fun CartContent(
    viewModel: CartViewModel,
    onCheckout: () -> Unit,
    paddingValues: PaddingValues,
    onHomeClick: () -> Unit
) {
    val cartState by viewModel.cartState.collectAsState()
    val totalPrice by viewModel.totalPrice.collectAsState()

    // Use M3 color scheme if possible
    val primaryColor = colorScheme.primary
    val onPrimaryColor = colorScheme.onPrimary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues) // Apply padding from Scaffold
            .padding(horizontal = 16.dp) // Apply horizontal padding for content
    ) {
        when (cartState) {
            is CartViewModel.CartState.Loading -> {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 56.dp), // Adjust if needed to avoid overlap with bottom bar
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = primaryColor)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.loading_cart),
                            style = typography.bodyLarge // M3 Typography
                        )
                    }
                }
            }
            is CartViewModel.CartState.Success -> {
                val cartItems = (cartState as CartViewModel.CartState.Success).items

                if (cartItems.isEmpty()) {
                    // Empty cart state
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 56.dp), // Adjust padding
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_empty_cart), // Use a specific drawable
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = colorScheme.onSurfaceVariant // Use theme color
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.cart_empty_message),
                                style = MaterialTheme.typography.headlineSmall, // M3 Typography
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.cart_empty_suggestion),
                                style = MaterialTheme.typography.bodyLarge, // M3 Typography
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant // Use theme color
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onHomeClick,
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                            ) {
                                Text(stringResource(R.string.continue_shopping), color = onPrimaryColor)
                            }
                        }
                    }
                } else {
                    // Cart with items
                    // Add top padding for the header text
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.cart_item_list_header, cartItems.size),
                        style = MaterialTheme.typography.titleLarge, // M3 Typography
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f), // Takes available space
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp) // Add padding at the bottom of the list
                    ) {
                        items(
                            items = cartItems,
                            key = { item -> item.productId } // Stable key
                        ) { item ->
                            // Simplified animation: just fade in/out on add/remove
                            AnimatedVisibility(
                                visible = true, // Keep item visible in list
                                enter = fadeIn(animationSpec = tween(150)),
                                exit = fadeOut(animationSpec = tween(150))
                            ) {
                                EnhancedCartItemCard(
                                    cartItem = item,
                                    onQuantityChange = { newQuantity ->
                                        viewModel.updateQuantity(item, newQuantity)
                                    },
                                    onRemove = {
                                        viewModel.removeFromCart(item)
                                    },
                                    primaryColor = primaryColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp)) // Space before summary

                    // Order summary card
                    OptimizedOrderSummaryCard(
                        itemCount = cartItems.size,
                        totalPrice = totalPrice,
                        primaryColor = primaryColor,
                        onCheckout = onCheckout
                    )

                    Spacer(modifier = Modifier.height(16.dp)) // Space after summary before bottom bar
                }
            }
            is CartViewModel.CartState.Error -> {
                // Error state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.error_image), // Use an error icon
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = (cartState as CartViewModel.CartState.Error).message,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { /* TODO: Add retry logic */ },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                        ) {
                            Text(stringResource(R.string.retry), color = onPrimaryColor)
                        }
                    }
                }
            }
        }
    }
}
/**
 * Order summary card with optimized layout and M3 styling
 */
@SuppressLint("DefaultLocale")
@Composable
private fun OptimizedOrderSummaryCard(
    itemCount: Int,
    totalPrice: Int,
    primaryColor: Color, // Keep primaryColor for specific text styling if needed
    onCheckout: () -> Unit
) {
    // Use M3 Elevation and Surface for card-like appearance
    val cardContainerColor = MaterialTheme.colorScheme.surfaceVariant // M3 card color
    val onCardContainerColor = MaterialTheme.colorScheme.onSurfaceVariant
    val totalWithDelivery = totalPrice + 15.0 // Assuming delivery fee is fixed at 15.0

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        // Use pure white with no alpha
        color = Color.White,
        // Important: Set to 0dp to avoid the tint
        tonalElevation = 0.dp,
        // Use shadow elevation for depth instead
        shadowElevation = 1.dp,
        // Add the border directly to the surface
        border = BorderStroke(
            width = 1.dp,
            color = colorScheme.primary.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.order_summary),
                style = MaterialTheme.typography.titleMedium, // M3 Typography
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface // M3 text color
            )

            Spacer(modifier = Modifier.height(12.dp)) // M3 uses multiples of 4 or 8 often
            Divider(color = MaterialTheme.colorScheme.outlineVariant) // M3 divider color
            Spacer(modifier = Modifier.height(12.dp))

            // Items count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.items_count),
                    style = MaterialTheme.typography.bodyMedium, // M3 Typography
                    color = onCardContainerColor // Text color on card
                )
                Text(
                    text = stringResource(R.string.items_value, itemCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = onCardContainerColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtotal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.subtotal),
                    style = MaterialTheme.typography.bodyMedium,
                    color = onCardContainerColor
                )
                Text(
                    text = stringResource(R.string.price_value, String.format("%.2f", totalPrice.toDouble())),
                    style = MaterialTheme.typography.bodyMedium,
                    color = onCardContainerColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Delivery fee
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.delivery_fee),
                    style = MaterialTheme.typography.bodyMedium,
                    color = onCardContainerColor
                )
                Text(
                    text = stringResource(R.string.price_value, "15.00"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = onCardContainerColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.cart_total_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.cart_total, String.format("%.2f", totalWithDelivery)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor // Keep specific color for total if desired
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Use M3 Button
            Button(
                onClick = onCheckout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor, // M3 uses containerColor
                    contentColor = colorScheme.onPrimary // M3 uses contentColor
                )
            ) {
                Text(stringResource(R.string.cart_checkout_button))
            }
        }
    }
}


/**
 * Enhanced cart item card using M3 Surface and styling
 */
@SuppressLint("DefaultLocale")
@Composable
fun EnhancedCartItemCard(
    cartItem: CartItem,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit,
    primaryColor: Color, // Keep for specific elements if needed
    modifier: Modifier = Modifier
) {
    // *** FIX: Calculate displayPrice and originalPriceStrikethrough here ***
    // This makes them available throughout the Row scope
    val displayPrice: Double
    val originalPriceStrikethrough: Double?
    if (cartItem.discountPercentage > 0) {
        displayPrice = cartItem.price * (1 - cartItem.discountPercentage / 100.0)
        originalPriceStrikethrough = cartItem.price
    } else {
        displayPrice = cartItem.price
        originalPriceStrikethrough = null // No original price needed if not discounted
    }


    // Use M3 Surface for the card background and elevation
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        // Use pure white with no alpha
        color = Color.White,
        // Important: Set to 0dp to avoid the tint
        tonalElevation = 0.dp,
        // Use shadow elevation for depth instead
        shadowElevation = 1.dp,
        // Add the border directly to the surface
        border = BorderStroke(
            width = 1.dp,
            color = colorScheme.primary.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp), // Padding inside the card
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reduced size image for cart items
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(cartItem.imageUrl)
                    .crossfade(true)
                    .size(60) // Reduced from 80 to 60
                    .memoryCacheKey(cartItem.productId)
                    .diskCacheKey(cartItem.productId)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .build(),
                contentDescription = cartItem.name,
                modifier = Modifier
                    .size(60.dp) // Reduced from 80dp to 60dp
                    .padding(4.dp) // Added padding to give some space
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit // Changed from Crop to Fit
            )

            // Product details column
            Column(
                modifier = Modifier
                    .weight(1f) // Takes up remaining space
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = cartItem.name,
                    style = typography.titleMedium, // M3 Typography
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = colorScheme.onSurfaceVariant // Text color on card
                )

                Spacer(modifier = Modifier.height(4.dp))

                // *** FIX: Use the pre-calculated displayPrice ***
                Text(
                    text = stringResource(R.string.price_value, String.format("%.2f", displayPrice)),
                    style = typography.bodyMedium, // M3 Typography
                    color = primaryColor, // Use primary color for emphasis
                    fontWeight = FontWeight.Bold
                )

                // *** FIX: Use the pre-calculated originalPriceStrikethrough ***
                originalPriceStrikethrough?.let { original ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.price_value, String.format("%.2f", original)),
                            style = typography.bodySmall, // M3 Typography
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f), // Dimmed color
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        // Optional discount badge
                        Surface( // Using M2 Surface for simple colored background
                            color = Color.Red.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.discount_percentage, cartItem.discountPercentage.toString()),
                                color = Color.Red,
                                style = typography.labelSmall, // M3 Typography
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }


                // Brand (optional)
                if (cartItem.brand.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.brand_label, cartItem.brand),
                        style = typography.bodySmall, // M3 Typography
                        color = colorScheme.onSurfaceVariant // Text color on card
                    )
                }
            }

            // Quantity selector and remove button column
            Column(
                horizontalAlignment = Alignment.End // Align this column's content to the end
            ) {
                OptimizedQuantitySelector(
                    quantity = cartItem.quantity,
                    onIncrease = { onQuantityChange(cartItem.quantity + 1) },
                    onDecrease = { onQuantityChange(cartItem.quantity - 1) },
                    maxQuantity = cartItem.stock,
                    primaryColor = primaryColor // Pass color for buttons
                )

                Spacer(modifier = Modifier.height(8.dp))

                // *** FIX: Use the pre-calculated displayPrice to calculate item total ***
                val itemTotalPrice = displayPrice * cartItem.quantity
                Text(
                    text = stringResource(R.string.item_total, String.format("%.2f", itemTotalPrice)),
                    style = typography.bodyMedium, // M3 Typography
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurfaceVariant // Text color on card
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Remove button (use M3 IconButton)
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(36.dp) // Adequate touch target
                ) {
                    Icon(
                        Icons.Default.Delete, // Standard M2/M3 icon
                        contentDescription = stringResource(R.string.cart_item_remove, cartItem.name), // Specific description
                        tint = colorScheme.error, // Use M3 error color
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Optimized quantity selector component using M3 styling concepts
 */
@Composable
private fun OptimizedQuantitySelector(
    quantity: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    maxQuantity: Int,
    primaryColor: Color, // Keep for tinting if needed, otherwise use theme colors
    modifier: Modifier = Modifier
) {
    val canDecrease = quantity > 1
    val canIncrease = quantity < maxQuantity
    val interactionColor = MaterialTheme.colorScheme.primary // Color for enabled buttons
    val disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) // M3 disabled color

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp), // No space between elements
        modifier = modifier
            .height(36.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant, // M3 border color
                RoundedCornerShape(8.dp) // Slightly rounded corners for the whole component
            )
            .clip(RoundedCornerShape(8.dp)) // Clip content to rounded shape
    ) {
        // Decrease button
        IconButton(
            onClick = onDecrease,
            enabled = canDecrease,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Default.Remove,
                contentDescription = stringResource(R.string.cart_item_decrease),
                tint = if (canDecrease) interactionColor else disabledColor,
                modifier = Modifier.size(18.dp)
            )
        }

        // Quantity display with background
        Box(
            modifier = Modifier
                .width(40.dp) // Slightly wider
                .height(36.dp)
                .background(MaterialTheme.colorScheme.surface) // Background to separate from buttons
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ){
            Text(
                text = quantity.toString(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium, // M3 Typography
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface // Text color
            )
        }


        // Increase button
        IconButton(
            onClick = onIncrease,
            enabled = canIncrease,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(R.string.cart_item_increase),
                tint = if (canIncrease) interactionColor else disabledColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}