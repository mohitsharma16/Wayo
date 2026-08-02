package com.mslabs.wayo.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "parking_spots")
data class ParkingSpot(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val photoPath: String? = null,
    val note: String? = null,
    val isActive: Boolean = true
)
