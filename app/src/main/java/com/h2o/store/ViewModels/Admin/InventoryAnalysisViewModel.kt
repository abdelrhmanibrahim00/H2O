package com.h2o.store.ViewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.h2o.store.data.remote.BulkPredictionResponse
import com.h2o.store.repositories.InventoryPredictionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date

/**
 * ViewModel for inventory analysis and prediction
 */
class InventoryAnalysisViewModel(
    private val inventoryPredictionRepository: InventoryPredictionRepository
) : ViewModel() {

    private val TAG = "InventoryAnalysisVM"

    // UI States
    private val _uiState = MutableStateFlow<InventoryAnalysisUiState>(InventoryAnalysisUiState.Initial)
    val uiState: StateFlow<InventoryAnalysisUiState> = _uiState

    // Prediction data
    private val _predictionData = MutableStateFlow<BulkPredictionResponse?>(null)
    val predictionData: StateFlow<BulkPredictionResponse?> = _predictionData

    // Last generated timestamp
    private val _lastGeneratedAt = MutableStateFlow<Date?>(null)
    val lastGeneratedAt: StateFlow<Date?> = _lastGeneratedAt

    /**
     * Generate inventory prediction report
     */
    fun generateInventoryReport() {
        _uiState.value = InventoryAnalysisUiState.Loading

        viewModelScope.launch {
            Log.d(TAG, "Generating inventory report")

            inventoryPredictionRepository.generateInventoryReport()
                .onSuccess { response ->
                    Log.d(TAG, "Report generation successful with ${response.predictions.size} predictions")
                    _predictionData.value = response
                    _lastGeneratedAt.value = Date() // Current time
                    _uiState.value = InventoryAnalysisUiState.Success(response)
                }
                .onFailure { error ->
                    Log.e(TAG, "Report generation failed: ${error.message}")
                    _uiState.value = InventoryAnalysisUiState.Error(error.message ?: "Unknown error occurred")
                }
        }
    }

    /**
     * Reset UI state to initial
     */
    fun resetState() {
        _uiState.value = InventoryAnalysisUiState.Initial
    }

    /**
     * UI states for inventory analysis screen
     */
    sealed class InventoryAnalysisUiState {
        object Initial : InventoryAnalysisUiState()
        object Loading : InventoryAnalysisUiState()
        data class Success(val data: BulkPredictionResponse) : InventoryAnalysisUiState()
        data class Error(val message: String) : InventoryAnalysisUiState()
    }

    /**
     * Factory for creating this ViewModel with dependencies
     */
    class Factory(
        private val inventoryPredictionRepository: InventoryPredictionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(InventoryAnalysisViewModel::class.java)) {
                return InventoryAnalysisViewModel(inventoryPredictionRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}