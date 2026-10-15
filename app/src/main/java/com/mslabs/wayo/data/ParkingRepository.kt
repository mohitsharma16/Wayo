package com.mslabs.wayo.data

import kotlinx.coroutines.flow.Flow

class ParkingRepository(private val dao: ParkingSpotDao) {

    val activeSpot: Flow<ParkingSpot?> = dao.getActiveSpot()
    val history: Flow<List<ParkingSpot>> = dao.getHistory()

    suspend fun parkHere(
        latitude: Double,
        longitude: Double,
        captureAccuracyMeters: Float,
        photoPath: String?,
        note: String?
    ) {
        // Only one active spot at a time -- clear any previous one first.
        dao.deactivateAllActive()
        dao.insert(
            ParkingSpot(
                latitude = latitude,
                longitude = longitude,
                timestamp = System.currentTimeMillis(),
                captureAccuracyMeters = captureAccuracyMeters,
                photoPath = photoPath,
                note = note,
                isActive = true
            )
        )
    }

    /**
     * Called when the user finds their car. Every spot is kept in history;
     * how many of them are visible is decided by the view model (free users
     * see only the most recent few until they unlock full access).
     */
    suspend fun foundCar() {
        dao.deactivateAllActive()
    }

    suspend fun deleteFromHistory(spot: ParkingSpot) {
        dao.delete(spot)
    }
}