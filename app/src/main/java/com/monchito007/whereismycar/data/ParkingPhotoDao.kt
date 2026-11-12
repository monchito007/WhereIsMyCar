package com.monchito007.whereismycar.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ParkingPhotoDao {
    @Insert
    suspend fun insert(photo: ParkingPhoto): Long

    @Query("SELECT * FROM parking_photos WHERE parkingId = :parkingId ORDER BY createdAtMillis ASC")
    suspend fun list(parkingId: Int = ParkedLocation.SINGLETON_ID): List<ParkingPhoto>

    @Query("SELECT * FROM parking_photos WHERE parkingId = :parkingId ORDER BY createdAtMillis ASC")
    fun observe(parkingId: Int = ParkedLocation.SINGLETON_ID): Flow<List<ParkingPhoto>>

    @Query("SELECT COUNT(*) FROM parking_photos WHERE parkingId = :parkingId")
    suspend fun count(parkingId: Int = ParkedLocation.SINGLETON_ID): Int

    @Query("SELECT * FROM parking_photos WHERE id = :id")
    suspend fun getById(id: Long): ParkingPhoto?

    @Query("DELETE FROM parking_photos WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM parking_photos")
    suspend fun clear()
}
