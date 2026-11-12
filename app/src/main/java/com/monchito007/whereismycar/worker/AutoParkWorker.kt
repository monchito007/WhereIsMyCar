package com.monchito007.whereismycar.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.monchito007.whereismycar.WhereIsMyCarApp
import com.monchito007.whereismycar.bluetooth.AutoParkPermissions
import com.monchito007.whereismycar.location.currentLocationOnce
import kotlinx.coroutines.delay

/**
 * Se lanza al desconectarse el coche:
 * 1. Obtiene la ubicación en ese momento (sigues junto al coche).
 * 2. Espera [DEBOUNCE_MS]. Si el coche se reconecta antes, el trabajo se cancela
 *    (microcorte en marcha o apagar/encender rápido) y no se guarda nada.
 * 3. Guarda la ubicación como aparcamiento actual. El anterior pasa al historial,
 *    igual que al aparcar a mano.
 */
class AutoParkWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        if (!AutoParkPermissions.hasBackgroundLocation(context)) {
            Log.w(TAG, "Sin permiso de ubicación en segundo plano: no se guarda")
            return Result.failure()
        }

        val disconnectedAt = inputData.getLong(KEY_DISCONNECTED_AT, System.currentTimeMillis())

        val location = currentLocationOnce(context, LOCATION_TIMEOUT_MS)
        if (location == null) {
            Log.w(TAG, "No se pudo obtener la ubicación")
            return Result.failure()
        }

        // Si el coche se reconecta durante la espera, AutoParkScheduler.cancel() interrumpe aquí.
        delay(DEBOUNCE_MS)

        (context as WhereIsMyCarApp).parkingRepository.save(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracyMeters,
            savedAtMillis = disconnectedAt,
        )
        Log.d(TAG, "Aparcamiento guardado: ${location.latitude}, ${location.longitude} (±${location.accuracyMeters} m)")
        return Result.success()
    }

    companion object {
        const val KEY_DISCONNECTED_AT = "disconnected_at"
        const val LOCATION_TIMEOUT_MS = 20_000L
        const val DEBOUNCE_MS = 60_000L
        private const val TAG = "AutoParkWorker"
    }
}

object AutoParkScheduler {
    private const val UNIQUE_WORK_NAME = "auto_park_on_disconnect"

    fun schedule(context: Context, disconnectedAt: Long) {
        val request = OneTimeWorkRequestBuilder<AutoParkWorker>()
            .setInputData(workDataOf(AutoParkWorker.KEY_DISCONNECTED_AT to disconnectedAt))
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}
