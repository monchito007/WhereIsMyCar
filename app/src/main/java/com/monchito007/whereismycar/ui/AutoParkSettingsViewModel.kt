package com.monchito007.whereismycar.ui

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothManager
import androidx.core.content.getSystemService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.monchito007.whereismycar.bluetooth.AutoParkLog
import com.monchito007.whereismycar.bluetooth.AutoParkPermissions
import com.monchito007.whereismycar.worker.AutoParkScheduler
import com.monchito007.whereismycar.bluetooth.CarBluetoothConfig
import com.monchito007.whereismycar.bluetooth.CarDeviceSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PairedDevice(val name: String, val address: String)

sealed interface PairedDevicesState {
    data object NoPermission : PairedDevicesState
    data object Unsupported : PairedDevicesState
    data object BluetoothOff : PairedDevicesState
    data class Loaded(val devices: List<PairedDevice>) : PairedDevicesState
}

class AutoParkSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = CarDeviceSettings(application)

    val config: StateFlow<CarBluetoothConfig> = settings.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CarBluetoothConfig())

    private val _pairedDevices = MutableStateFlow<PairedDevicesState>(PairedDevicesState.NoPermission)
    val pairedDevices: StateFlow<PairedDevicesState> = _pairedDevices.asStateFlow()

    @SuppressLint("MissingPermission") // comprobado con AutoParkPermissions.hasBluetooth
    fun refreshPairedDevices() {
        val context = getApplication<Application>()
        val adapter = context.getSystemService<BluetoothManager>()?.adapter
        _pairedDevices.value = when {
            adapter == null -> PairedDevicesState.Unsupported
            !AutoParkPermissions.hasBluetooth(context) -> PairedDevicesState.NoPermission
            !adapter.isEnabled -> PairedDevicesState.BluetoothOff
            else -> PairedDevicesState.Loaded(
                adapter.bondedDevices.orEmpty()
                    .map { PairedDevice(name = it.name ?: it.address, address = it.address) }
                    .sortedBy { it.name.lowercase() },
            )
        }
    }

    fun selectDevice(device: PairedDevice) {
        viewModelScope.launch { settings.setCarDevice(device.address, device.name) }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setEnabled(enabled)
            log.add(if (enabled) "Aparcamiento automático activado" else "Aparcamiento automático desactivado")
        }
    }

    // --- Registro de actividad ---

    private val log = AutoParkLog(application)

    val logEntries: StateFlow<List<String>> = log.entries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearLog() {
        viewModelScope.launch { log.clear() }
    }

    /** Solo en versiones de prueba: simula que el coche se desconecta, sin Bluetooth real. */
    fun simulateDisconnect() {
        viewModelScope.launch {
            log.add("SIMULACIÓN: desconexión del coche")
            AutoParkScheduler.schedule(getApplication(), disconnectedAt = System.currentTimeMillis())
        }
    }

    /** Solo en versiones de prueba: simula que el coche se vuelve a conectar. */
    fun simulateReconnect() {
        viewModelScope.launch {
            log.add("SIMULACIÓN: reconexión del coche → se cancela cualquier guardado pendiente")
            AutoParkScheduler.cancel(getApplication())
        }
    }
}
