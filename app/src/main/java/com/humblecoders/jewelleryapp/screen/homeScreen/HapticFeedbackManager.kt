package com.humblecoders.jewelleryapp.screen.homeScreen

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresPermission

// ADD this HapticFeedbackManager class before the HomeScreen composable:
class HapticFeedbackManager(private val context: Context) {
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    fun performHapticFeedback(type: HapticType) {
        if (!vibrator.hasVibrator()) return

        when (type) {
            HapticType.ELEGANT_CLICK -> {
                // Professional, refined single vibration - subtle and responsive
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Use a refined amplitude (120 = ~53% of max 225) for elegance - noticeable but not harsh
                    val amplitude = 120.coerceIn(1, 225)
                    // 18ms duration - short enough to feel responsive, long enough to be noticeable
                    vibrator.vibrate(VibrationEffect.createOneShot(18, amplitude))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(18)
                }
            }
            HapticType.LIGHT_CLICK -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
            HapticType.MEDIUM_CLICK -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(100)
                }
            }
            HapticType.SUCCESS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 100), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 100, 50, 100), -1)
                }
            }
            HapticType.ERROR -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200, 100, 200), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 200, 100, 200, 100, 200), -1)
                }
            }
        }
    }
}

enum class HapticType {
    ELEGANT_CLICK,  // Professional, refined single vibration
    LIGHT_CLICK,
    MEDIUM_CLICK,
    SUCCESS,
    ERROR
}

