package pt.tiago.ducknight.leveltool.content

import org.junit.Assert.assertEquals
import org.junit.Test
import pt.tiago.ducknight.core.level.Difficulty
import pt.tiago.ducknight.core.level.LevelType

class CalibratorBandsTest {
    @Test
    fun `each kind of level has its own target band`() {
        assertEquals(0.72..0.88, Calibrator.bandFor(Difficulty.MEDIUM, 3, 20, LevelType.CLASSIC))
        assertEquals(0.95..1.0, Calibrator.bandFor(Difficulty.MEDIUM, 1, 5, LevelType.CLASSIC))
        assertEquals(0.72..0.88, Calibrator.bandFor(Difficulty.MEDIUM, 2, 5, LevelType.CLASSIC))
        assertEquals(0.62..0.80, Calibrator.bandFor(Difficulty.MEDIUM, 4, 47, LevelType.DOUBLE))
        assertEquals(0.30..0.55, Calibrator.bandFor(Difficulty.HARD, 7, 50, LevelType.BOSS))
    }
}
