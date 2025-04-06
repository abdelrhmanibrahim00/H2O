package com.h2o.store.ViewModels.Admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.h2o.store.data.models.Product
import com.h2o.store.repositories.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class ManageProductsViewModel(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())
    val allProducts: StateFlow<List<Product>> = _allProducts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _editProductResult = MutableStateFlow<Boolean?>(null)
    val editProductResult: StateFlow<Boolean?> = _editProductResult

    // Add result state for adding a product
    private val _addProductResult = MutableStateFlow<Boolean?>(null)
    val addProductResult: StateFlow<Boolean?> = _addProductResult

    // Add state for delete operation
    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting

    private val _deleteProductResult = MutableStateFlow<Boolean?>(null)
    val deleteProductResult: StateFlow<Boolean?> = _deleteProductResult

    private val TAG = "ManageProductsViewModel"

    // Fetch all products
    fun fetchAllProducts() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                productRepository.getAllProductsFlow()
                    .catch { e ->
                        Log.e(TAG, "Error fetching products: ${e.message}")
                        _errorMessage.value = "Failed to load products: ${e.message}"
                        _isLoading.value = false
                    }
                    .collectLatest { products ->
                        _allProducts.value = products
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in fetchAllProducts: ${e.message}")
                _errorMessage.value = "Failed to load products: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Get product details
    suspend fun getProductDetails(productId: String): Product? {
        return try {
            productRepository.getProductById(productId)
        } catch (e: Exception) {
            _errorMessage.value = "Error fetching product details: ${e.message}"
            null
        }
    }

    // Add a new product
    fun addProduct(product: Product) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _addProductResult.value = null

            try {
                Log.d(TAG, "Adding new product: ${product.name}")

                val newProductId = productRepository.addProduct(product)

                if (newProductId != null) {
                    Log.d(TAG, "Successfully added product with ID: $newProductId")
                    _addProductResult.value = true

                    // Refresh the product list
                    fetchAllProducts()
                } else {
                    Log.e(TAG, "Failed to add product, no ID returned")
                    _errorMessage.value = "Failed to add product"
                    _addProductResult.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding product: ${e.message}")
                _errorMessage.value = "Error adding product: ${e.message}"
                _addProductResult.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Delete a product
    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            _isDeleting.value = true
            _errorMessage.value = null
            _deleteProductResult.value = null

            try {
                Log.d(TAG, "Deleting product: $productId")

                val success = productRepository.deleteProduct(productId)

                if (success) {
                    Log.d(TAG, "Successfully deleted product: $productId")
                    _deleteProductResult.value = true

                    // Refresh the product list
                    fetchAllProducts()
                } else {
                    Log.e(TAG, "Failed to delete product: $productId")
                    _errorMessage.value = "Failed to delete product"
                    _deleteProductResult.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting product: ${e.message}")
                _errorMessage.value = "Error deleting product: ${e.message}"
                _deleteProductResult.value = false
            } finally {
                _isDeleting.value = false
            }
        }
    }

    // Update product
    fun updateProduct(product: Product, originalProduct: Product): Boolean {
        // Do a proper comparison by checking each field that we allow editing
        val hasChanges = !(
                product.name == originalProduct.name &&
                        product.description == originalProduct.description &&
                        product.price == originalProduct.price &&
                        product.discountPercentage == originalProduct.discountPercentage &&
                        product.imageUrl == originalProduct.imageUrl &&
                        product.category == originalProduct.category &&
                        product.stock == originalProduct.stock &&
                        product.brand == originalProduct.brand &&
                        product.onSale == originalProduct.onSale &&
                        product.featured == originalProduct.featured &&
                        product.rating == originalProduct.rating &&
                        product.quantity == originalProduct.quantity
                )

        // If nothing has changed, return false immediately
        if (!hasChanges) {
            Log.d(TAG, "No changes detected in product ${product.id}")
            return false
        }

        // Log what changed for debugging
        Log.d(TAG, "Changes detected in product ${product.id}:")
        if (product.name != originalProduct.name)
            Log.d(TAG, "Name changed: ${originalProduct.name} -> ${product.name}")
        if (product.description != originalProduct.description)
            Log.d(TAG, "Description changed")
        if (product.price != originalProduct.price)
            Log.d(TAG, "Price changed: ${originalProduct.price} -> ${product.price}")
        if (product.discountPercentage != originalProduct.discountPercentage)
            Log.d(TAG, "Discount changed: ${originalProduct.discountPercentage} -> ${product.discountPercentage}")
        if (product.imageUrl != originalProduct.imageUrl)
            Log.d(TAG, "Image URL changed")
        if (product.category != originalProduct.category)
            Log.d(TAG, "Category changed: ${originalProduct.category} -> ${product.category}")
        if (product.stock != originalProduct.stock)
            Log.d(TAG, "Stock changed: ${originalProduct.stock} -> ${product.stock}")
        if (product.brand != originalProduct.brand)
            Log.d(TAG, "Brand changed: ${originalProduct.brand} -> ${product.brand}")
        if (product.onSale != originalProduct.onSale)
            Log.d(TAG, "On Sale changed: ${originalProduct.onSale} -> ${product.onSale}")
        if (product.featured != originalProduct.featured)
            Log.d(TAG, "Featured changed: ${originalProduct.featured} -> ${product.featured}")
        if (product.rating != originalProduct.rating)
            Log.d(TAG, "Rating changed: ${originalProduct.rating} -> ${product.rating}")
        if (product.quantity != originalProduct.quantity)
            Log.d(TAG, "Quantity changed: ${originalProduct.quantity} -> ${product.quantity}")

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _editProductResult.value = null

            try {
                val success = productRepository.updateProduct(product)
                _editProductResult.value = success

                if (success) {
                    Log.d(TAG, "Successfully updated product ${product.id}")
                } else {
                    _errorMessage.value = "Failed to update product"
                    Log.e(TAG, "Failed to update product ${product.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating product: ${e.message}")
                _errorMessage.value = "Error updating product: ${e.message}"
                _editProductResult.value = false
            } finally {
                _isLoading.value = false
            }
        }

        return true
    }

    // Filter products by text search
    fun filterProductsByText(query: String) {
        if (query.isBlank()) {
            fetchAllProducts()
            return
        }

        viewModelScope.launch {
            try {
                // We'll implement client-side filtering
                val allProductsList = _allProducts.value
                val lowerQuery = query.lowercase(Locale.getDefault())

                val filteredProducts = allProductsList.filter { product ->
                    product.name.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                            product.description.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                            product.category.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                            product.brand.lowercase(Locale.getDefault()).contains(lowerQuery)
                }

                // Update the state with filtered products
                _allProducts.value = filteredProducts
            } catch (e: Exception) {
                Log.e(TAG, "Error filtering products: ${e.message}")
                _errorMessage.value = "Error filtering products: ${e.message}"
            }
        }
    }

    // Reset edit result state
    fun resetEditProductResult() {
        _editProductResult.value = null
    }

    // Reset add result state
    fun resetAddProductResult() {
        _addProductResult.value = null
    }

    // Reset delete result state
    fun resetDeleteProductResult() {
        _deleteProductResult.value = null
    }

    // Factory for creating this ViewModel with dependencies
    class Factory(private val productRepository: ProductRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ManageProductsViewModel::class.java)) {
                return ManageProductsViewModel(productRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}