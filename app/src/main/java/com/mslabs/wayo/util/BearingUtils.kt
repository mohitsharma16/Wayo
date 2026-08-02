package com.mslabs.wayo.util

import android.location.Location
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object BearingUtils {

    fun calculateBearing(
        currentLat: Double,
        currentLng: Double,
        targetLat: Double,
        targetLng: Double
    ): Float {
        val lat1 = Math.toRadians(currentLat)
        val lat2 = Math.toRadians(targetLat)
        val dLng = Math.toRadians(targetLng - currentLng)

        val y = sin(dLng) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
        val bearing = Math.toDegrees(atan2(y, x))

        return ((bearing + 360) % 360).toFloat()
    }

    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0]
    }
}
