package com.mslabs.wayo.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ParkingSpotDao {

    @Query("SELECT * FROM parking_spots WHERE isActive = 1 LIMIT 1")
    fun getActiveSpot(): Flow<ParkingSpot?>

    @Query("SELECT * FROM parking_spots WHERE isActive = 0 ORDER BY timestamp DESC")
    fun getHistory(): Flow<List<ParkingSpot>>

    @Insert
    suspend fun insert(spot: ParkingSpot)

    @Query("UPDATE parking_spots SET isActive = 0 WHERE isActive = 1")
    suspend fun deactivateAllActive()

    @Query("DELETE FROM parking_spots WHERE isActive = 1")
    suspend fun deleteActiveSpots()

    @Delete
    suspend fun delete(spot: ParkingSpot)
}
