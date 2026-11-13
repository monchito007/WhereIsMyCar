package com.monchito007.whereismycar.ui

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.monchito007.whereismycar.WhereIsMyCarApp
import com.monchito007.whereismycar.data.ParkedLocation
import com.monchito007.whereismycar.data.ParkingHistoryEntry
import com.monchito007.whereismycar.data.ParkingPhoto
import com.monchito007.whereismycar.location.Geo
import com.monchito007.whereismycar.location.LocationTracker
import com.monchito007.whereismycar.location.UserLocation
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraPosition

data class ParkingUiState(
    val hasLocationPermission: Boolean = false,
    val userLocation: UserLocation? = null,
    val parkedLocation: ParkedLocation? = null,
    val photos: List<ParkingPhoto> = emptyList(),
    val distanceMeters: Double? = null,
    val walkingMinutes: Int? = null,
    val isSaving: Boolean = false,
    val message: String? = null,
    val mapStyle: MapStyle = MapStyle.LIBERTY,
    val history: List<ParkingHistoryEntry> = emptyList(),
)

class ParkingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as WhereIsMyCarApp).parkingRepository
    private val locationTracker = LocationTracker(application)
    private var locationJob: Job? = null
    private var isInForeground = false
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(ParkingUiState(mapStyle = savedMapStyle()))
    val uiState: StateFlow<ParkingUiState> = _uiState.asStateFlow()

    /**
     * Última posición de la cámara del mapa. El mapa se destruye al abrir otra pantalla
     * (p. ej. "Aparcamiento automático") y al girar el móvil; con esto vuelve donde estaba.
     */
    var lastCameraPosition: CameraPosition? = null

    init {
        viewModelScope.launch {
            repository.parkingHistory.collect { history ->
                _uiState.update { it.copy(history = history) }
            }
        }
        viewModelScope.launch {
            repository.parkedLocation.collect { parked ->
                _uiState.update { current ->
                    current.copy(parkedLocation = parked).withDistance()
                }
            }
        }
        viewModelScope.launch {
            repository.parkingPhotos.collect { photos ->
                _uiState.update { current ->
                    current.copy(photos = photos)
                }
            }
        }
    }

    fun onPermissionChanged(granted: Boolean) {
        _uiState.update { it.copy(hasLocationPermission = granted) }
        syncLocationUpdates()
    }

    fun onForegroundChanged(foreground: Boolean) {
        isInForeground = foreground
        syncLocationUpdates()
    }

    fun saveCurrentParking() {
        val user = _uiState.value.userLocation
        if (user == null) {
            _uiState.update { it.copy(message = MESSAGE_NO_GPS) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            repository.save(user.latitude, user.longitude, user.accuracyMeters)
            _uiState.update { it.copy(isSaving = false, message = MESSAGE_PARKED) }
        }
    }

    fun clearParking() {
        viewModelScope.launch {
            repository.clear()
            _uiState.update { it.copy(message = MESSAGE_DELETED) }
        }
    }

    fun setMapStyle(style: MapStyle) {
        prefs.edit { putString(KEY_MAP_STYLE, style.name) }
        _uiState.update { it.copy(mapStyle = style) }
    }

    private fun savedMapStyle(): MapStyle {
        val saved = prefs.getString(KEY_MAP_STYLE, null)
        return MapStyle.entries.firstOrNull { it.name == saved } ?: MapStyle.LIBERTY
    }

    fun restoreFromHistory(entry: ParkingHistoryEntry) {
        viewModelScope.launch {
            repository.restoreFromHistory(entry)
            _uiState.update { it.copy(message = MESSAGE_PARKED) }
        }
    }

    fun deleteHistoryEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteHistoryEntry(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun updateNote(note: String) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    fun addPhoto(fileName: String) {
        viewModelScope.launch {
            repository.addPhoto(fileName)
        }
    }

    fun deletePhoto(id: Long) {
        viewModelScope.launch {
            repository.deletePhoto(id)
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun reportNoNavigationApp() {
        _uiState.update { it.copy(message = MESSAGE_NO_NAV) }
    }

    private fun syncLocationUpdates() {
        if (isInForeground && _uiState.value.hasLocationPermission) {
            startLocationUpdates()
        } else {
            locationJob?.cancel()
            locationJob = null
        }
    }

    private fun startLocationUpdates() {
        if (locationJob?.isActive == true) return
        locationJob = viewModelScope.launch {
            locationTracker.updates().collect { location ->
                _uiState.update { current ->
                    current.copy(userLocation = location).withDistance()
                }
            }
        }
    }

    private fun ParkingUiState.withDistance(): ParkingUiState {
        val user = userLocation
        val parked = parkedLocation
        if (user == null || parked == null) {
            return copy(distanceMeters = null, walkingMinutes = null)
        }
        val distance = Geo.haversineMeters(
            user.latitude,
            user.longitude,
            parked.latitude,
            parked.longitude,
        )
        return copy(
            distanceMeters = distance,
            walkingMinutes = Geo.walkingMinutes(distance),
        )
    }

    companion object {
        private const val PREFS_NAME = "settings"
        private const val KEY_MAP_STYLE = "map_style"
        const val MESSAGE_NO_GPS = "cannot_park"
        const val MESSAGE_PARKED = "parked_ok"
        const val MESSAGE_DELETED = "deleted_ok"
        const val MESSAGE_NO_NAV = "no_nav_app"
    }
}
