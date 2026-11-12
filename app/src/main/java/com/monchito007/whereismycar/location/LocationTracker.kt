package com.monchito007.whereismycar.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LocationTracker(private val context: Context) {
    @SuppressLint("MissingPermission")
    fun updates(): Flow<UserLocation> = callbackFlow {
        val fused = LocationServices.getFusedLocationProviderClient(context)
        var usingFused = true

        val fusedCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.toUserLocation()?.let { trySend(it) }
            }
        }

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val androidListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location.toUserLocation())
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) = Unit
        }

        fun startAndroidUpdates() {
            usingFused = false
            val provider = when {
                manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> manager.getProviders(true).firstOrNull()
            }
            if (provider != null) {
                manager.getLastKnownLocation(provider)?.toUserLocation()?.let { trySend(it) }
                manager.requestLocationUpdates(provider, 2_000L, 3f, androidListener, Looper.getMainLooper())
            }
        }

        try {
            fused.lastLocation.addOnSuccessListener { location ->
                location?.toUserLocation()?.let { trySend(it) }
            }
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000L)
                .setMinUpdateDistanceMeters(3f)
                .build()
            fused.requestLocationUpdates(request, fusedCallback, Looper.getMainLooper())
                .addOnFailureListener { startAndroidUpdates() }
        } catch (_: Exception) {
            startAndroidUpdates()
        }

        awaitClose {
            if (usingFused) {
                fused.removeLocationUpdates(fusedCallback)
            } else {
                manager.removeUpdates(androidListener)
            }
        }
    }
}

private fun Location.toUserLocation() = UserLocation(
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = if (hasAccuracy()) accuracy else null,
)
