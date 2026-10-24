package pt.tiago.ducknight.core.flight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.tiago.ducknight.core.fixed.WORLD_HEIGHT
import pt.tiago.ducknight.core.fixed.WORLD_WIDTH
import pt.tiago.ducknight.core.fixed.fx
import pt.tiago.ducknight.core.level.Difficulty
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.random.SplitMix64
import pt.tiago.ducknight.core.species.FlightPattern
import pt.tiago.ducknight.core.species.SpeciesCatalog

/**
 * Property: across 10,000 CLASSIC seeds a bird is photographable
 * (visible and inside the world) for at least 70 % of its flight time.
 */
class FlightPropertyTest {
    @Test
    fun `ten thousand seeds stay photographable for at least seventy percent`() {
        val rng = SplitMix64(0xD00D_CAFE_1234_5678UL.toLong())
        val patterns = listOf(FlightPattern.ZIGZAG, FlightPattern.GLIDE, FlightPattern.DART)
        val species = listOf("mallard", "teal", "gadwall")
        var worst = 1.0
        var worstSeed = 0L
        var samples = 0
        repeat(10_000) { index ->
            val seed = rng.nextLong()
            val spec = LevelSpec.classic(seed = seed, difficulty = Difficulty.EASY)
            val speciesId = species[index % species.size]
            val pattern = patterns[index % patterns.size]
            var bird = Flight.spawn(
                id = 0,
                species = SpeciesCatalog.byId(speciesId),
                pattern = pattern,
                spec = spec,
                birdIndex = 0,
                spawnRngState = seed,
            ).bird
            var ticks = 0
            var visible = 0
            while (bird.status == BirdStatus.FLYING && ticks < spec.timeoutTicks) {
                if (Flight.isVisible(bird) && inWorld(bird)) visible++
                bird = Flight.step(bird, spec, 0)
                ticks++
            }
            assertTrue("seed $seed never flew", ticks > 0)
            val ratio = visible.toDouble() / ticks
            if (ratio < worst) {
                worst = ratio
                worstSeed = seed
            }
            samples++
            assertTrue("seed $seed ratio $ratio", ratio >= 0.70)
            assertEquals(BirdStatus.FLEEING, bird.status)
        }
        assertEquals(10_000, samples)
        println("Worst photographable ratio: %.4f (seed %d)".format(worst, worstSeed))
    }

    private fun inWorld(bird: BirdState): Boolean =
        bird.x in 0..fx(WORLD_WIDTH) && bird.y in 0..fx(WORLD_HEIGHT)
}
