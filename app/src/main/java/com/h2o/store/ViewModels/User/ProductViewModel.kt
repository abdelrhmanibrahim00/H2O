package com.h2o.store.ViewModels.User

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.h2o.store.data.models.Product
import com.h2o.store.repositories.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Define SizeFilter enum/sealed class outside the ViewModel for better organization
sealed class SizeFilter {
    object All : SizeFilter()
    object Small : SizeFilter() // <= 600
    object Large : SizeFilter() // > 600 && < 1500
    object Gallon : SizeFilter() // >= 1500
}


class ProductViewModel(private val repository: ProductRepository) : ViewModel() {

    private val TAG = "ProductViewModel"
    private val auth = Firebase.auth

    // --- Factory remains the same ---
    class Factory(private val repository: ProductRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            Log.d("ProductViewModel", "Factory.create called with class: ${modelClass.name}")
            if (modelClass.isAssignableFrom(ProductViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ProductViewModel(repository) as T
            }
            Log.e("ProductViewModel", "Unknown ViewModel class: ${modelClass.name}")
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    // --- Updated ProductState ---
    data class ProductState(
        val loading: Boolean = true,
        val allProducts: List<Product> = emptyList(), // Holds the original list
        val filteredProducts: List<Product> = emptyList(), // Holds the list to display
        val availableBrands: List<String> = emptyList(), // Unique brands for filter
        val selectedBrand: String? = null, // null means "All Brands"
        val selectedSizeFilter: SizeFilter = SizeFilter.All, // Default to "All Sizes"
        val error: String? = null
    )

    private val _productState = MutableStateFlow(ProductState())
    val productState: StateFlow<ProductState> = _productState.asStateFlow()

    // --- Auth state listener remains the same ---
    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        if (auth.currentUser != null) {
            Log.d(TAG, "Auth state changed: User authenticated, uid: ${auth.currentUser?.uid}")
            refreshProducts()
        } else {
            Log.d(TAG, "Auth state changed: User not authenticated")
            viewModelScope.launch(Dispatchers.Main) {
                _productState.update { currentState ->
                    currentState.copy(
                        allProducts = emptyList(),
                        filteredProducts = emptyList(),
                        availableBrands = emptyList(),
                        loading = false,
                        error = "Please log in to view products",
                        selectedBrand = null, // Reset filters on logout
                        selectedSizeFilter = SizeFilter.All
                    )
                }
            }
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "Initial auth check: User authenticated, uid: ${currentUser.uid}")
            ensureTokenAndFetchProducts()
        } else {
            Log.d(TAG, "Initial auth check: User not authenticated")
            viewModelScope.launch(Dispatchers.Main) {
                _productState.update {
                    it.copy(
                        loading = false,
                        error = "Please log in to view products"
                    )
                }
            }
        }
    }

    // --- ensureTokenAndFetchProducts, checkAndRefreshTokenIfNeeded, refreshToken remain the same ---
    private fun ensureTokenAndFetchProducts() {
        val user = auth.currentUser
        if (user == null) {
            Log.d(TAG, "Cannot fetch products: No authenticated user")
            viewModelScope.launch(Dispatchers.Main) {
                _productState.update { it.copy(loading = false, error = "Authentication required") }
            }
            return
        }
        viewModelScope.launch(Dispatchers.Main) {
            _productState.update { it.copy(loading = true) }
        }
        checkAndRefreshTokenIfNeeded(user) { success ->
            if (success) {
                fetchProducts()
            } else {
                viewModelScope.launch(Dispatchers.Main) {
                    _productState.update { it.copy(loading = false, error = "Authentication error: Failed to refresh token") }
                }
            }
        }
    }

    private fun checkAndRefreshTokenIfNeeded(user: FirebaseUser, callback: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                user.getIdToken(false)
                    .addOnSuccessListener { result ->
                        val token = result.token
                        if (token == null) {
                            refreshToken(user, callback)
                        } else {
                            FirebaseAuth.getInstance().getAccessToken(false)
                                .addOnSuccessListener { tokenResult ->
                                    val exp = tokenResult.claims["exp"] as? Long ?: 0
                                    val currentTime = System.currentTimeMillis() / 1000
                                    if (exp - currentTime < 300) {
                                        refreshToken(user, callback)
                                    } else {
                                        Log.d(TAG, "Token is still valid, skipping refresh")
                                        callback(true)
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Failed to check token expiration: ${e.message}")
                                    refreshToken(user, callback)
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to get ID token: ${e.message}")
                        callback(false)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking token: ${e.message}")
                callback(false)
            }
        }
    }

    private fun refreshToken(user: FirebaseUser, callback: (Boolean) -> Unit) {
        user.getIdToken(true)
            .addOnSuccessListener { _ ->
                Log.d(TAG, "Token refreshed successfully")
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to refresh token: ${e.message}")
                callback(false)
            }
    }
    // --- fetchProducts updates state and triggers filtering ---
    private fun fetchProducts() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val products = withContext(Dispatchers.IO) {
                    repository.getProducts()
                }
                Log.d(TAG, "Successfully fetched ${products.size} products")

                // Extract unique brands
                val brands = products.mapNotNull { it.brand?.trim() }.filter { it.isNotEmpty() }.distinct().sorted()

                _productState.update { currentState ->
                    currentState.copy(
                        allProducts = products,
                        // Apply existing filters to the new product list
                        filteredProducts = applyFilters(products, currentState.selectedBrand, currentState.selectedSizeFilter),
                        availableBrands = brands,
                        loading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching products: ${e.message}")
                _productState.update { currentState ->
                    currentState.copy(
                        loading = false,
                        error = "Error fetching products: ${e.message}"
                    )
                }
            }
        }
    }

    // --- Function to apply filters ---
    private fun applyFilters(products: List<Product>, brand: String?, sizeFilter: SizeFilter): List<Product> {
        return products.filter { product ->
            val brandMatch = brand == null || product.brand?.equals(brand, ignoreCase = true) == true
            val sizeMatch = when (sizeFilter) {
                SizeFilter.All -> true
                SizeFilter.Small -> (product.category?.toIntOrNull() ?: 0) <= 600
                SizeFilter.Large -> (product.category?.toIntOrNull() ?: 0).let { it > 600 && it < 1500 }
                SizeFilter.Gallon -> (product.category?.toIntOrNull() ?: 0) >= 1500
            }
            brandMatch && sizeMatch
        }
    }

    // --- Functions to update filters ---
    fun selectBrand(brand: String?) { // null means "All Brands"
        _productState.update { currentState ->
            currentState.copy(
                selectedBrand = brand,
                filteredProducts = applyFilters(currentState.allProducts, brand, currentState.selectedSizeFilter)
            )
        }
    }

    fun selectSizeFilter(sizeFilter: SizeFilter) {
        _productState.update { currentState ->
            currentState.copy(
                selectedSizeFilter = sizeFilter,
                filteredProducts = applyFilters(currentState.allProducts, currentState.selectedBrand, sizeFilter)
            )
        }
    }


    fun refreshProducts() {
        ensureTokenAndFetchProducts()
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
    }

}