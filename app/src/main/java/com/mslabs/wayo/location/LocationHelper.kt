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
    fun locationUpdates(): Flow<Pair<Double, Double>> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    Log.d("LocationHelper", "location update: ${it.latitude}, ${it.longitude}")
                    trySend(it.latitude to it.longitude)
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

