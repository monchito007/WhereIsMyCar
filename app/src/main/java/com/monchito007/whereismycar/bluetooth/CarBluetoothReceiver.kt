package com.monchito007.whereismycar.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
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
        // Se lee aquí, de forma síncrona: al apagar el Bluetooth del móvil el adaptador ya está
        // en TURNING_OFF (u OFF) cuando llegan los ACL_DISCONNECTED.
        val phoneBluetoothOn = isPhoneBluetoothOn(context)

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                handle(appContext, event, address, name, phoneBluetoothOn)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handle(
        context: Context,
        event: CarConnectionEvent,
        address: String?,
        name: String?,
        phoneBluetoothOn: Boolean,
    ) {
        val log = AutoParkLog(context)
        val config = CarDeviceSettings(context).current()
        val what = if (event == CarConnectionEvent.CONNECTED) "Conectado" else "Desconectado"
        val who = "${name ?: "?"} ($address)"

        when (decideAutoParkAction(event, address, config, phoneBluetoothOn)) {
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
                    address != null && address.equals(config.deviceAddress, ignoreCase = true) ->
                        "se ha apagado el Bluetooth del móvil, no es que hayas aparcado"
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

/**
 * true si el Bluetooth del móvil está encendido (estado ON). Durante el apagado el estado es
 * TURNING_OFF, así que las desconexiones que provoca apagarlo no cuentan como aparcamiento.
 * getState() no necesita permisos en tiempo de ejecución.
 */
internal fun isPhoneBluetoothOn(context: Context): Boolean {
    val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return false
    return try {
        adapter.state == BluetoothAdapter.STATE_ON
    } catch (_: SecurityException) {
        true // Si no se puede saber, se mantiene el comportamiento anterior.
    }
}
