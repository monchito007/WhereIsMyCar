package com.monchito007.whereismycar.data

import androidx.room.Embedded
import androidx.room.Relation

data class ParkingWithPhotos(
    @Embedded val location: ParkedLocation,
    @Relation(
        parentColumn = "id",
        entityColumn = "parkingId",
    )
    val photos: List<ParkingPhoto>,
)
