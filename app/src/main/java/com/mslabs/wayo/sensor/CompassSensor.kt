package com.mslabs.wayo.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class CompassSensor(private val context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    /**
     * Emits smoothed compass heading in degrees (0-360, 0 = north),
     * corrected for the current device/display rotation.
     */
    fun headingFlow(): Flow<Float> = callbackFlow {
        var smoothedHeading = 0f
        var initialized = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                val (worldAxisX, worldAxisZ) = axesForDisplayRotation()
                val adjustedMatrix = FloatArray(9)
                SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisX, worldAxisZ, adjustedMatrix)

                val orientation = FloatArray(3)
                SensorManager.getOrientation(adjustedMatrix, orientation)

                var azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                if (azimuth < 0) azimuth += 360f

                smoothedHeading = if (!initialized) {
                    initialized = true
                    azimuth
                } else {
                    lowPassAngle(azimuth, smoothedHeading)
                }

                trySend(smoothedHeading)
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }

        rotationSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose { sensorManager.unregisterListener(listener) }
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

    /** Low-pass filter that correctly handles the 0/360 degree wraparound. */
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
