package com.monchito007.whereismycar.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.monchito007.whereismycar.WhereIsMyCarApp
import com.monchito007.whereismycar.bluetooth.AutoParkLog
import com.monchito007.whereismycar.bluetooth.AutoParkPermissions
import com.monchito007.whereismycar.location.currentLocationOnce
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/*
 * Aparcamiento automático en dos pasos encadenados (un único trabajo "único" de WorkManager):
 *
 * 1. AutoParkLocationWorker (urgente/expedited): obtiene la ubicación justo al desconectarse,
 *    cuando aún estás junto al coche.
 * 2. AutoParkSaveWorker (con 60 s de retraso): guarda esa ubicación como aparcamiento.
 *
 * Si el coche se reconecta antes de que termine la espera, AutoParkScheduler.cancel() cancela la
 * cadena y no se guarda nada (microcorte en marcha o apagar/encender rápido).
 */

private const val KEY_DISCONNECTED_AT = "disconnected_at"
private const val KEY_LATITUDE = "latitude"
private const val KEY_LONGITUDE = "longitude"
private const val KEY_ACCURACY = "accuracy"

class AutoParkLocationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val log = AutoParkLog(applicationContext)
        try {
            log.add("Obteniendo ubicación…")
            if (!AutoParkPermissions.hasBackgroundLocation(applicationContext)) {
                log.add("ERROR: falta el permiso de ubicación \"Permitir todo el tiempo\"")
                return Result.failure()
            }
            val location = currentLocationOnce(applicationContext, LOCATION_TIMEOUT_MS)
            if (location == null) {
                log.add("ERROR: no se pudo obtener la ubicación (¿GPS desactivado?)")
                return Result.failure()
            }
            val accuracy = location.accuracyMeters?.let { "±${it.toInt()} m" } ?: "precisión desconocida"
            log.add("Ubicación obtenida ($accuracy). Se guardará en ${SAVE_DELAY_SECONDS} s si el coche no se reconecta")
            return Result.success(
                workDataOf(
                    KEY_DISCONNECTED_AT to inputData.getLong(KEY_DISCONNECTED_AT, System.currentTimeMillis()),
                    KEY_LATITUDE to location.latitude,
                    KEY_LONGITUDE to location.longitude,
                    KEY_ACCURACY to (location.accuracyMeters ?: -1f),
                ),
            )
        } catch (e: CancellationException) {
            log.add("Cancelado mientras se obtenía la ubicación")
            throw e
        }
    }

    /** Solo se usa en Android 11 o anterior, donde el trabajo urgente se ejecuta como servicio en primer plano. */
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Aparcamiento automático", NotificationManager.IMPORTANCE_LOW),
            )
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Guardando dónde has aparcado…")
            .setOngoing(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val LOCATION_TIMEOUT_MS = 20_000L
        const val CHANNEL_ID = "auto_park"
        const val NOTIFICATION_ID = 1001
    }
}

class AutoParkSaveWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val latitude = inputData.getDouble(KEY_LATITUDE, Double.NaN)
        val longitude = inputData.getDouble(KEY_LONGITUDE, Double.NaN)
        if (latitude.isNaN() || longitude.isNaN()) return Result.failure()
        val accuracy = inputData.getFloat(KEY_ACCURACY, -1f).takeIf { it >= 0f }

        (applicationContext as WhereIsMyCarApp).parkingRepository.save(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracy,
            savedAtMillis = inputData.getLong(KEY_DISCONNECTED_AT, System.currentTimeMillis()),
        )
        AutoParkLog(applicationContext).add(
            "✔ Aparcamiento guardado: %.5f, %.5f".format(Locale.US, latitude, longitude),
        )
        return Result.success()
    }
}

const val SAVE_DELAY_SECONDS = 60L

object AutoParkScheduler {
    private const val UNIQUE_WORK_NAME = "auto_park_on_disconnect"

    fun schedule(context: Context, disconnectedAt: Long) {
        val locate = OneTimeWorkRequestBuilder<AutoParkLocationWorker>()
            .setInputData(workDataOf(KEY_DISCONNECTED_AT to disconnectedAt))
            // Urgente: que Android no lo retrase. Si se agota la cuota, se ejecuta como normal.
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        val save = OneTimeWorkRequestBuilder<AutoParkSaveWorker>()
            .setInitialDelay(SAVE_DELAY_SECONDS, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .beginUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, locate)
            .then(save)
            .enqueue()
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}
