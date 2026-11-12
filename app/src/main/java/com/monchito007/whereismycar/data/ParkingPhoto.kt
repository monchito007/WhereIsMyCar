package com.monchito007.whereismycar.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "parking_photos",
    foreignKeys = [
        ForeignKey(
            entity = ParkedLocation::class,
            parentColumns = ["id"],
            childColumns = ["parkingId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("parkingId")],
)
data class ParkingPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parkingId: Int = ParkedLocation.SINGLETON_ID,
    val fileName: String,
    val createdAtMillis: Long,
)
