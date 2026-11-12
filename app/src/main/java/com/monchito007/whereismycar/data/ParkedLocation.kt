package com.monchito007.whereismycar.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parked_locations")
data class ParkedLocation(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val latitude: Double,
    val longitude: Double,
    val savedAtMillis: Long,
    val accuracyMeters: Float?,
    val note: String? = null,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
