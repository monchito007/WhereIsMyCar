package com.monchito007.whereismycar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ParkedLocation::class, ParkingPhoto::class, ParkingHistoryEntry::class],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun parkedLocationDao(): ParkedLocationDao
    abstract fun parkingPhotoDao(): ParkingPhotoDao
    abstract fun parkingHistoryDao(): ParkingHistoryDao

    companion object {
        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "where_is_my_car.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `parking_history` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`latitude` REAL NOT NULL, " +
                        "`longitude` REAL NOT NULL, " +
                        "`savedAtMillis` INTEGER NOT NULL, " +
                        "`endedAtMillis` INTEGER NOT NULL, " +
                        "`note` TEXT)",
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `parked_locations` ADD COLUMN `note` TEXT")
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `parking_photos` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`parkingId` INTEGER NOT NULL, " +
                        "`fileName` TEXT NOT NULL, " +
                        "`createdAtMillis` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`parkingId`) REFERENCES `parked_locations`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_parking_photos_parkingId` " +
                        "ON `parking_photos` (`parkingId`)",
                )
            }
        }
    }
}
