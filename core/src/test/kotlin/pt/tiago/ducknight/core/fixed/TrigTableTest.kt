package pt.tiago.ducknight.core.fixed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrigTableTest {
    @Test
    fun `the table has one entry per angle step`() {
        assertEquals(4096, TrigTable.STEPS)
    }

    @Test
    fun `sine has the four cardinal values`() {
        assertEquals(0, TrigTable.sin(0))
        assertEquals(256, TrigTable.sin(1024))
        assertEquals(0, TrigTable.sin(2048))
        assertEquals(-256, TrigTable.sin(3072))
    }

    @Test
    fun `cosine is sine a quarter turn ahead`() {
        for (angle in 0 until TrigTable.STEPS step 37) {
            assertEquals(TrigTable.sin(angle + 1024), TrigTable.cos(angle))
        }
    }

    @Test
    fun `every sine stays inside the fixed point unit`() {
        for (angle in 0 until TrigTable.STEPS) {
            val value = TrigTable.sin(angle)
            assertTrue("sin($angle) = $value", value in -256..256)
        }
    }

    @Test
    fun `sine is odd and has a half-turn symmetry`() {
        for (angle in 1 until TrigTable.STEPS step 53) {
            assertEquals(-TrigTable.sin(angle), TrigTable.sin(-angle))
            assertEquals(-TrigTable.sin(angle), TrigTable.sin(angle + 2048))
        }
    }

    @Test
    fun `angles wrap around the circle`() {
        assertEquals(TrigTable.sin(10), TrigTable.sin(10 + 4096))
        assertEquals(TrigTable.sin(10), TrigTable.sin(10 - 4096))
        assertEquals(TrigTable.sin(-1), TrigTable.sin(4095))
    }

    @Test
    fun `the table hash is stable`() {
        // Locked after the first run: any change to the table invalidates this value.
        assertEquals(3714804484989037062L, TrigTable.tableHash())
    }
}
