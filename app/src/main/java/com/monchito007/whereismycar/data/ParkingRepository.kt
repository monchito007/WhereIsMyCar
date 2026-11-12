package com.monchito007.whereismycar.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

private const val MAX_HISTORY_ENTRIES = 50

class ParkingRepository(
    private val parkedLocationDao: ParkedLocationDao,
    private val parkingPhotoDao: ParkingPhotoDao,
    private val parkingHistoryDao: ParkingHistoryDao,
    private val photosDir: File,
) {
    val parkingHistory: Flow<List<ParkingHistoryEntry>> = parkingHistoryDao.observe()
    val parkedLocation: Flow<ParkedLocation?> = parkedLocationDao.observe()
    val parkingPhotos: Flow<List<ParkingPhoto>> = parkingPhotoDao.observe()

    suspend fun save(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float?,
        savedAtMillis: Long = System.currentTimeMillis(),
    ) {
        replaceCurrentParking(
            ParkedLocation(
                latitude = latitude,
                longitude = longitude,
                savedAtMillis = savedAtMillis,
                accuracyMeters = accuracyMeters,
            ),
        )
    }

    suspend fun restoreFromHistory(entry: ParkingHistoryEntry) {
        replaceCurrentParking(
            ParkedLocation(
                latitude = entry.latitude,
                longitude = entry.longitude,
                savedAtMillis = System.currentTimeMillis(),
                accuracyMeters = null,
                note = entry.note,
            ),
        )
        parkingHistoryDao.deleteById(entry.id)
    }

    private suspend fun replaceCurrentParking(parked: ParkedLocation) {
        archiveCurrentParking()
        // REPLACE removes the previous row, and the foreign key cascades to its photos.
        val replacedPhotos = parkingPhotoDao.list()
        parkedLocationDao.save(parked)
        deleteFiles(replacedPhotos)
    }

    suspend fun updateNote(note: String) {
        parkedLocationDao.updateNote(note.trim().ifEmpty { null })
    }

    suspend fun addPhoto(fileName: String) {
        parkingPhotoDao.insert(
            ParkingPhoto(
                parkingId = ParkedLocation.SINGLETON_ID,
                fileName = fileName,
                createdAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun deletePhoto(id: Long) {
        val photo = parkingPhotoDao.getById(id) ?: return
        parkingPhotoDao.deleteById(id)
        deleteFiles(listOf(photo))
    }

    suspend fun deleteHistoryEntry(id: Long) {
        parkingHistoryDao.deleteById(id)
    }

    suspend fun clearHistory() {
        parkingHistoryDao.clear()
    }

    private suspend fun archiveCurrentParking() {
        val current = parkedLocationDao.get() ?: return
        parkingHistoryDao.insert(
            ParkingHistoryEntry(
                latitude = current.latitude,
                longitude = current.longitude,
                savedAtMillis = current.savedAtMillis,
                endedAtMillis = System.currentTimeMillis(),
                note = current.note,
            ),
        )
        parkingHistoryDao.trim(MAX_HISTORY_ENTRIES)
    }

    suspend fun clear() {
        archiveCurrentParking()
        val photos = parkingPhotoDao.list()
        parkingPhotoDao.clear()
        parkedLocationDao.clear()
        deleteFiles(photos)
    }

    private suspend fun deleteFiles(photos: List<ParkingPhoto>) {
        withContext(Dispatchers.IO) {
            photos.forEach { File(photosDir, it.fileName).delete() }
        }
    }
}
