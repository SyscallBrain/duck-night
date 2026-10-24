package pt.tiago.ducknight.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldMappingTest {
    @Test
    fun `the center maps to the middle of the world`() {
        assertEquals(120, WorldMapping.worldX(195f, 390f))
        assertEquals(150, WorldMapping.worldY(300f, 600f))
    }

    @Test
    fun `the corners map to the world corners`() {
        assertEquals(0, WorldMapping.worldX(0f, 240f))
        assertEquals(240, WorldMapping.worldX(240f, 240f))
        assertEquals(0, WorldMapping.worldY(0f, 300f))
        assertEquals(300, WorldMapping.worldY(300f, 300f))
    }

    @Test
    fun `the mapping is clamped`() {
        assertEquals(0, WorldMapping.worldX(-10f, 240f))
        assertEquals(240, WorldMapping.worldX(999f, 240f))
        assertEquals(300, WorldMapping.worldY(999f, 300f))
    }

    @Test
    fun `a zero size is rejected`() {
        assertTrue(runCatching { WorldMapping.worldX(0f, 0f) }.isFailure)
        assertTrue(runCatching { WorldMapping.worldY(0f, 0f) }.isFailure)
    }
}
