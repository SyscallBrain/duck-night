package pt.tiago.ducknight.core

import org.junit.Assert.assertEquals
import org.junit.Test
import pt.tiago.ducknight.core.fixed.TICK_HZ
import pt.tiago.ducknight.core.fixed.VEGETATION_Y
import pt.tiago.ducknight.core.fixed.WORLD_HEIGHT
import pt.tiago.ducknight.core.fixed.WORLD_WIDTH

class VersionTest {
    @Test
    fun `version constants follow the plan`() {
        // M1 froze the simulation at 1; M3 freezes the photographer at 1.
        assertEquals(1, SIM_VERSION)
        assertEquals(2, BOT_VERSION)
    }

    @Test
    fun `the world and the tick rate are the documented ones`() {
        assertEquals(60, TICK_HZ)
        assertEquals(240, WORLD_WIDTH)
        assertEquals(300, WORLD_HEIGHT)
        assertEquals(232, VEGETATION_Y)
    }
}
