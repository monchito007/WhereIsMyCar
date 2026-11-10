package com.monchito007.whereismycar.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.monchito007.whereismycar.data.dao.ParkingDao
import com.monchito007.whereismycar.data.entity.ParkedLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class ParkingRepository(
    private val parkingDao: ParkingDao,
    private val fusedLocationClient: FusedLocationProviderClient
) {
    constructor(context: Context, parkingDao: ParkingDao) : this(
        parkingDao,
        LocationServices.getFusedLocationProviderClient(context.applicationContext)
    )

    val parkedLocation: Flow<ParkedLocation?> = parkingDao.getParkedLocation()

    suspend fun saveParkedLocation(
        latitude: Double,
        longitude: Double,
        addressOrNote: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val location = ParkedLocation(
            id = 1,
            latitude = latitude,
            longitude = longitude,
            timestamp = timestamp,
            addressOrNote = addressOrNote
        )
        parkingDao.upsertParkedLocation(location)
    }

    suspend fun saveParkedLocation(location: ParkedLocation) {
        parkingDao.upsertParkedLocation(location.copy(id = 1))
    }

    suspend fun clearParkedLocation() {
        parkingDao.clearParkedLocation()
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location: Location? ->
                if (location != null) {
                    continuation.resume(location)
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        continuation.resume(lastLoc)
                    }.addOnFailureListener {
                        continuation.resume(null)
                    }
                }
            }.addOnFailureListener {
                continuation.resume(null)
            }

            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
        }
    }
}
