package com.h2o.store.Screens.Admin
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.h2o.store.ViewModels.Admin.ManageProductsViewModel
import com.h2o.store.data.models.Product
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(
    navController: NavHostController,
    viewModel: ManageProductsViewModel,
    productId: String,
    onBackClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // States for the form
    var originalProduct by remember { mutableStateOf<Product?>(null) }
    var editedProduct by remember { mutableStateOf<Product?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Form fields
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var discountPercentage by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var onSale by remember { mutableStateOf(false) }
    var featured by remember { mutableStateOf(false) }
    var rating by remember { mutableStateOf("") }
    // State for the quantity field
    var quantity by remember { mutableStateOf("") }

    // Observe edit result
    val editResult by viewModel.editProductResult.collectAsState()

    // Load product details
    LaunchedEffect(productId) {
        isLoading = true
        try {
            val productDetails = viewModel.getProductDetails(productId)
            originalProduct = productDetails
            editedProduct = productDetails?.copy()

            // Initialize form fields
            productDetails?.let { product ->
                name = product.name
                description = product.description
                price = product.price.toString()
                discountPercentage = product.discountPercentage.toString()
                imageUrl = product.imageUrl
                category = product.category
                stock = product.stock.toString()
                brand = product.brand
                onSale = product.onSale
                featured = product.featured
                rating = product.rating.toString()
                quantity = product.quantity.toString()  // Initialize quantity from product
            }
        } catch (e: Exception) {
            scope.launch {
                snackbarHostState.showSnackbar("Failed to load product details: ${e.message}")
            }
        } finally {
            isLoading = false
        }
    }

    // Reset edit result when entering the screen
    LaunchedEffect(Unit) {
        viewModel.resetEditProductResult()
    }

    // Handle edit result
    LaunchedEffect(editResult) {
        if (editResult == true) {
            // Show success message
            snackbarHostState.showSnackbar("Product updated successfully")
            // Navigate back after showing the message
            onBackClick()
        } else if (editResult == false) {
            // Show error in UI
            snackbarHostState.showSnackbar("Failed to update product")
        }
    }

    // Function to update product with form values
    fun prepareUpdatedProduct(): Product? {
        val currentProduct = editedProduct ?: return null

        // Parse numeric values, defaulting to original values if parsing fails
        val updatedPrice = price.toDoubleOrNull() ?: currentProduct.price
        val updatedDiscountPercentage = discountPercentage.toDoubleOrNull() ?: currentProduct.discountPercentage
        val updatedStock = stock.toIntOrNull() ?: currentProduct.stock
        val updatedRating = rating.toDoubleOrNull() ?: currentProduct.rating
        val updatedQuantity = quantity.toIntOrNull() ?: currentProduct.quantity

        // Create updated product
        return currentProduct.copy(
            name = name,
            description = description,
            price = updatedPrice,
            discountPercentage = updatedDiscountPercentage,
            imageUrl = imageUrl,
            category = category,
            stock = updatedStock,
            brand = brand,
            onSale = onSale,
            featured = featured,
            rating = updatedRating,
            quantity = updatedQuantity
        )
    }

    // Save function
    fun saveChanges() {
        val updatedProduct = prepareUpdatedProduct()
        val originalProductCopy = originalProduct

        if (updatedProduct == null || originalProductCopy == null) {
            scope.launch {
                snackbarHostState.showSnackbar("Cannot update product: Missing data")
            }
            return
        }

        // Check if anything changed
        val hasChanges = viewModel.updateProduct(updatedProduct, originalProductCopy)

        if (!hasChanges) {
            // Show message that nothing changed
            scope.launch {
                snackbarHostState.showSnackbar("No changes were made to the product")
            }
        }
    }

    // Confirmation dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Changes") },
            text = { Text("Are you sure you want to save the changes to this product?") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        saveChanges()
                    }
                ) {
                    Text("Yes, Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Product") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showConfirmDialog = true }
                    ) {
                        Text("Save")
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Product ID (non-editable)
                    Text(
                        text = "Product ID: ${originalProduct?.id}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    // Product image preview
                    if (imageUrl.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Product image",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Basic product information
                    Text(
                        text = "Basic Information",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Product Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = brand,
                            onValueChange = { brand = it },
                            label = { Text("Brand") },
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Category") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Pricing information
                    Text(
                        text = "Pricing",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = it },
                            label = { Text("Price") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = discountPercentage,
                            onValueChange = { discountPercentage = it },
                            label = { Text("Discount %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Quantity information
                    Text(
                        text = "Quantity",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = quantity,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d+$"))) {
                                quantity = it
                            }
                        },
                        label = { Text("Product Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Stock information
                    Text(
                        text = "Stock",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = stock,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d+$"))) {
                                stock = it
                            }
                        },
                        label = { Text("Stock Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Image URL
                    Text(
                        text = "Image",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("Image URL") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Additional information
                    Text(
                        text = "Additional Information",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = onSale,
                            onCheckedChange = { onSale = it }
                        )
                        Text("On Sale")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = featured,
                            onCheckedChange = { featured = it }
                        )
                        Text("Featured Product")
                    }

                    OutlinedTextField(
                        value = rating,
                        onValueChange = { rating = it },
                        label = { Text("Rating (0-5)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Save button
                    Button(
                        onClick = { showConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Changes")
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}