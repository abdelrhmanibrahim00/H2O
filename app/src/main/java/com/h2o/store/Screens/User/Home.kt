package com.h2o.store.Screens.User

// Import necessary components for dropdowns
// --- End Dropdown Imports ---
import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.h2o.store.R
import com.h2o.store.ViewModels.User.CartViewModel
import com.h2o.store.ViewModels.User.ProductViewModel
import com.h2o.store.ViewModels.User.SizeFilter
import com.h2o.store.components.MainScaffold
import com.h2o.store.data.models.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    productViewModel: ProductViewModel,
    cartViewModel: CartViewModel,
    navController: NavHostController,
    onCartClick: () -> Unit,
    onHomeClick: () -> Unit,
    onOrderClick: () -> Unit,
    onProfileClick: () -> Unit,
    onHelpClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    MainScaffold(
        navController = navController,
        cartViewModel = cartViewModel,
        title = stringResource(id = R.string.app_title),
        onHomeClick = onHomeClick,
        onCartClick = onCartClick,
        onOrderClick = onOrderClick,
        onProfileClick = onProfileClick,
        onHelpClick = onHelpClick,
        onLogoutClick = onLogoutClick
    ) { paddingValues ->
        MainContent(
            cartViewModel = cartViewModel,
            productViewModel = productViewModel,
            paddingValues = PaddingValues( // Adjust padding as needed, maybe remove horizontal if LazyColumn handles it
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding(),
                start = 0.dp,
                end = 0.dp
            )
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    cartViewModel: CartViewModel,
    productViewModel: ProductViewModel,
    paddingValues: PaddingValues
) {
    val productState by productViewModel.productState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State for dropdown menus
    var brandExpanded by remember { mutableStateOf(false) }
    var sizeExpanded by remember { mutableStateOf(false) }

    val sizeFilterOptions = mapOf(
        SizeFilter.All to stringResource(R.string.all_sizes),
        SizeFilter.Small to stringResource(R.string.size_small),
        SizeFilter.Large to stringResource(R.string.size_large),
        SizeFilter.Gallon to stringResource(R.string.size_gallon)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Filter Dropdown
            Box(modifier = Modifier.weight(1f)) {
                ExposedDropdownMenuBox(
                    expanded = brandExpanded,
                    onExpandedChange = { brandExpanded = !brandExpanded }
                ) {
                    OutlinedTextField(
                        value = productState.selectedBrand ?: stringResource(R.string.all_brands),
                        onValueChange = {}, // Not editable
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        textStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                    )
                    ExposedDropdownMenu(
                        expanded = brandExpanded,
                        onDismissRequest = { brandExpanded = false },
                        modifier = Modifier.exposedDropdownSize(true)
                    ) {
                        // "All Brands" option
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.all_brands),
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                                )
                            },
                            onClick = {
                                productViewModel.selectBrand(null) // null represents "All Brands"
                                brandExpanded = false
                            }
                        )

                        // Brand options
                        productState.availableBrands.forEach { brand ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = brand,
                                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                                    )
                                },
                                onClick = {
                                    productViewModel.selectBrand(brand)
                                    brandExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Size Filter Dropdown
            Box(modifier = Modifier.weight(1f)) {
                ExposedDropdownMenuBox(
                    expanded = sizeExpanded,
                    onExpandedChange = { sizeExpanded = !sizeExpanded }
                ) {
                    OutlinedTextField(
                        value = sizeFilterOptions[productState.selectedSizeFilter] ?: "",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sizeExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        textStyle = typography.bodyLarge
                    )

                    ExposedDropdownMenu(
                        expanded = sizeExpanded,
                        onDismissRequest = { sizeExpanded = false },
                        modifier = Modifier.exposedDropdownSize(true)
                    ) {
                        sizeFilterOptions.forEach { (sizeFilter, label) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = label,
                                        style = typography.bodyMedium
                                    )
                                },
                                onClick = {
                                    productViewModel.selectSizeFilter(sizeFilter)
                                    sizeExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        } // End Filter Row

        // Clear Filters Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            // Only show clear button when filters are applied
            val hasActiveFilters = productState.selectedBrand != null || productState.selectedSizeFilter != SizeFilter.All

            if (hasActiveFilters) {
                androidx.compose.material3.TextButton(
                    onClick = {
                        productViewModel.selectBrand(null)
                        productViewModel.selectSizeFilter(SizeFilter.All)
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.clear_filters),
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        // Product List Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp)
        ) {
            when {
                productState.loading -> {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                    )
                }
                productState.error != null -> {
                    Text(
                        text = stringResource(R.string.error_prefix, productState.error ?: ""),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        style = typography.bodyLarge,
                        color = colorScheme.error
                    )
                }
                productState.filteredProducts.isEmpty() && !productState.loading -> {
                    val message = if(productState.allProducts.isEmpty()){
                        stringResource(R.string.no_products_available)
                    } else {
                        stringResource(R.string.no_products_match_filters)
                    }
                    Text(
                        text = message,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        textAlign = TextAlign.Center,
                        style = typography.bodyLarge,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            items = productState.filteredProducts,
                            key = { product -> product.id }
                        ) { product ->
                            ProductCard(
                                product = product,
                                onAddToCart = {
                                    cartViewModel.addToCart(product)
                                    scope.launch {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.product_added_to_cart, product.name),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        } // End Product List Area
    } // End Main Column
}
@SuppressLint("DefaultLocale")
@Composable
fun ProductCard(
    product: Product,
    onAddToCart: () -> Unit
) {
    // Using Material3 Card with white background and border
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shapes.medium,
        elevation = cardElevation(defaultElevation = 0.06.dp),
        colors = cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(
            width = 1.dp,
            color = colorScheme.primary.copy(alpha = 0.3f)
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Promotion badge for sales items
            // Promotion badge for sales items
            if (product.onSale && product.discountPercentage > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp) // Reduced padding from 8dp to 4dp to keep it closer to edge
                        .background(
                            color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp) // Reduced internal padding to make badge smaller
                ) {
                    Text(
                        text = stringResource(R.string.sale_percent, product.discountPercentage.toInt()),
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = androidx.compose.ui.graphics.Color.White,
                        maxLines = 1 // Ensure text doesn't wrap
                    )
                }
            }

            Column(Modifier.padding(16.dp)) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.imageUrl)
                        .crossfade(true)
                        .memoryCacheKey(product.id)
                        .diskCacheKey(product.id)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
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

                Text(
                    text = product.name,
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (product.onSale && product.discountPercentage > 0) {
                            val discountedPrice = product.price * (1 - product.discountPercentage / 100.0)
                            Text(
                                text = stringResource(R.string.price_value, String.format("%.2f", discountedPrice)),
                                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.price_value, String.format("%.2f", product.price)),
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                textDecoration = TextDecoration.LineThrough
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.price_value, String.format("%.2f", product.price)),
                                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    androidx.compose.material3.IconButton(onClick = onAddToCart) {
                        androidx.compose.material3.Icon(
                            Icons.Default.AddShoppingCart,
                            contentDescription = stringResource(R.string.add_to_cart, product.name),
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Show quantity instead of just "In Stock"
                Text(
                    text = stringResource(R.string.quantity_available, product.quantity),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = if (product.stock > 0)
                        androidx.compose.material3.MaterialTheme.colorScheme.tertiary
                    else
                        androidx.compose.material3.MaterialTheme.colorScheme.error
                )
            }
        }
    }
}