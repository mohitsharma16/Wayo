package com.mslabs.wayo.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object VibrationUtils {

    /**
     * A short double-pulse, distinct from the single-click haptic used on
     * ordinary button taps elsewhere -- reserved for the one moment in the
     * app that's worth calling out specially: actually arriving back at
     * the marked spot.
     */
    fun vibrateArrivalSuccess(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return

        if (!vibrator.hasVibrator()) return

        val pattern = longArrayOf(0, 35, 55, 35)
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }
}
