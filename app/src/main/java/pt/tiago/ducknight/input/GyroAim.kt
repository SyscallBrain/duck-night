package pt.tiago.ducknight.input

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pt.tiago.ducknight.core.fixed.WORLD_HEIGHT
import pt.tiago.ducknight.core.fixed.WORLD_WIDTH
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Maps the phone rotation to a point in the 240 x 300 world. `TYPE_GAME_ROTATION_VECTOR` needs no
 * magnetometer and no permission; if it is missing, `TYPE_ROTATION_VECTOR` is used. A light
 * one-euro-style low-pass removes tremor without adding a noticeable delay.
 */
class GyroAim(context: Context, private var gain: Float = 1f) : SensorEventListener {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? =
        manager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val rotation = FloatArray(9)
    private val orientation = FloatArray(3)
    private var referenceYaw = 0f
    private var referencePitch = 0f
    private var hasReference = false
    private var filteredYaw = 0f
    private var filteredPitch = 0f
    private var lastYaw = 0f
    private var lastPitch = 0f

    var aimX by mutableStateOf(WORLD_WIDTH / 2f)
        private set
    var aimY by mutableStateOf(WORLD_HEIGHT / 2f)
        private set
    var driftDegrees by mutableStateOf(0f)
        private set
    var available: Boolean = sensor != null
        private set

    val maxAngleRadians: Float = Math.toRadians(30.0).toFloat()

    fun setGain(value: Float) {
        gain = value.coerceIn(0.6f, 1.6f)
    }

    fun start() {
        sensor ?: return
        manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        manager.unregisterListener(this)
    }

    fun recenter() {
        // The next sensor event becomes the new reference; this also works before the first event.
        hasReference = false
        filteredYaw = 0f
        filteredPitch = 0f
        aimX = WORLD_WIDTH / 2f
        aimY = WORLD_HEIGHT / 2f
        driftDegrees = 0f
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_GAME_ROTATION_VECTOR &&
            event.sensor.type != Sensor.TYPE_ROTATION_VECTOR
        ) {
            return
        }
        SensorManager.getRotationMatrixFromVector(rotation, event.values)
        SensorManager.getOrientation(rotation, orientation)
        val yaw = orientation[0]
        val pitch = orientation[1]
        lastYaw = yaw
        lastPitch = pitch
        if (!hasReference) {
            referenceYaw = yaw
            referencePitch = pitch
            hasReference = true
        }
        val rawYaw = normalize(yaw - referenceYaw)
        val rawPitch = pitch - referencePitch
        // Low-pass with a faster response when the hand actually moves.
        val yawAlpha = if (abs(rawYaw - filteredYaw) > 0.02f) 0.45f else 0.18f
        val pitchAlpha = if (abs(rawPitch - filteredPitch) > 0.02f) 0.45f else 0.18f
        filteredYaw += (rawYaw - filteredYaw) * yawAlpha
        filteredPitch += (rawPitch - filteredPitch) * pitchAlpha
        val scale = WORLD_WIDTH / (2f * maxAngleRadians)
        aimX = (WORLD_WIDTH / 2f - filteredYaw * scale * gain)
            .coerceIn(4f, WORLD_WIDTH - 4f)
        aimY = (WORLD_HEIGHT / 2f - filteredPitch * scale * gain)
            .coerceIn(4f, WORLD_HEIGHT - 4f)
        driftDegrees = (abs(filteredYaw) + abs(filteredPitch)) * (180f / PI.toFloat())
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun normalize(angle: Float): Float {
        var value = angle
        while (value > PI) value -= (2 * PI).toFloat()
        while (value < -PI) value += (2 * PI).toFloat()
        return value
    }

    companion object {
        fun available(context: Context): Boolean {
            val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            return manager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) != null ||
                manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
        }
    }
}
