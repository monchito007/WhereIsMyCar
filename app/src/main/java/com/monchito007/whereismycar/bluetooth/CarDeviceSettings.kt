package com.monchito007.whereismycar.bluetooth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.carBluetoothDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "car_bluetooth_settings"
)

/** Configuración del aparcamiento automático por Bluetooth. */
data class CarBluetoothConfig(
    val enabled: Boolean = false,
    val deviceAddress: String? = null,
    val deviceName: String? = null
) {
    /** Solo se actúa si el usuario lo activó y eligió un coche. */
    val isActive: Boolean get() = enabled && deviceAddress != null
}

class CarDeviceSettings(context: Context) {

    private val dataStore = context.applicationContext.carBluetoothDataStore

    val config: Flow<CarBluetoothConfig> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            CarBluetoothConfig(
                enabled = prefs[KEY_ENABLED] ?: false,
                deviceAddress = prefs[KEY_DEVICE_ADDRESS],
                deviceName = prefs[KEY_DEVICE_NAME]
            )
        }

    suspend fun current(): CarBluetoothConfig = config.first()

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_ENABLED] = enabled }
    }

    suspend fun setCarDevice(address: String, name: String?) {
        dataStore.edit { prefs ->
            prefs[KEY_DEVICE_ADDRESS] = address
            if (name != null) prefs[KEY_DEVICE_NAME] = name else prefs.remove(KEY_DEVICE_NAME)
        }
    }

    private companion object {
        val KEY_ENABLED = booleanPreferencesKey("auto_park_enabled")
        val KEY_DEVICE_ADDRESS = stringPreferencesKey("car_device_address")
        val KEY_DEVICE_NAME = stringPreferencesKey("car_device_name")
    }
}
