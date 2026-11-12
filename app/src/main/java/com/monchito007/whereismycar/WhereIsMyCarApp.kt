package com.monchito007.whereismycar

import android.app.Application
import com.monchito007.whereismycar.data.AppDatabase
import com.monchito007.whereismycar.data.ParkingRepository
import org.maplibre.android.MapLibre
import java.io.File

class WhereIsMyCarApp : Application() {
    val database by lazy { AppDatabase.create(this) }
    val parkingRepository by lazy {
        ParkingRepository(
            database.parkedLocationDao(),
            database.parkingPhotoDao(),
            database.parkingHistoryDao(),
            File(filesDir, "parking_photos"),
        )
    }

    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
    }
}
