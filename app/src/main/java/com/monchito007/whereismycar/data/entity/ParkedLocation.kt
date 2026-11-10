package com.monchito007.whereismycar.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parked_location")
data class ParkedLocation(
    @PrimaryKey val id: Int = 1,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val addressOrNote: String? = null
)
