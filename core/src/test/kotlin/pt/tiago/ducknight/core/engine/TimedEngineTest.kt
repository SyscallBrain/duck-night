package pt.tiago.ducknight.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.level.StarThresholds

class TimedEngineTest {
    private fun spec(seconds: Int = 5, need: Int = 1) = LevelSpec.timed(
        seed = 111L, seconds = seconds, need = need, stars = StarThresholds(100, 200),
    )

    @Test
    fun `the clock is part of the level`() {
        val state = Simulation.start(spec(seconds = 2))
        assertEquals(120, state.extras?.timedEndTick)
    }

    @Test
    fun `the shutter has a 0 point 35 second cooldown`() {
        var state = Simulation.start(spec())
        state = state.step(InputAction.Snap(1, 1)).state
        assertEquals(1, state.photosTaken)
        val blocked = state.step(InputAction.Snap(1, 1))
        assertEquals(1, blocked.state.photosTaken)
        val after = blocked.state.runUntil(30) { it.tick >= 22 }.first
        val allowed = after.step(InputAction.Snap(1, 1))
        assertEquals(2, allowed.state.photosTaken)
    }

    @Test
    fun `the round ends when the clock runs out`() {
        val state = Simulation.start(spec(seconds = 1))
        val (finished, events) = state.runUntil(100) { it.phase == RoundPhase.FINISHED }
        val summary = events.filterIsInstance<GameEvent.RoundEnded>().single().result
        assertEquals(60, finished.tick)
        assertTrue(!summary.passed)
    }

    @Test
    fun `a timed level keeps spawning while the clock runs`() {
        var state = Simulation.start(spec(seconds = 10, need = 1))
        state = state.step(state.birds.first().aim()).state
        val (next, _) = state.runUntil(500) { s -> s.birds.any { it.id == 1 } }
        assertTrue(next.birds.any { it.id == 1 })
    }

    @Test
    fun `a timed round passes with the required photos`() {
        var state = Simulation.start(spec(seconds = 10, need = 1))
        state = state.step(state.birds.first().aim()).state
        val (finished, events) = state.runUntil(700) { it.phase == RoundPhase.FINISHED }
        val summary = events.filterIsInstance<GameEvent.RoundEnded>().single().result
        assertTrue(summary.passed)
        assertTrue(summary.photographed >= 1)
        assertEquals(RoundPhase.FINISHED, finished.phase)
    }

    @Test
    fun `the cooldown does not consume film`() {
        var state = Simulation.start(spec(seconds = 10))
        state = state.step(InputAction.Snap(1, 1)).state
        val before = state.photosTaken
        val blocked = state.step(InputAction.Snap(1, 1))
        assertEquals(before, blocked.state.photosTaken)
        assertEquals(before, blocked.state.photosUsed)
    }

    @Test
    fun `timed levels ignore the per-bird photo limit`() {
        var state = Simulation.start(spec(seconds = 10))
        repeat(4) { state = state.step(InputAction.Snap(1, 1)).state }
        assertEquals(1, state.photosTaken)
        // Advance past the cooldown and take a second photo.
        state = state.runUntil(30) { it.tick >= 22 }.first
        state = state.step(InputAction.Snap(1, 1)).state
        assertEquals(2, state.photosTaken)
    }
}
