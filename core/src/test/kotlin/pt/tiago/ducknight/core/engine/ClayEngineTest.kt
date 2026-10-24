package pt.tiago.ducknight.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.tiago.ducknight.core.fixed.fx
import pt.tiago.ducknight.core.fixed.toUnits
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.level.Modifier
import pt.tiago.ducknight.core.level.StarThresholds
import pt.tiago.ducknight.core.level.TypeRules

class ClayEngineTest {
    private fun spec(pairs: Int = 1, need: Int = 1) = LevelSpec.clay(
        seed = 141L, pairs = pairs, need = need, stars = StarThresholds(100, 200),
    )

    @Test
    fun `a clay level starts with a pair and three corks`() {
        val state = Simulation.start(spec())
        assertEquals(2, state.extras?.clays?.size)
        assertEquals(1, state.extras?.clayPairsSpawned)
        assertEquals(3, state.extras?.corksLeft)
        assertTrue(state.birds.isEmpty())
    }

    @Test
    fun `breaking a plate scores and consumes a cork`() {
        val state = Simulation.start(spec())
        val clay = state.extras!!.clays.first()
        val result = state.step(InputAction.Snap(toUnits(clay.x), toUnits(clay.y)))
        assertEquals(2, result.state.extras?.corksLeft)
        assertEquals(1, result.state.birdsPhotographed)
        assertTrue(result.state.score > 0)
        assertTrue(result.events.any { it is GameEvent.ClayBroken })
    }

    @Test
    fun `a cork miss consumes a cork but does not score`() {
        val state = Simulation.start(spec())
        val result = state.step(InputAction.Snap(1, 1))
        assertEquals(0, result.state.score)
        assertEquals(2, result.state.extras?.corksLeft)
        assertTrue(result.events.any { it is GameEvent.CorkMiss })
    }

    @Test
    fun `three corks end a pair`() {
        var state = Simulation.start(spec(pairs = 2))
        repeat(3) { state = state.step(InputAction.Snap(1, 1)).state }
        assertEquals(2, state.birdsFled)
        assertTrue(state.extras!!.clays.all { it.status != pt.tiago.ducknight.core.fair.ClayStatus.FLYING })
    }

    @Test
    fun `a far plate is worth more`() {
        val state = Simulation.start(spec())
        val clay = state.extras!!.clays.first().copy(x = fx(120), y = fx(100), scalePermille = 500)
        val near = state.copy(extras = state.extras!!.copy(clays = listOf(clay)))
        val result = near.step(InputAction.Snap(120, 100))
        assertEquals(1, result.state.birdsPhotographed)
        assertEquals(750, result.state.score)
    }

    @Test
    fun `a clay pair ends the round and gives the summary`() {
        var state = Simulation.start(spec())
        val first = state.extras!!.clays.first()
        state = state.step(InputAction.Snap(toUnits(first.x), toUnits(first.y))).state
        val second = state.extras!!.clays.first { it.status == pt.tiago.ducknight.core.fair.ClayStatus.FLYING }
        state = state.step(InputAction.Snap(toUnits(second.x), toUnits(second.y))).state
        val summary = state.summary ?: state.runUntil(200) { it.phase == RoundPhase.FINISHED }
            .second.filterIsInstance<GameEvent.RoundEnded>().single().result
        assertTrue(summary.passed)
        assertEquals(2, summary.platesBroken)
        assertEquals(2, summary.need + 1)
    }

    @Test
    fun `a failed clay round has no stars`() {
        var state = Simulation.start(spec(need = 2))
        repeat(3) { state = state.step(InputAction.Snap(1, 1)).state }
        val summary = state.summary ?: state.runUntil(200) { it.phase == RoundPhase.FINISHED }
            .second.filterIsInstance<GameEvent.RoundEnded>().single().result
        assertTrue(!summary.passed)
        assertEquals(0, summary.stars)
    }

    @Test
    fun `the next pair arrives after the gap`() {
        var state = Simulation.start(spec(pairs = 2, need = 1))
        repeat(3) { state = state.step(InputAction.Snap(1, 1)).state }
        val (next, _) = state.runUntil(200) { it.extras?.clayPairsSpawned == 2 }
        assertEquals(2, next.extras?.clayPairsSpawned)
        assertEquals(2, next.extras?.clays?.size)
        assertEquals(3, next.extras?.corksLeft)
    }

    @Test
    fun `plates shrink as they fly`() {
        var state = Simulation.start(spec())
        val before = state.extras!!.clays.first().scalePermille
        state = state.runUntil(30) { it.tick >= 30 }.first
        assertTrue(state.extras!!.clays.first().scalePermille < before)
    }

    @Test
    fun `manual advance works at the fair`() {
        var state = Simulation.start(
            spec().copy(modifiers = setOf(Modifier.MANUAL_ADVANCE), rules = TypeRules(clayPairs = 1)),
        )
        state = state.step(InputAction.Snap(1, 1)).state
        assertTrue(state.extras?.needsAdvance == true)
        val blocked = state.step(InputAction.Snap(1, 1))
        assertEquals(1, blocked.state.photosTaken)
        state = blocked.state.step(InputAction.Advance).state
        val allowed = state.step(InputAction.Snap(1, 1))
        assertEquals(2, allowed.state.photosTaken)
    }

    @Test
    fun `the clay state is deterministic`() {
        assertEquals(Simulation.start(spec()).extras?.clays, Simulation.start(spec()).extras?.clays)
    }
}
