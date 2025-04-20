package com.h2o.store.Utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.h2o.store.ViewModels.Location.LocationViewModel
import com.h2o.store.data.models.LocationData
import java.util.Locale

class LocationUtils(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // Store the callback to allow removing updates later
    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun requestLocationUpdates(viewModel: LocationViewModel) {
        if (!hasLocationPermission(context)) {
            Log.d("LocationUtils", "Location permission not granted")
            return
        }

        try {
            // First try to get the last known location, which returns immediately
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val locationData = LocationData(it.latitude, it.longitude)
                    viewModel.updateLocation(locationData)
                    viewModel.fetchAddress("${locationData.latitude},${locationData.longitude}")
                    Log.d("LocationUtils", "Last known location: ${it.latitude}, ${it.longitude}")
                }
            }

            // Remove previous callback if exists
            locationCallback?.let {
                fusedLocationClient.removeLocationUpdates(it)
            }

            // Create new callback
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let {
                        val location = LocationData(it.latitude, it.longitude)
                        viewModel.updateLocation(location)
                        viewModel.fetchAddress("${location.latitude},${location.longitude}")
                        Log.d("LocationUtils", "New location: ${it.latitude}, ${it.longitude}")
                    }
                }
            }

            // Create location request for continuous updates
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(2000)
                .build()

            // Register for location updates
            locationCallback?.let {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    it,
                    Looper.getMainLooper()
                )
            }
        } catch (e: SecurityException) {
            Log.e("LocationUtils", "Error requesting location updates: ${e.message}")
        }
    }

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun reverseGeocodeLocation(location: LocationData): String {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses: List<Address>? = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        return addresses?.firstOrNull()?.getAddressLine(0) ?: "Address not found"
    }

    /**
     * Stops location updates
     */
    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }
}