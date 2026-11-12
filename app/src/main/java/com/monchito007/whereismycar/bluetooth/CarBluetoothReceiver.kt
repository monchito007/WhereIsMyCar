package com.monchito007.whereismycar.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.IntentCompat
import com.monchito007.whereismycar.worker.AutoParkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Escucha las conexiones/desconexiones Bluetooth. Si el dispositivo es el coche elegido,
 * programa (o cancela) el guardado automático del aparcamiento. Todo queda en [AutoParkLog].
 */
class CarBluetoothReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> CarConnectionEvent.CONNECTED
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> CarConnectionEvent.DISCONNECTED
            else -> return
        }
        val device = IntentCompat.getParcelableExtra(
            intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java,
        )
        val address = device?.address
        val name = device?.safeName()

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                handle(appContext, event, address, name)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handle(context: Context, event: CarConnectionEvent, address: String?, name: String?) {
        val log = AutoParkLog(context)
        val config = CarDeviceSettings(context).current()
        val what = if (event == CarConnectionEvent.CONNECTED) "Conectado" else "Desconectado"
        val who = "${name ?: "?"} ($address)"

        when (decideAutoParkAction(event, address, config)) {
            AutoParkAction.START -> {
                log.add("$what: $who → es tu coche, se programa el guardado")
                AutoParkScheduler.schedule(context, disconnectedAt = System.currentTimeMillis())
            }
            AutoParkAction.CANCEL -> {
                log.add("$what: $who → es tu coche, se cancela cualquier guardado pendiente")
                AutoParkScheduler.cancel(context)
            }
            AutoParkAction.IGNORE -> {
                val reason = when {
                    !config.enabled -> "la función está desactivada"
                    config.deviceAddress == null -> "no hay coche elegido"
                    else -> "no es tu coche (${config.deviceName} ${config.deviceAddress})"
                }
                log.add("$what: $who → se ignora: $reason")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.safeName(): String? =
        try {
            name
        } catch (_: SecurityException) {
            null // sin permiso BLUETOOTH_CONNECT
        }
}
