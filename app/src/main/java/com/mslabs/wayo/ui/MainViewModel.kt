package com.mslabs.wayo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mslabs.wayo.billing.BillingManager
import com.mslabs.wayo.data.AppDatabase
import com.mslabs.wayo.data.ParkingRepository
import com.mslabs.wayo.data.ParkingSpot
import com.mslabs.wayo.location.LocationHelper
import com.mslabs.wayo.sensor.CompassSensor
import com.mslabs.wayo.util.BearingUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NavigationState(
    val bearing: Float = 0f,
    val heading: Float = 0f,
    val distanceMeters: Float = 0f,
    // True when this device has no usable orientation sensor and we're
    // relying on GPS course-over-ground instead.
    val usingGpsHeadingFallback: Boolean = false,
    // True when a real magnetometer exists but Android itself flags the
    // reading as unreliable -- this is when the figure-8 calibration
    // gesture actually helps.
    val compassNeedsCalibration: Boolean = false,
    // Android's own accuracy radius in meters for the current GPS fix.
    val gpsAccuracyMeters: Float = 0f
) {
    val isGpsWeak: Boolean get() = gpsAccuracyMeters > GPS_WEAK_THRESHOLD_METERS

    companion object {
        // A reasonable default for pedestrian use -- tune if real-world
        // testing suggests otherwise.
        const val GPS_WEAK_THRESHOLD_METERS = 30f
    }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ParkingRepository(
        AppDatabase.getInstance(application).parkingSpotDao()
    )
    private val locationHelper = LocationHelper(application)
    private val compassSensor = CompassSensor(application)
    val billingManager = BillingManager(application)

    val activeSpot: StateFlow<ParkingSpot?> = repository.activeSpot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val history: StateFlow<List<ParkingSpot>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isPro: StateFlow<Boolean> = billingManager.isPro

    private val sensorHeading = compassSensor.headingFlow()
    private val locationFlow = locationHelper.locationUpdates()

    // Retained across combine() emissions so the arrow doesn't reset to 0
    // every time GPS momentarily has no fresh bearing (e.g. briefly
    // standing still) on devices using the GPS-heading fallback.
    private var lastGpsHeading: Float = 0f

    val navigationState: StateFlow<NavigationState> =
        combine(activeSpot, locationFlow, sensorHeading) { spot, location, sensorHead ->
            if (spot == null) return@combine NavigationState()

            val bearing = BearingUtils.calculateBearing(
                location.latitude, location.longitude, spot.latitude, spot.longitude
            )
            val distance = BearingUtils.distanceMeters(
                location.latitude, location.longitude, spot.latitude, spot.longitude
            )

            val usingFallback = !compassSensor.hasOrientationSensor
            val heading = if (usingFallback) {
                location.bearing?.also { lastGpsHeading = it } ?: lastGpsHeading
            } else {
                sensorHead.degrees
            }

            NavigationState(
                bearing = bearing,
                heading = heading,
                distanceMeters = distance,
                usingGpsHeadingFallback = usingFallback,
                compassNeedsCalibration = !usingFallback && !sensorHead.isReliable,
                gpsAccuracyMeters = location.accuracyMeters
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NavigationState())

    init {
        billingManager.startConnection()
    }

    fun parkHere(photoPath: String?, note: String?) {
        viewModelScope.launch {
            val location = locationHelper.getLastKnownLocation() ?: return@launch
            repository.parkHere(location.first, location.second, photoPath, note)
        }
    }

    fun foundCar() {
        viewModelScope.launch {
            repository.foundCar(keepInHistory = isPro.value)
        }
    }
}
