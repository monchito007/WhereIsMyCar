package com.monchito007.whereismycar.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.IntentCompat
import com.monchito007.whereismycar.worker.AutoParkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Escucha las conexiones/desconexiones Bluetooth. Si el dispositivo es el coche elegido,
 * programa (o cancela) el guardado automático del aparcamiento.
 */
class CarBluetoothReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission") // getAddress() no requiere BLUETOOTH_CONNECT
    override fun onReceive(context: Context, intent: Intent) {
        val event = when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> CarConnectionEvent.CONNECTED
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> CarConnectionEvent.DISCONNECTED
            else -> return
        }
        val device = IntentCompat.getParcelableExtra(
            intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java
        ) ?: return
        val address = device.address

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val config = CarDeviceSettings(appContext).current()
                when (decideAutoParkAction(event, address, config)) {
                    AutoParkAction.START -> {
                        Log.d(TAG, "Coche desconectado ($address): programando guardado")
                        AutoParkScheduler.schedule(appContext, disconnectedAt = System.currentTimeMillis())
                    }
                    AutoParkAction.CANCEL -> {
                        Log.d(TAG, "Coche reconectado ($address): cancelando guardado pendiente")
                        AutoParkScheduler.cancel(appContext)
                    }
                    AutoParkAction.IGNORE -> Unit
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "CarBluetoothReceiver"
    }
}
