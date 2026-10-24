package pt.tiago.ducknight.core.random

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitMix64Test {
    @Test
    fun `same seed produces the same stream`() {
        val a = SplitMix64(0x1234abcdL)
        val b = SplitMix64(0x1234abcdL)
        repeat(100) { assertEquals(a.nextLong(), b.nextLong()) }
    }

    @Test
    fun `different seeds produce different streams`() {
        val a = SplitMix64(1L)
        val b = SplitMix64(2L)
        assertNotEquals(a.nextLong(), b.nextLong())
    }

    @Test
    fun `nextInt stays inside the bound`() {
        val rng = SplitMix64(42L)
        repeat(1000) {
            val v = rng.nextInt(7)
            assertTrue(v in 0..6)
        }
    }

    @Test
    fun `nextInt covers every value`() {
        val rng = SplitMix64(7L)
        val seen = BooleanArray(5)
        repeat(500) { seen[rng.nextInt(5)] = true }
        assertTrue(seen.all { it })
    }

    @Test
    fun `nextInt rejects non-positive bounds`() {
        val rng = SplitMix64(1L)
        for (bound in listOf(0, -1, Int.MIN_VALUE)) {
            val failed = runCatching { rng.nextInt(bound) }.isFailure
            assertTrue("bound $bound must fail", failed)
        }
    }

    @Test
    fun `split streams are independent and stable`() {
        assertEquals(SplitMix64.split(123L, 1L), SplitMix64.split(123L, 1L))
        assertNotEquals(SplitMix64.split(123L, 1L), SplitMix64.split(123L, 2L))
        // Golden values lock the derivation so certificates stay valid across refactors.
        assertEquals(0x564c34f8699eb77bL, SplitMix64.split(123L, 1L))
    }

    @Test
    fun `fromHex round trips through stateHex`() {
        val rng = SplitMix64.fromHex("deadbeefcafe1234")
        assertEquals("deadbeefcafe1234", rng.stateHex())
    }

    @Test
    fun `seedFromHex accepts one to sixteen digits`() {
        assertEquals(0xaL, SplitMix64.seedFromHex("a"))
        assertEquals(-1L, SplitMix64.seedFromHex("ffffffffffffffff"))
        assertTrue(runCatching { SplitMix64.seedFromHex("0123456789abcdef0") }.isFailure)
    }
}
