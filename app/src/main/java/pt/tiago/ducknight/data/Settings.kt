package pt.tiago.ducknight.data

import kotlinx.serialization.Serializable

/** How the player points the camera. */
@Serializable
enum class AimMode { TAP, GYRO, AIM_TRIGGER }

/** The player's settings: aiming, help, accessibility, sound and theme. */
@Serializable
data class Settings(
    val aimMode: AimMode = AimMode.TAP,
    val gyroGainPercent: Int = 100,
    val tolerancePercent: Int = 100,
    val aimAssist: Boolean = false,
    val gameSpeedPercent: Int = 100,
    val biggerBirds: Boolean = false,
    val leftHanded: Boolean = false,
    val advanceLongPress: Boolean = false,
    val dogLaugh: Boolean = true,
    val vibration: Boolean = true,
    val sfxVolume: Float = 0.8f,
    val musicVolume: Float = 0.4f,
    val theme: String = "LAGOON",
    val highContrast: Boolean = false,
    val largeText: Boolean = false,
    val reducedMotion: Boolean = false,
    val followSystemMotion: Boolean = true,
    /** NORMAL, DEUTERANOPIA, PROTANOPIA or TRITANOPIA. */
    val palette: String = "NORMAL",
)

fun Settings.sanitized(): Settings = copy(
    gyroGainPercent = gyroGainPercent.coerceIn(60, 160),
    tolerancePercent = tolerancePercent.coerceIn(85, 125),
    gameSpeedPercent = gameSpeedPercent.coerceIn(70, 100),
    sfxVolume = if (sfxVolume.isFinite()) sfxVolume.coerceIn(0f, 1f) else 0.8f,
    musicVolume = if (musicVolume.isFinite()) musicVolume.coerceIn(0f, 1f) else 0.4f,
    theme = theme.takeIf { it in setOf("CASSETTE", "LAGOON", "PAPER", "FAIR") } ?: "LAGOON",
    palette = palette.takeIf {
        it in setOf("NORMAL", "DEUTERANOPIA", "PROTANOPIA", "TRITANOPIA")
    } ?: "NORMAL",
)
