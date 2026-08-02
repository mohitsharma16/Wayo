package com.mslabs.wayo.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class CompassSensor(private val context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    /**
     * False on devices with no usable orientation sensor at all -- confirmed
     * to genuinely happen despite manufacturer spec sheets claiming
     * otherwise (some Motorola models are a documented example). When this
     * is false, MainViewModel falls back to GPS course-over-ground instead
     * of a magnetometer-based heading.
     */
    val hasOrientationSensor: Boolean =
        rotationSensor != null || (accelerometer != null && magnetometer != null)

    /**
     * Emits smoothed compass heading in degrees (0-360, 0 = north).
     *
     * Root-cause fix: this used to only register TYPE_ROTATION_VECTOR and,
     * if that sensor was unavailable or never fired, would emit NOTHING --
     * ever. Since MainViewModel combines this flow with location updates
     * using combine(), which requires every source to emit at least once
     * before producing any result, a silent heading flow silently froze the
     * entire navigation screen at its default (0m, no direction) even
     * though location updates kept arriving perfectly. Two fixes:
     *   1. Always send a safe 0f immediately so combine() can never get
     *      permanently stuck waiting on this flow specifically.
     *   2. Fall back to classic accelerometer+magnetometer orientation if
     *      the fused rotation-vector sensor isn't available on this device.
     */
    /**
     * A heading reading plus whether Android itself trusts it.
     * isReliable comes straight from the sensor's own onAccuracyChanged
     * callback -- when a magnetometer-based reading is flagged UNRELIABLE
     * or LOW, that's Android's standard signal that the classic figure-8
     * calibration gesture would actually help (this is the same mechanism
     * apps like Google Maps use for their "calibrate compass" prompt).
     */
    data class HeadingReading(val degrees: Float, val isReliable: Boolean)

    fun headingFlow(): Flow<HeadingReading> = callbackFlow {
        var smoothedHeading = 0f
        var initialized = false
        var isReliable = true

        trySend(HeadingReading(0f, true))

        fun emitHeading(azimuthDegrees: Float) {
            var azimuth = azimuthDegrees
            if (azimuth < 0) azimuth += 360f
            smoothedHeading = if (!initialized) {
                initialized = true
                azimuth
            } else {
                lowPassAngle(azimuth, smoothedHeading)
            }
            trySend(HeadingReading(smoothedHeading, isReliable))
        }

        when {
            rotationSensor != null -> {
                Log.d("CompassSensor", "using TYPE_ROTATION_VECTOR")
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        val rotationMatrix = FloatArray(9)
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                        val (worldAxisX, worldAxisZ) = axesForDisplayRotation()
                        val adjustedMatrix = FloatArray(9)
                        SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisX, worldAxisZ, adjustedMatrix)

                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(adjustedMatrix, orientation)

                        emitHeading(Math.toDegrees(orientation[0].toDouble()).toFloat())
                    }

                    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                        isReliable = accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM
                    }
                }
                sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
                awaitClose { sensorManager.unregisterListener(listener) }
            }

            accelerometer != null && magnetometer != null -> {
                Log.d("CompassSensor", "TYPE_ROTATION_VECTOR unavailable, falling back to accelerometer+magnetometer")
                val gravity = FloatArray(3)
                val geomagnetic = FloatArray(3)
                var haveGravity = false
                var haveGeomagnetic = false

                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        when (event.sensor.type) {
                            Sensor.TYPE_ACCELEROMETER -> {
                                System.arraycopy(event.values, 0, gravity, 0, 3)
                                haveGravity = true
                            }
                            Sensor.TYPE_MAGNETIC_FIELD -> {
                                System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                                haveGeomagnetic = true
                            }
                        }
                        if (haveGravity && haveGeomagnetic) {
                            val rotationMatrix = FloatArray(9)
                            if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                                val (worldAxisX, worldAxisZ) = axesForDisplayRotation()
                                val adjustedMatrix = FloatArray(9)
                                SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisX, worldAxisZ, adjustedMatrix)

                                val orientation = FloatArray(3)
                                SensorManager.getOrientation(adjustedMatrix, orientation)

                                emitHeading(Math.toDegrees(orientation[0].toDouble()).toFloat())
                            }
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                        // The magnetometer's own accuracy is the meaningful
                        // one here -- it's what actually degrades under
                        // magnetic interference and what the figure-8
                        // gesture fixes.
                        if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                            isReliable = accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM
                        }
                    }
                }
                sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
                sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
                awaitClose { sensorManager.unregisterListener(listener) }
            }

            else -> {
                Log.d("CompassSensor", "no compass-capable sensor on this device -- heading stays fixed at 0")
                awaitClose { }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun axesForDisplayRotation(): Pair<Int, Int> {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val rotation = windowManager.defaultDisplay.rotation
        return when (rotation) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
    }

    private fun lowPassAngle(newAngle: Float, oldAngle: Float, alpha: Float = 0.15f): Float {
        var delta = newAngle - oldAngle
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        var result = oldAngle + alpha * delta
        if (result < 0f) result += 360f
        if (result >= 360f) result -= 360f
        return result
    }
}
