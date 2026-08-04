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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

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

    companion object {
        // Don't settle for a fix worse than this without at least trying
        // for something better first.
        private const val GOOD_ENOUGH_ACCURACY_METERS = 15f
        private const val CAPTURE_TIMEOUT_MILLIS = 6000L
    }

    /**
     * Used when marking a spot. This used to just grab whatever fix was
     * fastest (a single lastLocation cache read, or a single
     * getCurrentLocation call) -- but neither guarantees a GPS-quality fix.
     * Android can hand back a fast, rough network/cell-tower estimate
     * (20m+ error) instead of waiting for GPS to lock in, especially right
     * after the location subsystem wakes up. That silently poisons the
     * saved spot's coordinates: live tracking can be accurate afterward,
     * but every distance reading is then measured against a bad anchor
     * point, which looks exactly like "the distance is wrong" even though
     * live GPS is working fine.
     *
     * This instead watches a short burst of live updates (same stream the
     * compass screen uses) for up to [CAPTURE_TIMEOUT_MILLIS], and takes
     * the best (lowest accuracy value) one seen -- preferring one at or
     * under [GOOD_ENOUGH_ACCURACY_METERS] if it arrives in time.
     */
    @SuppressLint("MissingPermission")
    suspend fun captureAccurateLocation(): LocationUpdate? {
        var best: LocationUpdate? = null

        val goodFix = withTimeoutOrNull(CAPTURE_TIMEOUT_MILLIS.milliseconds) {
            locationUpdates().first { update ->
                if (best == null || update.accuracyMeters < best!!.accuracyMeters) {
                    best = update
                }
                update.accuracyMeters <= GOOD_ENOUGH_ACCURACY_METERS
            }
        }

        if (goodFix != null) return goodFix
        if (best != null) return best

        // Extremely rare fallback: no live update arrived at all within the
        // timeout window. Fall back to a cache read / one-shot request so
        // "Mark this spot" still doesn't silently do nothing.
        fusedClient.lastLocation.await()?.let {
            return LocationUpdate(it.latitude, it.longitude, if (it.hasBearing()) it.bearing else null, it.accuracy)
        }
        val fresh = fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
        return fresh?.let {
            LocationUpdate(it.latitude, it.longitude, if (it.hasBearing()) it.bearing else null, it.accuracy)
        }
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