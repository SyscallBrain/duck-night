package pt.tiago.ducknight.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Haptic feedback. Never used for menu navigation. */
class Haptics(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private fun oneShot(milliseconds: Long, amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE) {
        val device = vibrator ?: return
        if (!device.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            device.vibrate(VibrationEffect.createOneShot(milliseconds, amplitude))
        } else {
            @Suppress("DEPRECATION")
            device.vibrate(milliseconds)
        }
    }

    private fun pattern(timings: LongArray, amplitudes: IntArray) {
        val device = vibrator ?: return
        if (!device.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            device.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            device.vibrate(timings, -1)
        }
    }

    fun shutter() = oneShot(12)
    fun goodPhoto() = oneShot(25)
    fun perfectPhoto() = pattern(longArrayOf(0, 20, 40, 20), intArrayOf(0, 160, 0, 220))
    fun advance() = pattern(longArrayOf(0, 6, 40, 6, 40, 6), intArrayOf(0, 90, 0, 90, 0, 90))
    fun dogPage() = pattern(longArrayOf(0, 40, 30, 60, 30, 80), intArrayOf(0, 140, 0, 180, 0, 220))
    fun clayBroken() = oneShot(18)
    fun star() = oneShot(18)
}
