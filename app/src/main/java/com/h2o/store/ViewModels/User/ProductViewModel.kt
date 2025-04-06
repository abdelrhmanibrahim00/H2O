package com.h2o.store.ViewModels.User

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.h2o.store.Graph.Graph
import com.h2o.store.data.models.Product
import com.h2o.store.repositories.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProductViewModel(private val repository: ProductRepository = Graph.productRepository
) : ViewModel() {

    private val TAG = "ProductViewModel"
    private val auth = Firebase.auth

    // Factory for dependency injection
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

    private val _productState = MutableStateFlow<ProductState>(ProductState())
    val productState = _productState.asStateFlow()

    // Authentication state listener
    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        if (auth.currentUser != null) {
            Log.d(TAG, "Auth state changed: User authenticated, uid: ${auth.currentUser?.uid}")
            refreshProducts()
        } else {
            Log.d(TAG, "Auth state changed: User not authenticated")
            _productState.value = _productState.value.copy(
                products = emptyList(),
                loading = false,
                error = "Please log in to view products"
            )
        }
    }

    init {
        // Register auth state listener
        auth.addAuthStateListener(authStateListener)

        // Check current auth state
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "Initial auth check: User authenticated, uid: ${currentUser.uid}")
            ensureTokenAndFetchProducts()
        } else {
            Log.d(TAG, "Initial auth check: User not authenticated")
            _productState.value = _productState.value.copy(
                loading = false,
                error = "Please log in to view products"
            )
        }
    }

    private fun ensureTokenAndFetchProducts() {
        val user = auth.currentUser
        if (user == null) {
            Log.d(TAG, "Cannot fetch products: No authenticated user")
            _productState.value = _productState.value.copy(
                loading = false,
                error = "Authentication required"
            )
            return
        }

        _productState.value = _productState.value.copy(loading = true)

        // Force token refresh to ensure fresh authentication
        user.getIdToken(true)
            .addOnSuccessListener { _ ->
                Log.d(TAG, "Token refreshed successfully, fetching products")
                fetchProducts()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to refresh token: ${e.message}")
                _productState.value = _productState.value.copy(
                    loading = false,
                    error = "Authentication error: ${e.message}"
                )
            }
    }

    private fun fetchProducts() {
        viewModelScope.launch {
            try {
                val products = repository.getProducts()
                Log.d(TAG, "Successfully fetched ${products.size} products")
                _productState.value = _productState.value.copy(
                    products = products,
                    loading = false,
                    error = null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching products: ${e.message}")
                _productState.value = _productState.value.copy(
                    loading = false,
                    error = "Error fetching products: ${e.message}"
                )
            }
        }
    }

    fun refreshProducts() {
        ensureTokenAndFetchProducts()
    }

    override fun onCleared() {
        super.onCleared()
        // Remove the auth state listener when ViewModel is cleared
        auth.removeAuthStateListener(authStateListener)
    }

    data class ProductState(
        val loading: Boolean = true,
        val products: List<Product> = emptyList(),
        val error: String? = null
    )
}