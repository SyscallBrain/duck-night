package pt.tiago.ducknight.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsTest {
    @Test
    fun `defaults are the documented M6 values`() {
        val settings = Settings()
        assertEquals(AimMode.TAP, settings.aimMode)
        assertEquals(100, settings.tolerancePercent)
        assertEquals(100, settings.gameSpeedPercent)
        assertEquals(100, settings.gyroGainPercent)
        assertEquals(false, settings.leftHanded)
        assertEquals(false, settings.highContrast)
        assertEquals(false, settings.largeText)
        assertEquals(false, settings.reducedMotion)
        assertEquals(true, settings.followSystemMotion)
        assertEquals("NORMAL", settings.palette)
    }

    @Test
    fun `values are clamped to their allowed ranges`() {
        val settings = Settings(gyroGainPercent = 999, tolerancePercent = 1, gameSpeedPercent = 200, sfxVolume = 4f)
        val clean = settings.sanitized()
        assertEquals(160, clean.gyroGainPercent)
        assertEquals(85, clean.tolerancePercent)
        assertEquals(100, clean.gameSpeedPercent)
        assertEquals(1f, clean.sfxVolume, 0.0001f)
        assertEquals("NORMAL", Settings(palette = "WRONG").sanitized().palette)
        assertEquals("TRITANOPIA", Settings(palette = "TRITANOPIA").sanitized().palette)
    }

    @Test
    fun `a broken sound value falls back to the default`() {
        assertEquals(0.8f, Settings(sfxVolume = Float.NaN).sanitized().sfxVolume, 0.0001f)
    }

    @Test
    fun `all three aiming modes are available`() {
        assertEquals(3, AimMode.entries.size)
        assertEquals(setOf(AimMode.TAP, AimMode.GYRO, AimMode.AIM_TRIGGER), AimMode.entries.toSet())
    }
}
