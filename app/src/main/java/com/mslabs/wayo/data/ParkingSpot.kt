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
    // How accurate the GPS fix was at the moment this spot was captured.
    // Needed because "am I there yet" has to account for uncertainty in
    // BOTH the original capture and the live reading, not just the live
    // one -- a poorly captured anchor point makes every later reading look
    // "off" even if live GPS is working perfectly.
    val captureAccuracyMeters: Float = 0f,
    val photoPath: String? = null,
    val note: String? = null,
    val isActive: Boolean = true
)