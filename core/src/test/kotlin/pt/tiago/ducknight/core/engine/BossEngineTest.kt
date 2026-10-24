package pt.tiago.ducknight.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.tiago.ducknight.core.flight.BirdState
import pt.tiago.ducknight.core.flight.BirdStatus
import pt.tiago.ducknight.core.flight.BossStyle
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.level.SpeciesWeight
import pt.tiago.ducknight.core.level.StarThresholds

class BossEngineTest {
    /** The album rules are checked on the Old Mallard: it never hides, so every aimed photo can land. */
    private fun spec(pages: Int = 3) = LevelSpec.boss(
        seed = 151L, pages = pages, stars = StarThresholds(100, 200),
    ).copy(species = listOf(SpeciesWeight("old_mallard", 100)))

    private fun styled(species: String) = spec().copy(species = listOf(SpeciesWeight(species, 100)))

    private fun GameState.boss(): BirdState = birds.first { it.bossPhase > 0 }

    /** Waits until the legendary bird is back in sight after a page, then photographs it. */
    private fun GameState.snapBoss(): GameState {
        val (ready, _) = runUntil(400) { state -> state.birds.any { it.bossPhase > 0 && !it.hidden && it.status == BirdStatus.FLYING } }
        return ready.step(ready.birds.first { it.bossPhase > 0 && !it.hidden }.aim()).state
    }

    @Test
    fun `a boss level starts with the legendary bird`() {
        val state = Simulation.start(spec())
        assertEquals(1, state.birds.size)
        assertEquals("old_mallard", state.birds.single().species.id)
        assertEquals(1, state.extras?.boss?.phase)
        assertEquals(0, state.extras?.boss?.pages)
    }

    @Test
    fun `a boss uses the legendary species of its spec`() {
        val wind = spec().copy(species = listOf(SpeciesWeight("wind_goose", 100)))
        assertEquals("wind_goose", Simulation.start(wind).birds.single().species.id)
    }

    @Test
    fun `a custom boss can fill its album`() {
        val wind = spec().copy(species = listOf(SpeciesWeight("wind_goose", 100)))
        var state = Simulation.start(wind)
        repeat(3) { state = state.snapBoss() }
        val (_, events) = state.runUntil(900) { it.phase == RoundPhase.FINISHED }
        val summary = events.filterIsInstance<GameEvent.RoundEnded>().single().result
        assertTrue(summary.passed)
        assertEquals(3, summary.photographed)
    }

    @Test
    fun `three good photos fill the album`() {
        var state = Simulation.start(spec())
        repeat(3) { state = state.snapBoss() }
        assertEquals(3, state.extras?.boss?.pages)
        val (finished, events) = state.runUntil(500) { it.phase == RoundPhase.FINISHED }
        val summary = events.filterIsInstance<GameEvent.RoundEnded>().single().result
        assertTrue(summary.passed)
        assertEquals(3, summary.bossPages)
        assertEquals(RoundPhase.FINISHED, finished.phase)
    }

    @Test
    fun `after a page the bird is out of sight for a moment`() {
        val state = Simulation.start(spec()).snapBoss()
        assertTrue(state.boss().hidden)
        val (back, _) = state.runUntil(BossStyle.PAGE_REST_TICKS + 2) { false }
        assertTrue(!back.boss().hidden)
    }

    @Test
    fun `a good photo changes the phase`() {
        var state = Simulation.start(spec())
        val result = state.step(state.birds.single().aim())
        assertEquals(2, result.state.extras?.boss?.phase)
        assertTrue(result.events.any { it is GameEvent.BossPhase })
    }

    @Test
    fun `a bad photo does not fill a page`() {
        val state = Simulation.start(spec())
        val boss = state.birds.single()
        val result = state.step(InputAction.Snap(pt.tiago.ducknight.core.fixed.toUnits(boss.x) + 20, pt.tiago.ducknight.core.fixed.toUnits(boss.y)))
        assertEquals(0, result.state.extras?.boss?.pages)
        assertEquals(1, result.state.photosTaken)
        assertEquals(BirdStatus.FLYING, result.state.birds.single().status)
    }

    @Test
    fun `the boss is startled only when the album is complete`() {
        var state = Simulation.start(spec())
        state = state.snapBoss()
        assertEquals(BirdStatus.FLYING, state.birds.single().status)
        state = state.snapBoss()
        assertEquals(BirdStatus.FLYING, state.birds.single().status)
        state = state.snapBoss()
        assertEquals(BirdStatus.STARTLED, state.birds.single().status)
    }

    @Test
    fun `three good photos give at least two stars`() {
        var state = Simulation.start(spec())
        repeat(3) { state = state.snapBoss() }
        val (_, events) = state.runUntil(500) { it.phase == RoundPhase.FINISHED }
        val summary = events.filterIsInstance<GameEvent.RoundEnded>().single().result
        assertTrue(summary.stars >= 2)
    }

    @Test
    fun `running out of time fails the album`() {
        val short = spec().copy(timeoutTicks = 120)
        val (finished, events) = Simulation.start(short).runUntil(300) { it.phase == RoundPhase.FINISHED }
        val summary = events.filterIsInstance<GameEvent.RoundEnded>().single().result
        assertTrue(!summary.passed)
        assertEquals(RoundPhase.FINISHED, finished.phase)
    }

    @Test
    fun `running out of photos fails the album`() {
        val short = spec().copy(photosPerBird = 2)
        var state = Simulation.start(short)
        repeat(2) { state = state.step(InputAction.Snap(1, 1)).state }
        val summary = state.summary ?: state.runUntil(200) { it.phase == RoundPhase.FINISHED }
            .second.filterIsInstance<GameEvent.RoundEnded>().single().result
        assertTrue(!summary.passed)
        assertTrue(state.tick <= 200)
    }

    @Test
    fun `the album page count is part of the summary`() {
        var state = Simulation.start(spec())
        state = state.snapBoss()
        state = state.snapBoss()
        assertEquals(2, state.extras?.boss?.pages)
        assertEquals(2, state.extras?.boss?.phase?.minus(1))
    }

    @Test
    fun `the twin teals fly as two birds and swap places on a page`() {
        val state = Simulation.start(styled("twin_teal"))
        assertEquals(2, state.birds.count { it.bossPhase > 0 })
        val (first, second) = state.birds
        val after = state.step(first.aim()).state
        assertEquals(1, after.extras?.boss?.pages)
        // Swapped by the photo, then moved by one tick of flight.
        assertTrue(kotlin.math.abs(after.birds.first { it.id == first.id }.x - second.x) < 3 * 256)
        assertTrue(kotlin.math.abs(after.birds.first { it.id == second.id }.x - first.x) < 3 * 256)
    }

    @Test
    fun `the thunder flock flies in a V and the lightning breaks it`() {
        val state = Simulation.start(styled("thunder_flock"))
        assertEquals(6, state.birds.size)
        val (_, events) = state.runUntil(BossStyle.stormGap(1) + 2) { false }
        assertTrue(events.any { it is GameEvent.Lightning })
    }

    @Test
    fun `the thief crow steals a page after three seconds`() {
        var state = Simulation.start(styled("thief_crow"))
        state = state.step(state.boss().aim()).state
        assertEquals(1, state.extras?.boss?.pages)
        val (after, events) = state.runUntil(BossStyle.CROW_STEAL_TICKS + 2) { false }
        assertTrue(events.any { it is GameEvent.BossStole })
        assertEquals(0, after.extras?.boss?.pages)
    }

    @Test
    fun `the mist goose is only visible part of every cycle`() {
        val (visible, _) = Simulation.start(styled("mist_goose")).runUntil(10) { false }
        assertTrue(!visible.boss().hidden)
        val (hidden, _) = Simulation.start(styled("mist_goose")).runUntil(BossStyle.mistVisibleTicks(1) + 5) { false }
        assertTrue(hidden.boss().hidden)
    }

    @Test
    fun `the ghost duck shows itself only around its turns`() {
        val (state, _) = Simulation.start(styled("ghost_duck")).runUntil(30) { false }
        assertTrue(state.boss().hidden)
    }

    @Test
    fun `the royal grebe dives and surfaces somewhere else`() {
        val start = Simulation.start(styled("royal_grebe"))
        val (diving, _) = start.runUntil(BossStyle.GREBE_FLY_TICKS + 2) { false }
        assertTrue(diving.boss().hidden)
        val (surfaced, _) = diving.runUntil(BossStyle.grebeDiveTicks(1) + 2) { false }
        assertTrue(!surfaced.boss().hidden)
        assertTrue(surfaced.boss().x != diving.boss().x)
    }

    @Test
    fun `the royal snow goose needs the lever after each photo`() {
        val state = Simulation.start(styled("royal_snow_goose"))
        val after = state.step(state.boss().aim()).state
        assertTrue(after.extras?.needsAdvance == true)
    }

    @Test
    fun `the wind goose gathers a flock for the last page`() {
        var state = Simulation.start(styled("wind_goose"))
        repeat(2) { state = state.snapBoss() }
        state = state.step().state
        assertEquals(5, state.birds.size)
    }

    @Test
    fun `the golden eider raises the multiplier with good photos`() {
        var state = Simulation.start(styled("golden_eider"))
        var raised = false
        repeat(10) {
            val boss = state.birds.firstOrNull { it.bossPhase > 0 && it.status == BirdStatus.FLYING } ?: return@repeat
            val result = state.step(boss.aim())
            raised = raised || result.events.any { it is GameEvent.ComboChanged && it.multiplierPercent > 100 }
            state = result.state.runUntil(25) { false }.first
        }
        assertTrue(raised)
    }

    @Test
    fun `a photo with a multiplier is reported once, together with the combo`() {
        val state = Simulation.start(styled("golden_eider"))
        val events = state.step(state.boss().aim()).events
        assertEquals(1, events.count { it is GameEvent.PhotoResult })
    }
}
