package pt.tiago.ducknight.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.tiago.ducknight.core.fixed.fx
import pt.tiago.ducknight.core.flight.BirdStatus
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.level.StarThresholds
import pt.tiago.ducknight.core.species.FlightPattern

class FlockEngineTest {
    private fun spec(need: Int = 2) = LevelSpec.flock(
        seed = 88L, flocks = 1, flockSize = 3, need = need, stars = StarThresholds(100, 200),
    )

    @Test
    fun `a flock starts with a leader and two followers`() {
        val state = Simulation.start(spec())
        assertEquals(3, state.birds.size)
        val leader = state.birds.single { it.leaderId == -1 }
        assertEquals(0, leader.id)
        assertEquals(FlightPattern.GLIDE, leader.pattern)
        assertEquals(2, state.birds.count { it.pattern == FlightPattern.FORMATION })
        assertTrue(state.birds.filter { it.leaderId == 0 }.all { it.formationOffsetX < 0 })
    }

    @Test
    fun `followers keep their offset from the leader`() {
        var state = Simulation.start(spec())
        val before = state.birds.associateBy { it.id }
        state = state.step().state
        val leader = state.birds.single { it.id == 0 }
        for (bird in state.birds.filter { it.leaderId == 0 }) {
            val offsetX = bird.formationOffsetX
            val expected = leader.x + offsetX
            assertTrue("bird ${bird.id} x=${bird.x} expected=$expected", kotlin.math.abs(bird.x - expected) <= fx(2))
        }
        assertNotEquals(before.getValue(0).x, leader.x)
    }

    @Test
    fun `a photo breaks the V`() {
        var state = Simulation.start(spec())
        state = state.step(state.birds.first().aim()).state
        assertTrue(state.extras?.formationBroken == true)
        assertTrue(state.birds.none { it.pattern == FlightPattern.FORMATION })
    }

    @Test
    fun `six photos exhaust the flock`() {
        var state = Simulation.start(spec())
        val events = mutableListOf<GameEvent>()
        repeat(6) {
            val result = state.step(InputAction.Snap(1, 1))
            state = result.state
            events += result.events
        }
        assertTrue("photos=${state.photosTaken} used=${state.photosUsed} birds=${state.birds}", state.birds.all { it.escapeAtTick > 0 })
        val (done, later) = state.runUntil(400) { it.birdsFled == 3 }
        events += later
        assertEquals(3, done.birdsFled)
        assertTrue(events.any { it is GameEvent.FilmEmpty })
    }

    @Test
    fun `a flock round can pass with two of three birds`() {
        var state = Simulation.start(spec(need = 2))
        val first = state.birds.first()
        state = state.step(first.aim()).state
        val second = state.birds.first { it.status == BirdStatus.FLYING }
        state = state.step(second.aim()).state
        val (finished, events) = state.runUntil(2_000) { it.phase == RoundPhase.FINISHED }
        val summary = events.filterIsInstance<GameEvent.RoundEnded>().single().result
        assertTrue(summary.passed)
        assertTrue(summary.photographed >= 2)
        assertEquals(RoundPhase.FINISHED, finished.phase)
    }

    @Test
    fun `a flock round fails if too few birds are photographed`() {
        var state = Simulation.start(spec(need = 3))
        val first = state.birds.first()
        state = state.step(first.aim()).state
        val (_, events) = state.runUntil(3_000) { it.phase == RoundPhase.FINISHED }
        val summary = events.filterIsInstance<GameEvent.RoundEnded>().single().result
        assertTrue(!summary.passed)
    }

    @Test
    fun `a flock only spawns one wave`() {
        var state = Simulation.start(spec())
        repeat(600) { state = state.step().state }
        assertEquals(3, state.nextBirdIndex)
        assertTrue(state.birds.size <= 3)
    }

    @Test
    fun `a group photo in a flock counts all birds in the frame`() {
        var state = Simulation.start(spec())
        val leader = state.birds.first()
        val close = state.birds.map { it.copy(x = leader.x + fx(2) * it.id, y = leader.y) }
        state = state.copy(birds = close)
        val result = state.step(leader.aim())
        assertTrue(result.state.results.count { it.photographed } >= 2)
    }

    @Test
    fun `the flock spawn is deterministic`() {
        assertEquals(Simulation.start(spec()).birds, Simulation.start(spec()).birds)
    }

    @Test
    fun `the next flock follows after the first one is resolved`() {
        val spec = LevelSpec.flock(
            seed = 88L, flocks = 2, flockSize = 3, need = 2, stars = StarThresholds(100, 200),
        )
        var state = Simulation.start(spec)
        repeat(6) { state = state.step(InputAction.Snap(1, 1)).state }
        val (next, _) = state.runUntil(1_500) { s -> s.birds.any { it.id >= 3 } }
        assertEquals(3, next.birds.minOfOrNull { it.id } ?: -1)
    }
}
