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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.sqrt

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
    // Android's own accuracy radius in meters for the CURRENT live GPS fix.
    val gpsAccuracyMeters: Float = 0f,
    // The actual threshold used to decide "arrived" -- combines live
    // accuracy AND the accuracy the spot was originally captured with (see
    // arrivalRadius calculation below). Shown in the UI so the displayed
    // number always matches what the app is actually using, instead of
    // only showing live accuracy and using a different number internally.
    val arrivalRadiusMeters: Float = NavigationState.ARRIVAL_FLOOR_METERS,
    val isArrived: Boolean = false
) {
    val isGpsWeak: Boolean get() = gpsAccuracyMeters > GPS_WEAK_THRESHOLD_METERS

    companion object {
        // A reasonable default for pedestrian use -- tune if real-world
        // testing suggests otherwise.
        const val GPS_WEAK_THRESHOLD_METERS = 30f

        // Never require tighter precision than this to declare arrival,
        // even on an excellent GPS day -- consumer phone GPS realistically
        // can't do meaningfully better than this outdoors.
        const val ARRIVAL_FLOOR_METERS = 8f
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

    /**
     * Only subscribes to GPS/compass updates while there's actually an
     * active spot to navigate to. Previously this combined the location
     * flow unconditionally, so GPS kept running the entire time the app
     * was open -- including while just sitting on the capture screen with
     * nothing to navigate to. flatMapLatest on activeSpot means the
     * location subscription starts only when a spot is marked and stops
     * immediately (not after a delay) when it's cleared.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val navigationState: StateFlow<NavigationState> =
        activeSpot.flatMapLatest { spot ->
            if (spot == null) {
                flowOf(NavigationState())
            } else {
                // Scoped to this specific "navigating to a spot" session --
                // resets naturally every time a new spot is marked, so
                // smoothing from a previous session never leaks into a new one.
                var smoothedDistance: Float? = null
                var lastGpsHeading = 0f

                combine(locationFlow, sensorHeading) { location, sensorHead ->
                    val bearing = BearingUtils.calculateBearing(
                        location.latitude, location.longitude, spot.latitude, spot.longitude
                    )
                    val rawDistance = BearingUtils.distanceMeters(
                        location.latitude, location.longitude, spot.latitude, spot.longitude
                    )

                    // Low-pass filter on distance, same idea as the existing
                    // compass heading smoothing -- GPS position noise alone
                    // makes raw distance visibly jitter even standing still;
                    // this settles it into a steadier number instead of a
                    // number that jumps around every 1-2 seconds.
                    smoothedDistance = smoothedDistance?.let { it + 0.3f * (rawDistance - it) }
                        ?: rawDistance
                    val distance = smoothedDistance ?: rawDistance

                    val usingFallback = !compassSensor.hasOrientationSensor
                    val heading = if (usingFallback) {
                        location.bearing?.also { lastGpsHeading = it } ?: lastGpsHeading
                    } else {
                        sensorHead.degrees
                    }

                    // Combine BOTH sources of uncertainty via quadrature (the
                    // statistically correct way to combine two independent
                    // error estimates), not just the live reading. A spot
                    // captured with poor accuracy stays "hard to fully
                    // arrive at" even once live GPS is excellent -- which is
                    // honest, since the anchor point itself carries that
                    // uncertainty forever.
                    val combinedUncertainty = sqrt(
                        location.accuracyMeters * location.accuracyMeters +
                                spot.captureAccuracyMeters * spot.captureAccuracyMeters
                    )
                    val arrivalRadius = maxOf(combinedUncertainty, NavigationState.ARRIVAL_FLOOR_METERS)

                    NavigationState(
                        bearing = bearing,
                        heading = heading,
                        distanceMeters = distance,
                        usingGpsHeadingFallback = usingFallback,
                        compassNeedsCalibration = !usingFallback && !sensorHead.isReliable,
                        gpsAccuracyMeters = location.accuracyMeters,
                        arrivalRadiusMeters = arrivalRadius,
                        isArrived = distance <= arrivalRadius
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NavigationState())

    init {
        billingManager.startConnection()
    }

    private val _isMarkingSpot = MutableStateFlow(false)
    val isMarkingSpot: StateFlow<Boolean> = _isMarkingSpot

    fun parkHere(photoPath: String?, note: String?, onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}) {
        viewModelScope.launch {
            _isMarkingSpot.value = true
            // captureAccurateLocation() actively waits for a good fix
            // instead of grabbing whatever's fastest -- see LocationHelper
            // for why that mattered here specifically.
            val location = locationHelper.captureAccurateLocation()
            _isMarkingSpot.value = false

            if (location == null) {
                onFailure()
                return@launch
            }
            repository.parkHere(
                latitude = location.latitude,
                longitude = location.longitude,
                captureAccuracyMeters = location.accuracyMeters,
                photoPath = photoPath,
                note = note
            )
            onSuccess()
        }
    }

    fun foundCar() {
        viewModelScope.launch {
            repository.foundCar(keepInHistory = isPro.value)
        }
    }
}