package pt.tiago.ducknight.core.level

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelSpecTest {
    @Test
    fun `easy classic follows the difficulty table`() {
        val spec = LevelSpec.classic(seed = 1L, difficulty = Difficulty.EASY)
        assertEquals(Difficulty.EASY, spec.difficulty)
        assertEquals(10, spec.birds)
        assertEquals(3, spec.photosPerBird)
        assertEquals(5, spec.need)
        assertEquals(480, spec.timeoutTicks)
        assertEquals(80, spec.speedPercent)
        assertEquals(125, spec.frameScalePercent)
        assertEquals(LevelType.CLASSIC, spec.type)
    }

    @Test
    fun `medium classic follows the difficulty table`() {
        val spec = LevelSpec.classic(seed = 2L, difficulty = Difficulty.MEDIUM)
        assertEquals(6, spec.need)
        assertEquals(390, spec.timeoutTicks)
        assertEquals(100, spec.speedPercent)
        assertEquals(100, spec.frameScalePercent)
    }

    @Test
    fun `hard classic follows the difficulty table`() {
        val spec = LevelSpec.classic(seed = 3L, difficulty = Difficulty.HARD)
        assertEquals(7, spec.need)
        assertEquals(300, spec.timeoutTicks)
        assertEquals(120, spec.speedPercent)
        assertEquals(85, spec.frameScalePercent)
    }

    @Test
    fun `the factory carries the seed and the attempt`() {
        val spec = LevelSpec.classic(seed = 0x1234L, attempt = 7)
        assertEquals(0x1234L, spec.seed)
        assertEquals(7, spec.attemptIndex)
    }

    @Test
    fun `species weights are all positive`() {
        val spec = LevelSpec.classic(seed = 1L)
        assertTrue(spec.species.isNotEmpty())
        for (weight in spec.species) assertTrue(weight.id, weight.weight > 0)
    }

    @Test
    fun `the level validates the round contract`() {
        val good = LevelSpec.classic(seed = 1L)
        assertTrue(good.need <= good.birds)
        assertTrue(good.photosPerBird > 0)
        assertTrue(good.timeoutTicks > 0)
        assertTrue(runCatching {
            good.copy(need = good.birds + 1)
        }.isFailure)
        assertTrue(runCatching {
            good.copy(birds = 0)
        }.isFailure)
        assertTrue(runCatching {
            good.copy(species = emptyList())
        }.isFailure)
    }

    @Test
    fun `star thresholds increase`() {
        val spec = LevelSpec.classic(seed = 1L)
        assertTrue(spec.stars.two < spec.stars.three)
    }

    @Test
    fun `the options are the accessibility settings that change the photo frame`() {
        val options = GameOptions()
        assertEquals(100, options.tolerancePercent)
        assertTrue(!options.biggerBirds)
        assertTrue(!options.aimAssist)
        assertTrue(runCatching { options.copy(tolerancePercent = 84) }.isFailure)
        assertTrue(runCatching { options.copy(tolerancePercent = 126) }.isFailure)
    }

    @Test
    fun `difficulty exposes the three documented rows`() {
        assertEquals(3, Difficulty.entries.size)
        assertEquals(listOf(80, 100, 120), Difficulty.entries.map { it.speedPercent })
        assertEquals(listOf(125, 100, 85), Difficulty.entries.map { it.frameScalePercent })
        assertEquals(listOf(480, 390, 300), Difficulty.entries.map { it.timeoutTicks })
    }
}
