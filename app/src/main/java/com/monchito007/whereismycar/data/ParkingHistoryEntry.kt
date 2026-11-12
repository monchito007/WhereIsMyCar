package com.monchito007.whereismycar.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parking_history")
data class ParkingHistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val savedAtMillis: Long,
    val endedAtMillis: Long,
    val note: String?,
)
