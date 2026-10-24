package pt.tiago.ducknight.core.fixed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FixedPointTest {
    @Test
    fun `one unit is 256 fixed-point steps`() {
        assertEquals(256, fx(1))
        assertEquals(0, fx(0))
        assertEquals(-512, fx(-2))
    }

    @Test
    fun `units round to the nearest whole unit`() {
        assertEquals(0, toUnits(127))
        assertEquals(1, toUnits(128))
        assertEquals(1, toUnits(255))
        assertEquals(1, toUnits(256))
        assertEquals(1, toUnits(257))
        assertEquals(2, toUnits(384))
        assertEquals(-1, toUnits(-128))
        assertEquals(-2, toUnits(-384))
    }

    @Test
    fun `multiplication keeps the fixed-point scale`() {
        assertEquals(256, fxMul(256, 256))
        assertEquals(128, fxMul(128, 256))
        assertEquals(64, fxMul(128, 128))
        assertEquals(-64, fxMul(-128, 128))
        assertEquals(1, fxMul(1, 128))
        assertEquals(-1, fxMul(-1, 128))
    }

    @Test
    fun `multiplication does not overflow at the worst speed`() {
        // S5 with a 1.25 ramp: 112 u/s * 1.2 * 1.25 = 168 u/s -> 717 fx/tick.
        val speed = 717
        val sine = TrigTable.sin(2048 - 512)
        assertEquals(717, fxMul(speed, 256))
        assertTrue(fxMul(speed, sine) in -speed..speed)
    }

    @Test
    fun `division keeps the fixed-point scale`() {
        assertEquals(256, fxDiv(256, 1))
        assertEquals(128, fxDiv(256, 2))
        assertEquals(85, fxDiv(256, 3))
        assertEquals(-85, fxDiv(-256, 3))
        assertEquals(0, fxDiv(0, 7))
    }

    @Test
    fun `division rejects a zero denominator`() {
        assertTrue(runCatching { fxDiv(10, 0) }.isFailure)
    }

    @Test
    fun `square root is exact for perfect squares`() {
        for (n in listOf(0L, 1L, 4L, 9L, 16L, 10_000L, 4_294_967_296L)) {
            val root = isqrt(n)
            assertEquals(n, root * root)
        }
    }

    @Test
    fun `square root floors between squares`() {
        assertEquals(1L, isqrt(2))
        assertEquals(1L, isqrt(3))
        assertEquals(2L, isqrt(8))
        assertEquals(99L, isqrt(9_999))
        assertEquals(100L, isqrt(10_000))
    }

    @Test
    fun `square root rejects negative values`() {
        assertTrue(runCatching { isqrt(-1) }.isFailure)
    }

    @Test
    fun `square root handles the longest possible world distance`() {
        val dx = fx(240)
        val dy = fx(300)
        val distance = isqrt(dx.toLong() * dx + dy.toLong() * dy)
        assertEquals(98351L, distance)
    }

    @Test
    fun `distance is symmetric and zero at the same point`() {
        assertEquals(0, distance(100, -100, 100, -100))
        assertEquals(256, distance(0, 0, 256, 0))
        assertEquals(distance(10, 20, 300, 400), distance(300, 400, 10, 20))
    }

    @Test
    fun `speed converts from units per second to fixed point per tick`() {
        assertEquals(205, fpsToFxPerTick(48))
        assertEquals(265, fpsToFxPerTick(62))
        assertEquals(333, fpsToFxPerTick(78))
        assertEquals(405, fpsToFxPerTick(95))
        assertEquals(478, fpsToFxPerTick(112))
        assertEquals(444, fpsToFxPerTick(104))
    }

    @Test
    fun `clamp keeps values inside the range`() {
        assertEquals(5, clamp(5, 0, 10))
        assertEquals(0, clamp(-1, 0, 10))
        assertEquals(10, clamp(11, 0, 10))
        assertEquals(-5, clamp(-5, -10, 0))
    }

    @Test
    fun `degrees convert to the 4096 step circle`() {
        assertEquals(0, degreesToAngle(0))
        assertEquals(1024, degreesToAngle(90))
        assertEquals(2048, degreesToAngle(180))
        assertEquals(3072, degreesToAngle(270))
        assertEquals(0, degreesToAngle(360))
        assertEquals(1024, degreesToAngle(-270))
    }
}
