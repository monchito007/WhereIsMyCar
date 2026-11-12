package com.monchito007.whereismycar.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ParkedLocationDao {
    @Query("SELECT * FROM parked_locations WHERE id = :id")
    fun observe(id: Int = ParkedLocation.SINGLETON_ID): Flow<ParkedLocation?>

    @Query("SELECT * FROM parked_locations WHERE id = :id")
    suspend fun get(id: Int = ParkedLocation.SINGLETON_ID): ParkedLocation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(location: ParkedLocation)

    @Query("UPDATE parked_locations SET note = :note WHERE id = :id")
    suspend fun updateNote(note: String?, id: Int = ParkedLocation.SINGLETON_ID)

    @Query("DELETE FROM parked_locations")
    suspend fun clear()
}
