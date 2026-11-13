package com.monchito007.whereismycar.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.SystemClock
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Una posición junto con el momento (hora de reloj, en ms) en que se tomó. */
data class LocationFix(
    val location: UserLocation,
    val fixTimeMillis: Long,
    /** true si viene de la última posición conocida porque no llegó una actual a tiempo. */
    val fromLastKnown: Boolean,
)

/** Antigüedad máxima que aceptamos para una posición "actual" que Google Play devuelva de caché. */
private const val MAX_CURRENT_FIX_AGE_MS = 15_000L

/**
 * Obtiene una única posición (pensado para segundo plano, p. ej. el aparcamiento automático).
 * Pide una posición actual de alta precisión; si no llega a tiempo, usa la última conocida.
 * Devuelve también cuándo se tomó, para que el llamador descarte posiciones viejas.
 * El llamador debe haber comprobado los permisos de ubicación.
 */
@SuppressLint("MissingPermission")
suspend fun currentLocationOnce(context: Context, timeoutMillis: Long): LocationFix? {
    val fused = LocationServices.getFusedLocationProviderClient(context)

    val current = withTimeoutOrNull(timeoutMillis) {
        suspendCancellableCoroutine<Location?> { continuation ->
            val cancellation = CancellationTokenSource()
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMaxUpdateAgeMillis(MAX_CURRENT_FIX_AGE_MS)
                .setDurationMillis(timeoutMillis)
                .build()
            fused.getCurrentLocation(request, cancellation.token)
                .addOnSuccessListener { continuation.resume(it) }
                .addOnFailureListener { continuation.resume(null) }
            continuation.invokeOnCancellation { cancellation.cancel() }
        }
    }
    if (current != null) return current.toFix(fromLastKnown = false)

    val last = suspendCancellableCoroutine<Location?> { continuation ->
        fused.lastLocation
            .addOnSuccessListener { continuation.resume(it) }
            .addOnFailureListener { continuation.resume(null) }
    }
    return last?.toFix(fromLastKnown = true)
}

private fun Location.toFix(fromLastKnown: Boolean) = LocationFix(
    location = UserLocation(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = if (hasAccuracy()) accuracy else null,
    ),
    fixTimeMillis = wallClockFixTime(),
    fromLastKnown = fromLastKnown,
)

/**
 * Hora de la posición en reloj de pared. Se calcula a partir de elapsedRealtimeNanos (reloj
 * monótono), que es fiable aunque la hora del GPS o del sistema esté desajustada.
 */
private fun Location.wallClockFixTime(): Long {
    val ageMillis = (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000L
    return System.currentTimeMillis() - ageMillis.coerceAtLeast(0L)
}
