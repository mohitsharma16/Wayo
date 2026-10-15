package com.mslabs.wayo.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Locale

object MapsLauncher {

    private const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"

    /**
     * Opens turn-by-turn walking directions from the user's current location
     * to the given point. Neither intent takes an explicit origin: map apps
     * start from the device's live location by default, which is exactly the
     * "start from where I am now" behavior wanted here.
     *
     * Prefers Google Maps' navigation intent; falls back to a generic geo:
     * intent so any other installed maps app can handle it. Returns false if
     * no app could be launched.
     */
    fun openWalkingDirections(context: Context, latitude: Double, longitude: Double): Boolean {
        val destination = String.format(Locale.US, "%.6f,%.6f", latitude, longitude)

        val googleMaps = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$destination&mode=w"))
            .setPackage(GOOGLE_MAPS_PACKAGE)
        if (tryStart(context, googleMaps)) return true

        val label = Uri.encode("Parked spot")
        val generic = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$destination?q=$destination($label)"))
        return tryStart(context, generic)
    }

    private fun tryStart(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
