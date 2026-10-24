package pt.tiago.ducknight.core.hash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import pt.tiago.ducknight.core.engine.GameState
import pt.tiago.ducknight.core.engine.RoundPhase
import pt.tiago.ducknight.core.engine.Simulation
import pt.tiago.ducknight.core.level.Difficulty
import pt.tiago.ducknight.core.level.GameOptions
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.level.StarThresholds

class StateHasherTest {
    private fun state(seed: Long = 1L): GameState =
        Simulation.start(LevelSpec.classic(seed = seed, birds = 3, need = 2))

    @Test
    fun `the same state hashes to the same value`() {
        assertEquals(StateHasher.hash(state()), StateHasher.hash(state()))
    }

    @Test
    fun `the tick changes the hash`() {
        val base = state()
        assertNotEquals(StateHasher.hash(base), StateHasher.hash(base.copy(tick = base.tick + 1)))
    }

    @Test
    fun `the level seed changes the hash`() {
        assertNotEquals(StateHasher.hash(state(1L)), StateHasher.hash(state(2L)))
    }

    @Test
    fun `the accessibility options change the hash`() {
        val base = state()
        assertNotEquals(
            StateHasher.hash(base),
            StateHasher.hash(base.copy(options = GameOptions(tolerancePercent = 125))),
        )
    }

    @Test
    fun `the bird state is part of the hash`() {
        val base = state()
        val bird = base.birds.single()
        assertNotEquals(
            StateHasher.hash(base),
            StateHasher.hash(base.copy(birds = listOf(bird.copy(x = bird.x + 1)))),
        )
    }

    @Test
    fun `the random streams are part of the hash`() {
        val base = state()
        assertNotEquals(StateHasher.hash(base), StateHasher.hash(base.copy(spawnRngState = base.spawnRngState + 1)))
        assertNotEquals(StateHasher.hash(base), StateHasher.hash(base.copy(weatherRngState = base.weatherRngState + 1)))
    }

    @Test
    fun `the score and counters are part of the hash`() {
        val base = state()
        assertNotEquals(StateHasher.hash(base), StateHasher.hash(base.copy(score = base.score + 1)))
        assertNotEquals(StateHasher.hash(base), StateHasher.hash(base.copy(photosTaken = base.photosTaken + 1)))
        assertNotEquals(StateHasher.hash(base), StateHasher.hash(base.copy(birdsPhotographed = 1)))
    }

    @Test
    fun `the phase is part of the hash`() {
        val base = state()
        assertNotEquals(StateHasher.hash(base), StateHasher.hash(base.copy(phase = RoundPhase.FINISHED)))
    }

    @Test
    fun `the star thresholds are part of the hash`() {
        val base = state()
        val other = LevelSpec.classic(
            seed = 1L, birds = 3, need = 2,
            stars = StarThresholds(4500, 6500),
            difficulty = Difficulty.MEDIUM,
        )
        assertNotEquals(
            StateHasher.hash(base),
            StateHasher.hash(base.copy(spec = other)),
        )
    }

    @Test
    fun `the starting hash is locked`() {
        // Locked after the first run: certificates and saved levels depend on this value.
        assertEquals(5931704948631361375L, StateHasher.hash(state()))
    }
}
