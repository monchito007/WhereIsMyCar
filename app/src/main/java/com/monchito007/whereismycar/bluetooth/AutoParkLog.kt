package com.monchito007.whereismycar.bluetooth

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.autoParkLogDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "auto_park_log",
)

/**
 * Registro de actividad del aparcamiento automático (últimos [MAX_ENTRIES] eventos, el más
 * reciente primero). Se muestra en la pantalla de ajustes para diagnosticar fallos en el coche,
 * donde no hay acceso a logcat.
 */
class AutoParkLog(context: Context) {

    private val dataStore = context.applicationContext.autoParkLogDataStore

    val entries: Flow<List<String>> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[KEY_ENTRIES].toLines() }

    suspend fun add(message: String) {
        Log.d(TAG, message)
        val time = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(Date())
        dataStore.edit { prefs ->
            val lines = listOf("$time  $message") + prefs[KEY_ENTRIES].toLines()
            prefs[KEY_ENTRIES] = lines.take(MAX_ENTRIES).joinToString(SEPARATOR)
        }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(KEY_ENTRIES) }
    }

    private fun String?.toLines(): List<String> =
        this?.split(SEPARATOR)?.filter { it.isNotBlank() }.orEmpty()

    private companion object {
        const val TAG = "AutoParkLog"
        const val MAX_ENTRIES = 40
        const val SEPARATOR = "\n"
        val KEY_ENTRIES = stringPreferencesKey("entries")
    }
}
