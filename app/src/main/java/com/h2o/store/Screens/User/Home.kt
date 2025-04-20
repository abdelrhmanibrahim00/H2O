package com.h2o.store.Screens.User

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
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.h2o.store.ViewModels.User.CartViewModel
import com.h2o.store.ViewModels.User.ProductViewModel
import com.h2o.store.components.MainScaffold

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
        title = "H2O Store",
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
            paddingValues = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding(),
                start = 16.dp,
                end = 16.dp
            )
        )
    }
}

@Composable
private fun MainContent(
    cartViewModel: CartViewModel,
    productViewModel: ProductViewModel,
    paddingValues: PaddingValues
) {
    val productState by productViewModel.productState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        when {
            productState.loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            productState.error != null -> {
                Text(
                    text = "Error: ${productState.error}",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
            productState.products.isEmpty() -> {
                Text(
                    text = "No products available",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(productState.products) { product ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = 4.dp
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                // Image
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(product.imageUrl)
                                        .crossfade(true)
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

                                // Product Name
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.h6,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(Modifier.height(8.dp))

                                // Price and Add to Cart
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        if (product.onSale) {
                                            Text(
                                                text = "${(product.price-(product.price*product.discountPercentage/100))} EGP",
                                                style = MaterialTheme.typography.body1,
                                                color = MaterialTheme.colors.primary
                                            )
                                            Text(
                                                text = "${product.price} EGP",
                                                style = MaterialTheme.typography.body2,
                                                color = Color.Gray,
                                                textDecoration = TextDecoration.LineThrough
                                            )
                                        } else {
                                            Text(
                                                text = "${product.price} EGP",
                                                style = MaterialTheme.typography.body1
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { cartViewModel.addToCart(product) }
                                    ) {
                                        androidx.compose.material.Icon(
                                            Icons.Default.AddShoppingCart,
                                            contentDescription = "Add to Cart"
                                        )
                                    }
                                }

                                Spacer(Modifier.height(4.dp))

                                // Stock Status
                                Text(
                                    text = "Quantity: ${product.quantity}",
                                    style = MaterialTheme.typography.caption,
                                    color = if (product.stock > 0) Color.Green else Color.Red
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}









