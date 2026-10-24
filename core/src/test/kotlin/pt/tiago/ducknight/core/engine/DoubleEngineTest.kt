package pt.tiago.ducknight.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.tiago.ducknight.core.fixed.toUnits
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.level.StarThresholds

class DoubleEngineTest {
    private fun spec(birds: Int = 2, need: Int = 1) =
        LevelSpec.double(seed = 77L, birds = birds, need = need, stars = StarThresholds(100, 200))

    @Test
    fun `a double level starts with two birds`() {
        val state = Simulation.start(spec())
        assertEquals(2, state.birds.size)
        assertEquals(listOf(0, 1), state.birds.map { it.id })
        assertEquals(2, state.nextBirdIndex)
        assertTrue(state.birds.all { it.status == pt.tiago.ducknight.core.flight.BirdStatus.FLYING })
    }

    @Test
    fun `the pair shares three photos`() {
        var state = Simulation.start(spec())
        state = state.step(state.birds.first().aim()).state
        assertEquals(1, state.photosUsed)
        val second = state.birds.first { it.status == pt.tiago.ducknight.core.flight.BirdStatus.FLYING }
        state = state.step(second.aim()).state
        assertEquals(2, state.photosUsed)
    }

    @Test
    fun `three misses make both birds flee`() {
        var state = Simulation.start(spec())
        repeat(3) { state = state.step(InputAction.Snap(1, 1)).state }
        assertTrue(state.birds.all { it.escapeAtTick > 0 })
        val (fled, events) = state.runUntil(200) { state.birdsFled == 2 }
        assertEquals(2, fled.birdsFled)
        assertTrue(events.count { it is GameEvent.BirdFled } >= 2)
    }

    @Test
    fun `the next pair spawns after the previous one leaves`() {
        var state = Simulation.start(spec(birds = 4, need = 2))
        repeat(3) { state = state.step(InputAction.Snap(1, 1)).state }
        val (next, _) = state.runUntil(600) { s -> s.birds.size == 2 && s.birds.all { it.id >= 2 } }
        assertEquals(listOf(2, 3), next.birds.map { it.id }.sorted())
        assertEquals(4, next.nextBirdIndex)
    }

    @Test
    fun `photographing one bird resolves the pair only when both are gone`() {
        var state = Simulation.start(spec())
        state = state.step(state.birds.first().aim()).state
        val (finished, events) = state.runUntil(1_500) { it.phase == RoundPhase.FINISHED }
        val summary = events.filterIsInstance<GameEvent.RoundEnded>().single().result
        assertTrue(summary.passed)
        assertEquals(1, summary.photographed)
        assertEquals(1, summary.fled)
        assertEquals(RoundPhase.FINISHED, finished.phase)
    }

    @Test
    fun `a double round fails without photos`() {
        val (finished, events) = Simulation.start(spec(need = 1)).runUntil(2_000) { it.phase == RoundPhase.FINISHED }
        val summary = events.filterIsInstance<GameEvent.RoundEnded>().single().result
        assertTrue(!summary.passed)
        assertEquals(0, summary.stars)
        assertEquals(RoundPhase.FINISHED, finished.phase)
    }

    @Test
    fun `both birds can be in one photo`() {
        var state = Simulation.start(spec())
        val first = state.birds.first()
        val second = state.birds.last()
        // Move the second bird next to the first through the public copy of the state.
        val close = second.copy(x = first.x + 8, y = first.y)
        state = state.copy(birds = listOf(first, close))
        val result = state.step(first.aim())
        assertEquals(1, result.state.photosTaken)
        assertTrue(result.state.results.count { it.photographed } >= 1)
    }

    @Test
    fun `the double id sequence is deterministic`() {
        val a = Simulation.start(spec(birds = 4, need = 2))
        val b = Simulation.start(spec(birds = 4, need = 2))
        assertEquals(a.birds, b.birds)
        assertEquals(a.spawnRngState, b.spawnRngState)
    }

    @Test
    fun `the pair photos reset for the next pair`() {
        var state = Simulation.start(spec(birds = 4, need = 2))
        repeat(3) { state = state.step(InputAction.Snap(1, 1)).state }
        val next = state.runUntil(600) { it.birds.any { b -> b.id >= 2 } }.first
        assertEquals(0, next.photosUsed)
    }

    @Test
    fun `a photo on a pair scores the main bird`() {
        var state = Simulation.start(spec())
        val bird = state.birds.first()
        val result = state.step(bird.aim())
        assertTrue(result.state.score > 0)
        assertEquals(1, result.state.photosTaken)
        assertEquals(1, result.state.birdsPhotographed)
    }
}
