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
    val distanceMeters: Float = 0f
)

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

    private val currentHeading = compassSensor.headingFlow()
    private val currentLocation = locationHelper.locationUpdates()

    val navigationState: StateFlow<NavigationState> =
        combine(activeSpot, currentLocation, currentHeading) { spot, location, heading ->
            if (spot == null) return@combine NavigationState(heading = heading)
            val (lat, lng) = location
            val bearing = BearingUtils.calculateBearing(lat, lng, spot.latitude, spot.longitude)
            val distance = BearingUtils.distanceMeters(lat, lng, spot.latitude, spot.longitude)
            NavigationState(bearing = bearing, heading = heading, distanceMeters = distance)
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
