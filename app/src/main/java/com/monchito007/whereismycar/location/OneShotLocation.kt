package com.monchito007.whereismycar.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Obtiene una única posición (pensado para segundo plano, p. ej. el aparcamiento automático).
 * Pide una posición actual de alta precisión; si no llega a tiempo, usa la última conocida.
 * El llamador debe haber comprobado los permisos de ubicación.
 */
@SuppressLint("MissingPermission")
suspend fun currentLocationOnce(context: Context, timeoutMillis: Long): UserLocation? {
    val fused = LocationServices.getFusedLocationProviderClient(context)

    val current = withTimeoutOrNull(timeoutMillis) {
        suspendCancellableCoroutine<Location?> { continuation ->
            val cancellation = CancellationTokenSource()
            fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token)
                .addOnSuccessListener { continuation.resume(it) }
                .addOnFailureListener { continuation.resume(null) }
            continuation.invokeOnCancellation { cancellation.cancel() }
        }
    }

    val location = current ?: suspendCancellableCoroutine<Location?> { continuation ->
        fused.lastLocation
            .addOnSuccessListener { continuation.resume(it) }
            .addOnFailureListener { continuation.resume(null) }
    }

    return location?.let {
        UserLocation(
            latitude = it.latitude,
            longitude = it.longitude,
            accuracyMeters = if (it.hasAccuracy()) it.accuracy else null,
        )
    }
}
