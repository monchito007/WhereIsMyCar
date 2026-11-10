package com.monchito007.whereismycar.ui.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.monchito007.whereismycar.data.db.AppDatabase
import com.monchito007.whereismycar.data.entity.ParkedLocation
import com.monchito007.whereismycar.data.repository.ParkingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.ceil

class ParkingViewModel(
    application: Application,
    private val repository: ParkingRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application,
        ParkingRepository(
            application,
            AppDatabase.getDatabase(application).parkingDao()
        )
    )

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    val parkedLocation: StateFlow<ParkedLocation?> = repository.parkedLocation
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val distanceToCarMeters: StateFlow<Float?> = combine(
        currentLocation,
        parkedLocation
    ) { current, parked ->
        if (current != null && parked != null) {
            calculateDistance(current.latitude, current.longitude, parked.latitude, parked.longitude)
        } else {
            null
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val estimatedWalkingTimeMinutes: StateFlow<Int?> = combine(
        distanceToCarMeters
    ) { distanceArr ->
        distanceArr.firstOrNull()?.let { calculateWalkingTimeMinutes(it) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun requestCurrentLocation() {
        viewModelScope.launch {
            val location = repository.getCurrentLocation()
            _currentLocation.value = location
        }
    }

    fun updateCurrentLocation(location: Location?) {
        _currentLocation.value = location
    }

    fun saveCurrentLocationAsParking(addressOrNote: String? = null) {
        val curr = _currentLocation.value ?: return
        saveParkingLocation(
            latitude = curr.latitude,
            longitude = curr.longitude,
            addressOrNote = addressOrNote
        )
    }

    fun saveParkingLocation(
        latitude: Double,
        longitude: Double,
        addressOrNote: String? = null
    ) {
        viewModelScope.launch {
            repository.saveParkedLocation(
                latitude = latitude,
                longitude = longitude,
                addressOrNote = addressOrNote
            )
        }
    }

    fun clearParkingLocation() {
        viewModelScope.launch {
            repository.clearParkedLocation()
        }
    }

    fun calculateDistance(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(fromLat, fromLng, toLat, toLng, results)
        return results[0]
    }

    fun calculateWalkingTimeMinutes(distanceInMeters: Float): Int {
        if (distanceInMeters <= 0f) return 0
        return ceil(distanceInMeters / 80.0f).toInt()
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ParkingViewModel::class.java)) {
                return ParkingViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
