package com.monchito007.whereismycar.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ParkingHistoryDao {
    @Query("SELECT * FROM parking_history ORDER BY savedAtMillis DESC")
    fun observe(): Flow<List<ParkingHistoryEntry>>

    @Insert
    suspend fun insert(entry: ParkingHistoryEntry): Long

    @Query(
        "DELETE FROM parking_history WHERE id NOT IN " +
            "(SELECT id FROM parking_history ORDER BY savedAtMillis DESC LIMIT :keep)",
    )
    suspend fun trim(keep: Int)

    @Query("DELETE FROM parking_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM parking_history")
    suspend fun clear()
}
