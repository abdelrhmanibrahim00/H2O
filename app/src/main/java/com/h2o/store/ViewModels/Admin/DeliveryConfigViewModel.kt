package com.h2o.store.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.h2o.store.Graph.Graph
import com.h2o.store.data.models.DeliveryConfig
import com.h2o.store.data.models.District
import com.h2o.store.repositories.DeliveryConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for delivery configuration management
 */
class DeliveryConfigViewModel(
    private val repository: DeliveryConfigRepository
) : ViewModel() {

    // UI state for delivery configuration
    val deliveryConfig: StateFlow<DeliveryConfig?> = repository.deliveryConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )


    // Districts for user selection (previously filtered by isActive)
    val activeDistricts: StateFlow<List<District>> = repository.districts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // All districts for admin management
    private val _allDistricts = MutableStateFlow<List<District>>(emptyList())
    val allDistricts = _allDistricts.asStateFlow()

    // UI state
    val isLoading: StateFlow<Boolean> = repository.isLoading
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val error: StateFlow<String?> = repository.error
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // User action results
    private val _actionCompleted = MutableStateFlow<ActionResult?>(null)
    val actionCompleted = _actionCompleted.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initialize()
        }
    }

    /**
     * Calculate delivery fee based on account type and order total
     */
    fun calculateDeliveryFee(isMarketAccount: Boolean, orderTotal: Double): Double {
        return repository.calculateDeliveryFee(isMarketAccount, orderTotal)
    }

    /**
     * Force refresh the delivery configuration data
     */
    fun refreshData() {
        viewModelScope.launch {
            repository.forceRefresh()
        }
    }

    /**
     * Load all districts (for admin view)
     */
    fun loadAllDistricts() {
        viewModelScope.launch {
            _allDistricts.value = repository.getAllDistricts()
        }
    }

    /**
     * Update delivery configuration (admin only)
     */
    fun updateDeliveryConfig(
        standardDeliveryFee: Double,
        freeDeliveryThresholdMarket: Double,
        freeDeliveryThresholdHome: Double
    ) {
        viewModelScope.launch {
            val success = repository.updateDeliveryConfig(
                standardDeliveryFee,
                freeDeliveryThresholdMarket,
                freeDeliveryThresholdHome
            )

            _actionCompleted.value = ActionResult(
                success = success,
                action = if (success) "Delivery configuration updated" else "Failed to update configuration"
            )
        }
    }

    /**
     * Add a new district
     */
    fun addDistrict(name: String) {
        viewModelScope.launch {
            val success = repository.addDistrict(name)

            _actionCompleted.value = ActionResult(
                success = success,
                action = if (success) "District added successfully" else "Failed to add district"
            )

            // Refresh the districts list for admin view
            loadAllDistricts()
        }
    }

    /**
     * Delete a district
     */
    fun deleteDistrict(districtId: String) {
        viewModelScope.launch {
            val success = repository.deleteDistrict(districtId)

            _actionCompleted.value = ActionResult(
                success = success,
                action = if (success) "District deleted successfully" else "Failed to delete district"
            )

            // Refresh the districts list for admin view
            loadAllDistricts()
        }
    }

    /**
     * Reset the action result after handling it in the UI
     */
    fun resetActionResult() {
        _actionCompleted.value = null
    }

    /**
     * Data class for action results
     */
    data class ActionResult(
        val success: Boolean,
        val action: String
    )

    /**
     * Factory for creating DeliveryConfigViewModel with dependencies
     */
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DeliveryConfigViewModel::class.java)) {
                return DeliveryConfigViewModel(Graph.deliveryConfigRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}