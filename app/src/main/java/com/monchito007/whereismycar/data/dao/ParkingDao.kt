package com.monchito007.whereismycar.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.monchito007.whereismycar.data.entity.ParkedLocation
import kotlinx.coroutines.flow.Flow

@Dao
interface ParkingDao {
    @Upsert
    suspend fun upsertParkedLocation(location: ParkedLocation)

    @Query("SELECT * FROM parked_location WHERE id = 1")
    fun getParkedLocation(): Flow<ParkedLocation?>

    @Query("DELETE FROM parked_location WHERE id = 1")
    suspend fun clearParkedLocation()
}
