package com.h2o.store.Screens.Admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.h2o.store.ViewModels.Admin.ManageProductsViewModel
import com.h2o.store.data.models.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    navController: NavHostController,
    viewModel: ManageProductsViewModel,
    onBackClick: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var discountPercentage by remember { mutableStateOf("0.0") }
    var imageUrl by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var onSale by remember { mutableStateOf(false) }
    var featured by remember { mutableStateOf(false) }

    var quantity by remember { mutableStateOf("") }
    var quantityError by remember { mutableStateOf(false) }

    // Form validation states
    var nameError by remember { mutableStateOf(false) }
    var descriptionError by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }
    var categoryError by remember { mutableStateOf(false) }
    var stockError by remember { mutableStateOf(false) }
    var brandError by remember { mutableStateOf(false) }

    // Dialog state
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Get add product result from ViewModel
    val addProductResult by viewModel.addProductResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Handle save result
    LaunchedEffect(addProductResult) {
        if (addProductResult == true) {
            // Navigate back after successful save
            onBackClick()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Product") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = it.isBlank()
                    },
                    label = { Text("Product Name*") },
                    isError = nameError,
                    supportingText = { if (nameError) Text("Name is required") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true
                )

                // Description field
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        descriptionError = it.isBlank()
                    },
                    label = { Text("Description*") },
                    isError = descriptionError,
                    supportingText = { if (descriptionError) Text("Description is required") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                // Price field
                OutlinedTextField(
                    value = price,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            price = it
                            priceError = it.isBlank() || it.toDoubleOrNull() == null || it.toDoubleOrNull() == 0.0
                        }
                    },
                    label = { Text("Price*") },
                    isError = priceError,
                    supportingText = { if (priceError) Text("Valid price is required") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    prefix = { Text("$") }
                )

                // Discount percentage field
                OutlinedTextField(
                    value = discountPercentage,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            discountPercentage = it
                        }
                    },
                    label = { Text("Discount Percentage") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    suffix = { Text("%") }
                )

                // Image URL field
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Image URL") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true
                )

                // Category field
                OutlinedTextField(
                    value = category,
                    onValueChange = {
                        category = it
                        categoryError = it.isBlank()
                    },
                    label = { Text("Category*") },
                    isError = categoryError,
                    supportingText = { if (categoryError) Text("Category is required") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true
                )
                // Quantity field
                OutlinedTextField(
                    value = quantity,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d+$"))) {
                            quantity = it
                            quantityError = it.isBlank() || it.toIntOrNull() == null
                        }
                    },
                    label = { Text("Quantity*") },
                    isError = quantityError,
                    supportingText = { if (quantityError) Text("Valid quantity is required") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true
                )

                // Stock field
                OutlinedTextField(
                    value = stock,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d+$"))) {
                            stock = it
                            stockError = it.isBlank() || it.toIntOrNull() == null
                        }
                    },
                    label = { Text("Stock Quantity*") },
                    isError = stockError,
                    supportingText = { if (stockError) Text("Valid stock quantity is required") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true
                )

                // Brand field
                OutlinedTextField(
                    value = brand,
                    onValueChange = {
                        brand = it
                        brandError = it.isBlank()
                    },
                    label = { Text("Brand*") },
                    isError = brandError,
                    supportingText = { if (brandError) Text("Brand is required") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true
                )

                // On Sale switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "On Sale",
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = onSale,
                        onCheckedChange = { onSale = it }
                    )
                }

                // Featured switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Featured Product",
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = featured,
                        onCheckedChange = { featured = it }
                    )
                }

                // Save button
                Button(
                    onClick = {
                        // Validate required fields
                        nameError = name.isBlank()
                        descriptionError = description.isBlank()
                        priceError = price.isBlank() || price.toDoubleOrNull() == null || price.toDoubleOrNull() == 0.0
                        categoryError = category.isBlank()
                        stockError = stock.isBlank() || stock.toIntOrNull() == null
                        brandError = brand.isBlank()

                        if (!nameError && !descriptionError && !priceError &&
                            !categoryError && !stockError && !brandError) {
                            // Show confirmation dialog
                            showConfirmDialog = true
                        } else {
                            // Show error dialog
                            errorMessage = "Please fill all required fields correctly"
                            showErrorDialog = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Save Product")
                    }
                }
            }

            // Confirmation Dialog
            if (showConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmDialog = false },
                    title = { Text("Confirm Add Product") },
                    text = { Text("Are you sure you want to add this product?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showConfirmDialog = false

                                // Create new product
                                val newProduct = Product(
                                    id = "", // Will be assigned by Firebase
                                    name = name.trim(),
                                    description = description.trim(),
                                    price = price.toDoubleOrNull() ?: 0.0,
                                    discountPercentage = discountPercentage.toDoubleOrNull() ?: 0.0,
                                    imageUrl = imageUrl.trim(),
                                    category = category.trim(),
                                    stock = stock.toIntOrNull() ?: 0,
                                    brand = brand.trim(),
                                    onSale = onSale,
                                    featured = featured,
                                    rating = 0.0 // Default to 0, will be updated as users review
                                )

                                // Call ViewModel to add product
                                viewModel.addProduct(newProduct)
                            }
                        ) {
                            Text("Yes")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmDialog = false }) {
                            Text("No")
                        }
                    }
                )
            }

            // Error Dialog
            if (showErrorDialog) {
                AlertDialog(
                    onDismissRequest = { showErrorDialog = false },
                    title = { Text("Validation Error") },
                    text = { Text(errorMessage) },
                    confirmButton = {
                        TextButton(onClick = { showErrorDialog = false }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}