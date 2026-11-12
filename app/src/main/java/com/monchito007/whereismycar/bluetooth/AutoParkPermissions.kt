package com.monchito007.whereismycar.bluetooth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Permisos necesarios para el aparcamiento automático. */
object AutoParkPermissions {

    /** Permiso de Bluetooth en tiempo de ejecución (solo Android 12+; antes es de instalación). */
    val bluetoothRuntimePermission: String?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_CONNECT
        } else null

    /** La ubicación en segundo plano es un permiso aparte desde Android 10. */
    val needsSeparateBackgroundLocation: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    fun hasBluetooth(context: Context): Boolean =
        bluetoothRuntimePermission?.let { isGranted(context, it) } ?: true

    fun hasFineLocation(context: Context): Boolean =
        isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)

    fun hasBackgroundLocation(context: Context): Boolean =
        if (needsSeparateBackgroundLocation) {
            isGranted(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            hasFineLocation(context)
        }

    fun hasAll(context: Context): Boolean =
        hasBluetooth(context) && hasFineLocation(context) && hasBackgroundLocation(context)

    private fun isGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
