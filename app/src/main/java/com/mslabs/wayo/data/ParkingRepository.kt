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
     * Called when the user finds their car.
     * Pro users keep it in history; free users have it deleted outright.
     */
    suspend fun foundCar(keepInHistory: Boolean) {
        if (keepInHistory) {
            dao.deactivateAllActive()
        } else {
            dao.deleteActiveSpots()
        }
    }
}