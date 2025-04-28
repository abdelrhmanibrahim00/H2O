package com.h2o.store.Screens.User

// Import necessary components for dropdowns
// --- End Dropdown Imports ---
import android.widget.Toast
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
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
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

// OptIn annotation needed for ExperimentalMaterial3Api
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    cartViewModel: CartViewModel,
    productViewModel: ProductViewModel,
    paddingValues: PaddingValues // Padding from Scaffold
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

    // --- Column wrapping filters and list ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues) // Apply scaffold padding first
    ) {

        // --- Filter Row ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp), // Padding for the filter row
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Filter Dropdown
            Box(modifier = Modifier.weight(1f)) {
                ExposedDropdownMenuBox(
                    expanded = brandExpanded,
                    onExpandedChange = { brandExpanded = !brandExpanded }
                ) {
                    OutlinedTextField( // Use OutlinedTextField for visual representation
                        value = productState.selectedBrand ?: stringResource(R.string.all_brands),
                        onValueChange = {}, // Not editable
                        readOnly = true,
                        label = { Text(stringResource(R.string.filter_by_brand)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandExpanded) },
                        modifier = Modifier
                            .menuAnchor() // Important for positioning the dropdown
                            .fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.textFieldColors(
                            // containerColor = Color.Transparent // Make background transparent if needed
                        ),
                        textStyle = MaterialTheme.typography.body2 // Adjust text style if needed
                    )
                    ExposedDropdownMenu(
                        expanded = brandExpanded,
                        onDismissRequest = { brandExpanded = false }
                    ) {
                        // "All Brands" option
                        DropdownMenuItem(onClick = {
                            productViewModel.selectBrand(null) // null represents "All Brands"
                            brandExpanded = false
                        }) {
                            Text(stringResource(R.string.all_brands))
                        }
                        // Brand options
                        productState.availableBrands.forEach { brand ->
                            DropdownMenuItem(onClick = {
                                productViewModel.selectBrand(brand)
                                brandExpanded = false
                            }) {
                                Text(brand)
                            }
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
                        label = { Text(stringResource(R.string.filter_by_size)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sizeExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.textFieldColors(
                            // containerColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.body2
                    )
                    ExposedDropdownMenu(
                        expanded = sizeExpanded,
                        onDismissRequest = { sizeExpanded = false }
                    ) {
                        sizeFilterOptions.forEach { (sizeFilter, label) ->
                            DropdownMenuItem(onClick = {
                                productViewModel.selectSizeFilter(sizeFilter)
                                sizeExpanded = false
                            }) {
                                Text(label)
                            }
                        }
                    }
                }
            }
        } // End Filter Row

        // --- Product List Area ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp) // Add some space below filters
        ) {
            when {
                productState.loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                productState.error != null -> {
                    Text(
                        text = stringResource(R.string.error_prefix, productState.error ?: ""),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                // Display products based on filtered list
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
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            items = productState.filteredProducts, // Use the filtered list
                            key = { product -> product.id } // Stable key
                        ) { product ->
                            ProductCard(
                                product = product,
                                onAddToCart = {
                                    cartViewModel.addToCart(product)
                                    scope.launch {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                // Use the correct string resource name
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


@Composable
fun ProductCard(
    product: Product,
    onAddToCart: () -> Unit
) {
    // Using M2 Card as potentially styled before
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
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
                contentScale = ContentScale.Crop,
                placeholder = rememberVectorPainter(Icons.Default.Image),
                error = rememberVectorPainter(Icons.Default.BrokenImage)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = product.name,
                style = MaterialTheme.typography.h6,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
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
                            style = MaterialTheme.typography.body1,
                            color = MaterialTheme.colors.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.price_value, String.format("%.2f", product.price)),
                            style = MaterialTheme.typography.body2,
                            color = Color.Gray,
                            textDecoration = TextDecoration.LineThrough
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.price_value, String.format("%.2f", product.price)),
                            style = MaterialTheme.typography.body1,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(onClick = onAddToCart) {
                    Icon( // M2 Icon
                        Icons.Default.AddShoppingCart,
                        contentDescription = stringResource(R.string.add_to_cart, product.name)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Use the correct string resource name
            val stockStatusText = if (product.stock > 0) {
                stringResource(R.string.in_stock)
            } else {
                stringResource(R.string.out_of_stock)
            }
            Text(
                text = stockStatusText,
                style = MaterialTheme.typography.caption,
                color = if (product.stock > 0) Color(0xFF388E3C) else Color(0xFFD32F2F)
            )
        }
    }
}