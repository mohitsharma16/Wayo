package com.mslabs.wayo.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * A location fix plus GPS-derived course-over-ground bearing, when available.
 * `bearing` is only non-null when Android itself is confident in the
 * direction of travel (typically requires actual movement) -- see
 * Location.hasBearing() in the Android docs for exactly when that's true.
 *
 * `accuracyMeters` is Android's own 68%-confidence accuracy radius for this
 * fix -- a large value means a weak GPS signal, which no amount of phone
 * movement fixes (unlike compass calibration); the only real remedy is a
 * clearer view of the sky.
 */
data class LocationUpdate(
    val latitude: Double,
    val longitude: Double,
    val bearing: Float?,
    val accuracyMeters: Float
)

class LocationHelper(context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    /**
     * Fast last-known-location fetch -- prioritizes speed over precision,
     * since capturing a parking spot quickly matters more than perfect accuracy.
     */
    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): Pair<Double, Double>? {
        val location = fusedClient.lastLocation.await() ?: return null
        return location.latitude to location.longitude
    }

    /** Continuous location updates for the live compass/distance screen. */
    @SuppressLint("MissingPermission")
    fun locationUpdates(): Flow<LocationUpdate> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    // hasBearing() is Android's own confidence check -- it's only
                    // true when the platform trusts the direction-of-travel
                    // reading, which in practice means you're actually moving.
                    val bearing = if (it.hasBearing()) it.bearing else null
                    Log.d(
                        "LocationHelper",
                        "location update: ${it.latitude}, ${it.longitude}, bearing=$bearing, accuracy=${it.accuracy}m"
                    )
                    trySend(LocationUpdate(it.latitude, it.longitude, bearing, it.accuracy))
                }
            }
        }

        // Explicitly using the main looper here rather than passing null.
        // Passing null asks the platform to infer the calling thread's
        // Looper, which is usually fine but has been inconsistent on some
        // OEM ROMs (Motorola in particular has a history of quirky location
        // update delivery). Being explicit removes that ambiguity.
        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { fusedClient.removeLocationUpdates(callback) }
    }
}
