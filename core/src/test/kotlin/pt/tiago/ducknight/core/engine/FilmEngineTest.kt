package pt.tiago.ducknight.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.level.StarThresholds

class FilmEngineTest {
    private fun spec(birds: Int = 2, frames: Int = 3, need: Int = 1) = LevelSpec.film(
        seed = 99L, birds = birds, filmFrames = frames, need = need, stars = StarThresholds(100, 200),
    )

    @Test
    fun `the film starts full`() {
        val state = Simulation.start(spec())
        assertEquals(3, state.extras?.filmLeft)
        assertEquals(0, state.photosTaken)
    }

    @Test
    fun `a miss consumes the shared roll`() {
        var state = Simulation.start(spec())
        state = state.step(InputAction.Snap(1, 1)).state
        assertEquals(2, state.extras?.filmLeft)
        assertEquals(1, state.photosTaken)
    }

    @Test
    fun `an empty roll blocks the shutter`() {
        var state = Simulation.start(spec())
        repeat(3) { state = state.step(InputAction.Snap(1, 1)).state }
        assertEquals(0, state.extras?.filmLeft)
        val result = state.step(InputAction.Snap(1, 1))
        assertEquals(3, result.state.photosTaken)
        assertTrue(result.events.none { it is GameEvent.PhotoResult })
    }

    @Test
    fun `the roll is not reset when the next bird arrives`() {
        var state = Simulation.start(spec(birds = 2, frames = 4))
        state = state.step(InputAction.Snap(1, 1)).state
        assertEquals(3, state.extras?.filmLeft)
        val next = state.runUntil(500) { it.birds.any { b -> b.id == 1 } }.first
        assertEquals(3, next.extras?.filmLeft)
    }

    @Test
    fun `the empty roll is announced`() {
        var state = Simulation.start(spec())
        var events = emptyList<GameEvent>()
        repeat(3) {
            val result = state.step(InputAction.Snap(1, 1))
            state = result.state
            events += result.events
        }
        assertTrue(events.any { it is GameEvent.FilmEmpty })
    }

    @Test
    fun `a film round can pass with one photo`() {
        var state = Simulation.start(spec(birds = 2, frames = 4, need = 1))
        state = state.step(state.birds.first().aim()).state
        val (finished, events) = state.runUntil(2_000) { it.phase == RoundPhase.FINISHED }
        val summary = events.filterIsInstance<GameEvent.RoundEnded>().single().result
        assertTrue(summary.passed)
        assertEquals(1, summary.photographed)
        assertEquals(RoundPhase.FINISHED, finished.phase)
    }

    @Test
    fun `a film round fails when the roll runs out`() {
        var state = Simulation.start(spec(birds = 2, frames = 1, need = 1))
        repeat(1) { state = state.step(InputAction.Snap(1, 1)).state }
        val (_, events) = state.runUntil(3_000) { it.phase == RoundPhase.FINISHED }
        val summary = events.filterIsInstance<GameEvent.RoundEnded>().single().result
        assertTrue(!summary.passed)
    }

    @Test
    fun `the film left is part of the deterministic state`() {
        val a = Simulation.start(spec())
        val b = Simulation.start(spec())
        assertEquals(a.extras?.filmLeft, b.extras?.filmLeft)
        assertEquals(a, b)
    }
}
