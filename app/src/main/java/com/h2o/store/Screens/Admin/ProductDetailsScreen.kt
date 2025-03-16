//package com.h2o.store.Screens.Admin
//
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.text.font.FontWeight
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
//fun ProductDetailsScreen(
//    navController: NavHostController,
//    viewModel: ManageProductsViewModel,
//    productId: String,
//    onEditProduct: (String) -> Unit,
//    onBackClick: () -> Unit
//) {
//    val scrollState = rememberScrollState()
//    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
//
//    var product by remember { mutableStateOf<Product?>(null) }
//    var isLoading by remember { mutableStateOf(true) }
//    var errorMessage by remember { mutableStateOf<String?>(null) }
//
//    // Reset edit result when entering screen
//    LaunchedEffect(Unit) {
//        viewModel.resetEditProductResult()
//    }
//
//    // Load product details
//    LaunchedEffect(productId) {
//        isLoading = true
//        try {
//            val productDetails = viewModel.getProductDetails(productId)
//            product = productDetails
//        } catch (e: Exception) {
//            errorMessage = "Failed to load product details: ${e.message}"
//        } finally {
//            isLoading = false
//        }
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Product Details") },
//                navigationIcon = {
//                    IconButton(onClick = onBackClick) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                    }
//                },
//                actions = {
//                    IconButton(onClick = { onEditProduct(productId) }) {
//                        Icon(Icons.Default.Edit, contentDescription = "Edit")
//                    }
//                }
//            )
//        }
//    ) { innerPadding ->
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(innerPadding)
//        ) {
//            if (isLoading) {
//                CircularProgressIndicator(
//                    modifier = Modifier.align(Alignment.Center)
//                )
//            } else if (errorMessage != null) {
//                Text(
//                    text = errorMessage ?: "Unknown error",
//                    color = MaterialTheme.colorScheme.error,
//                    modifier = Modifier
//                        .align(Alignment.Center)
//                        .padding(16.dp)
//                )
//            } else if (product != null) {
//                Column(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .padding(16.dp)
//                        .verticalScroll(scrollState)
//                ) {
//                    // Product image
//                    if (product?.imageUrl?.isNotEmpty() == true) {
//                        Card(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .height(200.dp)
//                        ) {
//                            AsyncImage(
//                                model = product?.imageUrl,
//                                contentDescription = "Product image",
//                                contentScale = ContentScale.Fit,
//                                modifier = Modifier.fillMaxSize()
//                            )
//                        }
//
//                        Spacer(modifier = Modifier.height(16.dp))
//                    }
//
//                    // Product basic information
//                    Card(
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        Column(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(16.dp)
//                        ) {
//                            Row(
//                                modifier = Modifier.fillMaxWidth(),
//                                horizontalArrangement = Arrangement.SpaceBetween,
//                                verticalAlignment = Alignment.CenterVertically
//                            ) {
//                                Text(
//                                    text = product?.name ?: "",
//                                    style = MaterialTheme.typography.headlineSmall
//                                )
//
//                                // Price and discount
//                                Column(horizontalAlignment = Alignment.End) {
//                                    if (product?.discountPercentage != null && product?.discountPercentage!! > 0) {
//                                        val discountedPrice = product?.price!! * (1 - product?.discountPercentage!! / 100)
//                                        Text(
//                                            text = currencyFormat.format(discountedPrice),
//                                            style = MaterialTheme.typography.titleMedium,
//                                            color = MaterialTheme.colorScheme.primary
//                                        )
//                                        Text(
//                                            text = currencyFormat.format(product?.price),
//                                            style = MaterialTheme.typography.bodyMedium,
//                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
//                                        )
//                                    } else {
//                                        Text(
//                                            text = currencyFormat.format(product?.price),
//                                            style = MaterialTheme.typography.titleMedium
//                                        )
//                                    }
//                                }
//                            }
//
//                            Spacer(modifier = Modifier.height(8.dp))
//
//                            // Status chips
//                            Row(
//                                modifier = Modifier.fillMaxWidth(),
//                                horizontalArrangement = Arrangement.spacedBy(8.dp)
//                            ) {
//                                // Stock status
//                                val stockColor = when {
//                                    product?.stock == null || product?.stock!! <= 0 -> MaterialTheme.colorScheme.error
//                                    product?.stock!! < 10 -> MaterialTheme.colorScheme.tertiary
//                                    else -> MaterialTheme.colorScheme.secondary
//                                }
//
//                                Surface(
//                                    color = stockColor.copy(alpha = 0.1f),
//                                    shape = MaterialTheme.shapes.small
//                                ) {
//                                    Text(
//                                        text = if (product?.stock == null || product?.stock!! <= 0)
//                                            "Out of Stock"
//                                        else
//                                            "${product?.stock} in stock",
//                                        style = MaterialTheme.typography.bodySmall,
//                                        color = stockColor,
//                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
//                                    )
//                                }
//
//                                // On sale chip
//                                if (product?.onSale == true) {
//                                    Surface(
//                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
//                                        shape = MaterialTheme.shapes.small
//                                    ) {
//                                        Text(
//                                            text = "ON SALE",
//                                            style = MaterialTheme.typography.bodySmall,
//                                            color = MaterialTheme.colorScheme.error,
//                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
//                                        )
//                                    }
//                                }
//
//                                // Featured chip
//                                if (product?.featured == true) {
//                                    Surface(
//                                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
//                                        shape = MaterialTheme.shapes.small
//                                    ) {
//                                        Text(
//                                            text = "FEATURED",
//                                            style = MaterialTheme.typography.bodySmall,
//                                            color = MaterialTheme.colorScheme.tertiary,
//                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
//                                        )
//                                    }
//                                }
//                            }
//
//                            Spacer(modifier = Modifier.height(8.dp))
//                            Divider()
//                            Spacer(modifier = Modifier.height(8.dp))
//
//                            // Description
//                            Text(
//                                text = "Description",
//                                style = MaterialTheme.typography.titleMedium,
//                                fontWeight = FontWeight.Bold
//                            )
//
//                            Spacer(modifier = Modifier.height(4.dp))
//
//                            Text(
//                                text = product?.description ?: "No description available",
//                                style = MaterialTheme.typography.bodyMedium
//                            )
//
//                            Spacer(modifier = Modifier.height(8.dp))
//                        }
//                    }
//
//                    Spacer(modifier = Modifier.height(16.dp))
//
//                    // Additional product details
//                    Card(
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        Column(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(16.dp)
//                        ) {
//                            Text(
//                                text = "Product Details",
//                                style = MaterialTheme.typography.titleMedium,
//                                fontWeight = FontWeight.Bold
//                            )
//
//                            Spacer(modifier = Modifier.height(8.dp))
//                            Divider()
//                            Spacer(modifier = Modifier.height(8.dp))
//
//                            DetailItem(label = "ID", value = product?.id ?: "")
//                            DetailItem(label = "Brand", value = product?.brand ?: "")
//                            DetailItem(label = "Category", value = product?.category ?: "")
//                            DetailItem(label = "Discount", value = if (product?.discountPercentage != null && product?.discountPercentage!! > 0)
//                                "${product?.discountPercentage}%" else "None")
//                            DetailItem(label = "Rating", value = "${product?.rating ?: 0}/5")
//                        }
//                    }
//
//                    Spacer(modifier = Modifier.height(24.dp))
//
//                    Button(
//                        onClick = { onEditProduct(productId) },
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        Icon(Icons.Default.Edit, contentDescription = "Edit")
//                        Spacer(modifier = Modifier.width(8.dp))
//                        Text("Edit Product")
//                    }
//
//                    Spacer(modifier = Modifier.height(24.dp))
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun DetailItem(label: String, value: String) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 4.dp),
//        horizontalArrangement = Arrangement.SpaceBetween
//    ) {
//        Text(
//            text = label,
//            style = MaterialTheme.typography.bodyMedium,
//            color = MaterialTheme.colorScheme.outline
//        )
//        Text(
//            text = value.ifEmpty { "Not provided" },
//            style = MaterialTheme.typography.bodyMedium,
//            fontWeight = FontWeight.Medium
//        )
//    }
//}