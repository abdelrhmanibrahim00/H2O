//package com.h2o.store.Screens.Admin
//
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.grid.GridCells
//import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
//import androidx.compose.foundation.lazy.grid.items
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import androidx.navigation.NavHostController
//import coil.compose.AsyncImage
//import com.h2o.store.ViewModels.Admin.ManageProductsViewModel
//import com.h2o.store.data.models.Product
//import java.text.NumberFormat
//import java.util.*
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ManageProductsScreen(
//    navController: NavHostController,
//    viewModel: ManageProductsViewModel,
//    onProductDetails: (String) -> Unit,
//    onBackClick: () -> Unit
//) {
//    var searchQuery by remember { mutableStateOf("") }
//    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
//
//    // Collect state from ViewModel
//    val allProducts by viewModel.allProducts.collectAsState()
//    val isLoading by viewModel.isLoading.collectAsState()
//    val errorMessage by viewModel.errorMessage.collectAsState()
//
//    // Effect to fetch products when the screen is shown
//    LaunchedEffect(key1 = true) {
//        viewModel.fetchAllProducts()
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Manage Products") },
//                navigationIcon = {
//                    IconButton(onClick = onBackClick) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                    }
//                }
//            )
//        }
//    ) { innerPadding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(innerPadding)
//        ) {
//            // Search bar
//            OutlinedTextField(
//                value = searchQuery,
//                onValueChange = {
//                    searchQuery = it
//                    viewModel.filterProductsByText(it)
//                },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = 16.dp, vertical = 8.dp),
//                placeholder = { Text("Search products...") },
//                singleLine = true,
//                leadingIcon = {
//                    Icon(Icons.Default.Search, contentDescription = "Search")
//                },
//                trailingIcon = {
//                    if (searchQuery.isNotEmpty()) {
//                        IconButton(onClick = {
//                            searchQuery = ""
//                            viewModel.fetchAllProducts()
//                        }) {
//                            Icon(Icons.Default.Clear, contentDescription = "Clear")
//                        }
//                    }
//                }
//            )
//
//            // Show error message if any
//            errorMessage?.let { error ->
//                Text(
//                    text = error,
//                    color = MaterialTheme.colorScheme.error,
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(16.dp),
//                    textAlign = TextAlign.Center
//                )
//            }
//
//            // Show loading indicator
//            if (isLoading) {
//                Box(
//                    modifier = Modifier.fillMaxSize(),
//                    contentAlignment = Alignment.Center
//                ) {
//                    CircularProgressIndicator()
//                }
//            } else if (allProducts.isEmpty()) {
//                // Show empty state
//                Box(
//                    modifier = Modifier.fillMaxSize(),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Text(
//                        text = "No products found",
//                        style = MaterialTheme.typography.bodyLarge
//                    )
//                }
//            } else {
//                // Show products grid
//                LazyVerticalGrid(
//                    columns = GridCells.Adaptive(minSize = 160.dp),
//                    contentPadding = PaddingValues(16.dp),
//                    horizontalArrangement = Arrangement.spacedBy(8.dp),
//                    verticalArrangement = Arrangement.spacedBy(8.dp),
//                    modifier = Modifier.fillMaxSize()
//                ) {
//                    items(allProducts) { product ->
//                        ProductCard(
//                            product = product,
//                            currencyFormat = currencyFormat,
//                            onCardClick = { onProductDetails(product.id) }
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ProductCard(
//    product: Product,
//    currencyFormat: NumberFormat,
//    onCardClick: () -> Unit
//) {
//    Card(
//        onClick = onCardClick,
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(220.dp)
//    ) {
//        Column(
//            modifier = Modifier.fillMaxSize()
//        ) {
//            // Product image
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(120.dp)
//            ) {
//                if (product.imageUrl.isNotEmpty()) {
//                    AsyncImage(
//                        model = product.imageUrl,
//                        contentDescription = product.name,
//                        contentScale = ContentScale.Crop,
//                        modifier = Modifier.fillMaxSize()
//                    )
//                } else {
//                    Box(
//                        modifier = Modifier.fillMaxSize(),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.Image,
//                            contentDescription = "No image",
//                            modifier = Modifier.size(48.dp)
//                        )
//                    }
//                }
//
//                // Show sale badge if product is on sale
//                if (product.onSale) {
//                    Surface(
//                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
//                        shape = MaterialTheme.shapes.small,
//                        modifier = Modifier
//                            .align(Alignment.TopEnd)
//                            .padding(4.dp)
//                    ) {
//                        Text(
//                            text = "SALE",
//                            color = MaterialTheme.colorScheme.onError,
//                            style = MaterialTheme.typography.labelSmall,
//                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
//                        )
//                    }
//                }
//
//                // Show featured badge if product is featured
//                if (product.featured) {
//                    Surface(
//                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
//                        shape = MaterialTheme.shapes.small,
//                        modifier = Modifier
//                            .align(Alignment.TopStart)
//                            .padding(4.dp)
//                    ) {
//                        Text(
//                            text = "⭐ FEATURED",
//                            color = MaterialTheme.colorScheme.onTertiary,
//                            style = MaterialTheme.typography.labelSmall,
//                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
//                        )
//                    }
//                }
//            }
//
//            // Product info
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(8.dp)
//            ) {
//                Text(
//                    text = product.name,
//                    style = MaterialTheme.typography.titleSmall,
//                    fontWeight = FontWeight.Bold,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis
//                )
//
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    // Show discounted price if there's a discount
//                    if (product.discountPercentage > 0) {
//                        val discountedPrice = product.price * (1 - product.discountPercentage / 100)
//                        Column {
//                            Text(
//                                text = currencyFormat.format(discountedPrice),
//                                style = MaterialTheme.typography.bodyMedium,
//                                color = MaterialTheme.colorScheme.primary
//                            )
//                            Text(
//                                text = currencyFormat.format(product.price),
//                                style = MaterialTheme.typography.bodySmall,
//                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
//                            )
//                        }
//                    } else {
//                        Text(
//                            text = currencyFormat.format(product.price),
//                            style = MaterialTheme.typography.bodyMedium
//                        )
//                    }
//
//                    // Stock indicator
//                    val stockColor = when {
//                        product.stock <= 0 -> MaterialTheme.colorScheme.error
//                        product.stock < 10 -> MaterialTheme.colorScheme.tertiary
//                        else -> MaterialTheme.colorScheme.secondary
//                    }
//
//                    Surface(
//                        color = stockColor.copy(alpha = 0.1f),
//                        shape = MaterialTheme.shapes.small
//                    ) {
//                        Text(
//                            text = if (product.stock <= 0) "Out of Stock" else "${product.stock} in stock",
//                            style = MaterialTheme.typography.labelSmall,
//                            color = stockColor,
//                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
//                        )
//                    }
//                }
//            }
//        }
//    }
//}