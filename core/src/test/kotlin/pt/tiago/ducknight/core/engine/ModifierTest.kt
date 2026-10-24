package pt.tiago.ducknight.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.tiago.ducknight.core.fixed.toUnits
import pt.tiago.ducknight.core.flight.BirdStatus
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.level.Modifier
import pt.tiago.ducknight.core.level.SpeciesWeight
import pt.tiago.ducknight.core.level.StarThresholds
import pt.tiago.ducknight.core.level.TypeRules
import pt.tiago.ducknight.core.photo.PhotoResolver
import pt.tiago.ducknight.core.weather.CoverBox
import pt.tiago.ducknight.core.weather.FogBank

class ModifierTest {
    private fun spec(
        modifiers: Set<Modifier>,
        rules: TypeRules = TypeRules(),
        species: List<SpeciesWeight> = listOf(SpeciesWeight("mallard", 100)),
    ) = LevelSpec.classic(
        seed = 131L, birds = 2, need = 1, stars = StarThresholds(100, 200),
    ).copy(modifiers = modifiers, rules = rules, species = species)

    @Test
    fun `wind moves the bird horizontally`() {
        val plain = Simulation.start(spec(emptySet()))
        val windy = Simulation.start(spec(setOf(Modifier.WIND), TypeRules(windStrength = 30)))
        val plainAfter = plain.runUntil(120) { it.tick >= 120 }.first
        val windyAfter = windy.runUntil(120) { it.tick >= 120 }.first
        assertNotEquals(plainAfter.birds.single().x, windyAfter.birds.single().x)
    }

    @Test
    fun `fog hides a bird inside a bank`() {
        val state = Simulation.start(
            spec(setOf(Modifier.FOG), TypeRules(fogBanks = listOf(FogBank(190, 60)))),
        )
        assertTrue(state.birds.single().hidden)
        val shot = state.step(state.birds.single().aim())
        assertTrue(shot.state.birdsPhotographed == 0)
    }

    @Test
    fun `a bird outside the fog bank is visible`() {
        val state = Simulation.start(
            spec(setOf(Modifier.FOG), TypeRules(fogBanks = listOf(FogBank(0, 20)))),
        )
        assertTrue(!state.birds.single().hidden)
        val shot = state.step(state.birds.single().aim())
        assertEquals(1, shot.state.birdsPhotographed)
    }

    @Test
    fun `fog announces when a bird disappears`() {
        val state = Simulation.start(
            spec(setOf(Modifier.FOG), TypeRules(fogBanks = listOf(FogBank(150, 20)))),
        )
        assertTrue(!state.birds.single().hidden)
        val result = state.runUntil(300) { s -> s.birds.singleOrNull()?.hidden == true }
        assertTrue(result.second.any { it is GameEvent.BirdHidden })
    }

    @Test
    fun `cover hides a bird behind a tree`() {
        val state = Simulation.start(
            spec(setOf(Modifier.COVER), TypeRules(coverBoxes = listOf(CoverBox(0, 180, 240, 60)))),
        )
        assertTrue(state.birds.single().hidden)
        assertTrue(state.step(state.birds.single().aim()).state.birdsPhotographed == 0)
    }

    @Test
    fun `snow changes the look but not the flight`() {
        val plain = Simulation.start(spec(emptySet()))
        val snowy = Simulation.start(spec(setOf(Modifier.SNOW)))
        val a = plain.runUntil(100) { it.tick >= 100 }.first
        val b = snowy.runUntil(100) { it.tick >= 100 }.first
        assertEquals(a.birds.single().x, b.birds.single().x)
        assertEquals(a.birds.single().y, b.birds.single().y)
        assertTrue(!b.birds.single().hidden)
    }

    @Test
    fun `rain reduces the sharpness of the photo`() {
        val plain = Simulation.start(spec(emptySet()))
        val rainySpec = spec(setOf(Modifier.RAIN))
        val bird = plain.birds.single()
        val normal = PhotoResolver.resolve(listOf(bird), toUnits(bird.x), toUnits(bird.y), plain.spec, plain.options)
        val rain = PhotoResolver.resolve(listOf(bird), toUnits(bird.x), toUnits(bird.y), rainySpec, plain.options)
        assertTrue(rain.quality < normal.quality)
    }

    @Test
    fun `night hides birds outside the lamp but a snap lights the target`() {
        val state = Simulation.start(spec(setOf(Modifier.NIGHT)))
        val bird = state.birds.single()
        assertTrue(bird.hidden)
        val shot = state.step(bird.aim())
        assertEquals(1, shot.state.birdsPhotographed)
    }

    @Test
    fun `the night lamp follows the last bird for a short while`() {
        var state = Simulation.start(spec(setOf(Modifier.NIGHT)))
        val bird = state.birds.single()
        state = state.step(bird.aim()).state
        assertTrue(state.extras?.nightFollowId == bird.id)
        val followed = state.runUntil(30) { it.tick >= state.tick + 25 }.first
        assertTrue(followed.extras?.nightFollowId == -1)
    }

    @Test
    fun `a diving bird is hidden and then surfaces`() {
        var state = Simulation.start(
            spec(
                setOf(Modifier.DIVE),
                TypeRules(),
                listOf(SpeciesWeight("pochard", 100)),
            ).copy(timeoutTicks = 600),
        )
        assertEquals(BirdStatus.FLYING, state.birds.single().status)
        assertTrue(state.birds.single().hidden)
        val (surfaced, events) = state.runUntil(300) { s -> s.birds.singleOrNull()?.hidden == false }
        assertTrue(!surfaced.birds.single().hidden)
        assertTrue(events.any { it is GameEvent.BirdRevealed })
    }

    @Test
    fun `golden good photos raise the multiplier and a miss resets it`() {
        var state = Simulation.start(
            spec(
                setOf(Modifier.GOLDEN),
                TypeRules(),
                listOf(SpeciesWeight("golden", 100)),
            ).copy(birds = 3),
        )
        val first = state.birds.single()
        state = state.step(first.aim()).state
        assertEquals(1, state.extras?.combo)
        assertEquals(150, state.extras?.multiplierPercent)
        val second = state.runUntil(500) { s -> s.birds.any { it.id == 1 } }.first
        state = second.step(second.birds.first { it.id == 1 }.aim()).state
        assertEquals(2, state.extras?.combo)
        assertEquals(200, state.extras?.multiplierPercent)
        val third = state.runUntil(500) { s -> s.birds.any { it.id == 2 } }.first
        state = third.step(InputAction.Snap(1, 1)).state
        assertEquals(0, state.extras?.combo)
        assertEquals(100, state.extras?.multiplierPercent)
    }

    @Test
    fun `manual advance blocks the next photo until the lever is pulled`() {
        var state = Simulation.start(spec(setOf(Modifier.MANUAL_ADVANCE)))
        state = state.step(InputAction.Snap(1, 1)).state
        assertTrue(state.extras?.needsAdvance == true)
        val blocked = state.step(InputAction.Snap(1, 1))
        assertEquals(1, blocked.state.photosTaken)
        state = blocked.state.step(InputAction.Advance).state
        assertTrue(state.extras?.needsAdvance == false)
        val allowed = state.step(InputAction.Snap(1, 1))
        assertEquals(2, allowed.state.photosTaken)
    }

    @Test
    fun `manual advance announces the lever`() {
        val state = Simulation.start(spec(setOf(Modifier.MANUAL_ADVANCE)))
        val result = state.step(InputAction.Snap(1, 1))
        assertTrue(result.events.any { it is GameEvent.NeedsAdvance })
        assertTrue(result.state.extras?.needsAdvance == true)
        val advanced = result.state.step(InputAction.Advance)
        assertTrue(advanced.state.extras?.needsAdvance == false)
    }

    @Test
    fun `a modifier round can still finish`() {
        var state = Simulation.start(spec(setOf(Modifier.WIND), TypeRules(windStrength = 12)))
        state = state.step(state.birds.single().aim()).state
        val (finished, events) = state.runUntil(2_000) { it.phase == RoundPhase.FINISHED }
        assertTrue(events.any { it is GameEvent.RoundEnded })
        assertEquals(RoundPhase.FINISHED, finished.phase)
    }

    @Test
    fun `the protected modifier spawns a protected bird`() {
        val state = Simulation.start(
            spec(setOf(Modifier.PROTECTED), TypeRules(protectedBirds = 1)),
        )
        val (withHeron, _) = state.runUntil(100) { it.birds.any { b -> b.species.protected } }
        assertTrue(withHeron.birds.any { it.species.protected })
    }

    @Test
    fun `extra state keeps the simulation deterministic`() {
        val a = Simulation.start(spec(setOf(Modifier.FOG, Modifier.WIND), TypeRules(windStrength = 20)))
        val b = Simulation.start(spec(setOf(Modifier.FOG, Modifier.WIND), TypeRules(windStrength = 20)))
        assertEquals(a, b)
        val afterA = a.runUntil(200) { it.tick >= 200 }.first
        val afterB = b.runUntil(200) { it.tick >= 200 }.first
        assertEquals(afterA, afterB)
    }
}
