package com.h2o.store.repositories

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.h2o.store.data.models.DeliveryConfig
import com.h2o.store.data.models.District
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class DeliveryConfigRepository(private val context: Context) {
    private val TAG = "DeliveryConfigRepo"

    // Firestore references
    private val firestore = FirebaseFirestore.getInstance()
    private val configDocRef = firestore.collection("delivery_config").document("global_config")
    private val districtsCollectionRef = firestore.collection("districts")

    // Cache keys
    private val PREFS_NAME = "delivery_config_prefs"
    private val KEY_CONFIG_CACHE = "delivery_config_cache"
    private val KEY_DISTRICTS_CACHE = "districts_cache"
    private val KEY_LAST_FETCH_TIME = "last_fetch_time"
    private val CACHE_EXPIRATION_HOURS = 6L // Cache expiration time in hours

    // JSON parser
    private val gson = Gson()

    // Shared preferences for caching
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // StateFlows to emit the latest data
    private val _deliveryConfig = MutableStateFlow<DeliveryConfig?>(null)
    val deliveryConfig = _deliveryConfig.asStateFlow()

    private val _districts = MutableStateFlow<List<District>>(emptyList())
    val districts = _districts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    /**
     * Initialize repository and fetch initial data if needed
     */
    suspend fun initialize() {
        if (shouldRefreshCache()) {
            fetchAllData()
        } else {
            loadFromCache()
        }
    }

    /**
     * Fetch all delivery configuration data from Firestore
     * and update the cache
     */
    suspend fun fetchAllData() {
        _isLoading.value = true
        _error.value = null

        try {
            withContext(Dispatchers.IO) {
                // Fetch delivery config
                val configSnapshot = configDocRef.get().await()
                val config = configSnapshot.toObject(DeliveryConfig::class.java) ?: DeliveryConfig()
                _deliveryConfig.value = config

                // Fetch active districts
                val districtsSnapshot = districtsCollectionRef
                    .whereEqualTo("isActive", true)
                    .get()
                    .await()

                val districtsList = districtsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(District::class.java)
                }
                _districts.value = districtsList

                // Update cache
                updateCache(config, districtsList)
            }
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Error fetching delivery config data", e)
            _error.value = "Failed to load delivery configuration: ${e.message}"
            // If we fail to fetch, try to load from cache as fallback
            if (_deliveryConfig.value == null) {
                loadFromCache()
            }
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Check if we should refresh the cache based on last fetch time
     */
    private fun shouldRefreshCache(): Boolean {
        val lastFetchTime = prefs.getLong(KEY_LAST_FETCH_TIME, 0L)
        val currentTime = System.currentTimeMillis()
        val expirationTime = TimeUnit.HOURS.toMillis(CACHE_EXPIRATION_HOURS)

        return (currentTime - lastFetchTime) > expirationTime
    }

    /**
     * Load delivery configuration from local cache
     */
    private fun loadFromCache() {
        try {
            val configJson = prefs.getString(KEY_CONFIG_CACHE, null)
            val districtsJson = prefs.getString(KEY_DISTRICTS_CACHE, null)

            if (configJson != null) {
                _deliveryConfig.value = gson.fromJson(configJson, DeliveryConfig::class.java)
            }

            if (districtsJson != null) {
                val type = object : TypeToken<List<District>>() {}.type
                _districts.value = gson.fromJson(districtsJson, type)
            }

            Log.d(TAG, "Loaded from cache: config=${_deliveryConfig.value != null}, " +
                    "districts=${_districts.value.size} items")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading from cache", e)
            _error.value = "Failed to load cached data: ${e.message}"
        }
    }

    /**
     * Update the local cache with the latest data
     */
    private fun updateCache(config: DeliveryConfig, districts: List<District>) {
        try {
            val configJson = gson.toJson(config)
            val districtsJson = gson.toJson(districts)

            prefs.edit()
                .putString(KEY_CONFIG_CACHE, configJson)
                .putString(KEY_DISTRICTS_CACHE, districtsJson)
                .putLong(KEY_LAST_FETCH_TIME, System.currentTimeMillis())
                .apply()

            Log.d(TAG, "Updated cache with config and ${districts.size} districts")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating cache", e)
        }
    }

    /**
     * Get the current delivery fee based on account type and order total
     */
    fun calculateDeliveryFee(isMarketAccount: Boolean, orderTotal: Double): Double {
        val config = _deliveryConfig.value ?: return 0.0

        return when {
            isMarketAccount && orderTotal >= config.freeDeliveryThresholdMarket -> 0.0
            !isMarketAccount && orderTotal >= config.freeDeliveryThresholdHome -> 0.0
            else -> config.standardDeliveryFee
        }
    }

    // Admin functions

    /**
     * Update the delivery configuration values
     */
    suspend fun updateDeliveryConfig(
        standardDeliveryFee: Double,
        freeDeliveryThresholdMarket: Double,
        freeDeliveryThresholdHome: Double
    ): Boolean {
        _isLoading.value = true
        _error.value = null

        try {
            Log.d(TAG, "Updating config: fee=$standardDeliveryFee, marketThreshold=$freeDeliveryThresholdMarket, homeThreshold=$freeDeliveryThresholdHome")

            val updatedConfig = DeliveryConfig(
                standardDeliveryFee = standardDeliveryFee,
                freeDeliveryThresholdMarket = freeDeliveryThresholdMarket,
                freeDeliveryThresholdHome = freeDeliveryThresholdHome
            )

            withContext(Dispatchers.IO) {
                try {
                    configDocRef.set(updatedConfig, SetOptions.merge()).await()
                    Log.d(TAG, "Config updated successfully")
                    _deliveryConfig.value = updatedConfig

                    // Update cache with the new config
                    _districts.value?.let { updateCache(updatedConfig, it) }
                } catch (e: Exception) {
                    Log.e(TAG, "Firestore operation failed", e)
                    throw e
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating delivery config", e)
            _error.value = "Failed to update delivery configuration: ${e.message}"
            return false
        } finally {
            _isLoading.value = false
        }
    }



    /**
     * Get all districts (active and inactive) - admin only
     */
    suspend fun getAllDistricts(): List<District> {
        _isLoading.value = true
        _error.value = null

        return try {
            withContext(Dispatchers.IO) {
                val snapshot = districtsCollectionRef.get().await()
                snapshot.documents.mapNotNull { it.toObject(District::class.java) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all districts", e)
            _error.value = "Failed to fetch districts: ${e.message}"
            emptyList()
        } finally {
            _isLoading.value = false
        }
    }



    /**
     * Add a new district
     */
    suspend fun addDistrict(name: String): Boolean {
        _isLoading.value = true
        _error.value = null

        try {
            // Check if district already exists
            val existingDistrictQuery = districtsCollectionRef
                .whereEqualTo("name", name)
                .limit(1)
                .get()
                .await()

            if (existingDistrictQuery.documents.isNotEmpty()) {
                _error.value = "A district with this name already exists"
                return false
            } else {
                // Add new district
                val district = District(
                    name = name
                )

                withContext(Dispatchers.IO) {
                    districtsCollectionRef.add(district).await()
                }
            }

            // Refresh districts list
            fetchAllData()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding district", e)
            _error.value = "Failed to add district: ${e.message}"
            return false
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Delete a district
     */
    suspend fun deleteDistrict(districtId: String): Boolean {
        _isLoading.value = true
        _error.value = null

        try {
            withContext(Dispatchers.IO) {
                districtsCollectionRef.document(districtId)
                    .delete()
                    .await()
            }

            // Update the districts list
            val updatedList = _districts.value.filter { it.id != districtId }
            _districts.value = updatedList

            // Update cache
            _deliveryConfig.value?.let { updateCache(it, updatedList) }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting district", e)
            _error.value = "Failed to delete district: ${e.message}"
            return false
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Force refresh the data from Firestore
     */
    suspend fun forceRefresh() {
        fetchAllData()
    }
}