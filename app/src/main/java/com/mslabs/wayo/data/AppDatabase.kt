package com.mslabs.wayo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ParkingSpot::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun parkingSpotDao(): ParkingSpotDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wayo.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
