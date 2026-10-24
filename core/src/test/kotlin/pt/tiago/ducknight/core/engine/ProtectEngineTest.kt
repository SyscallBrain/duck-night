package pt.tiago.ducknight.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.tiago.ducknight.core.flight.BirdStatus
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.level.SpeciesWeight
import pt.tiago.ducknight.core.level.StarThresholds
import pt.tiago.ducknight.core.photo.PhotoOutcome

class ProtectEngineTest {
    private fun spec(birds: Int = 2, protected: Int = 1, need: Int = 1) = LevelSpec.protect(
        seed = 121L, birds = birds, protectedBirds = protected, need = need,
        stars = StarThresholds(100, 200),
    )

    @Test
    fun `the target pool never spawns a protected bird as the target`() {
        val targetSpec = spec(birds = 1, protected = 0, need = 1).copy(
            species = listOf(SpeciesWeight("mallard", 1), SpeciesWeight("white_stork", 99)),
        )
        val state = Simulation.start(targetSpec)
        assertEquals("mallard", state.birds.single().species.id)
    }

    @Test
    fun `a protected level uses the protected species in its pool`() {
        val storkSpec = spec(birds = 2, protected = 1, need = 1).copy(
            species = listOf(SpeciesWeight("mallard", 70), SpeciesWeight("white_stork", 30)),
        )
        val state = Simulation.start(storkSpec)
            .runUntil(100) { s -> s.birds.any { it.species.protected } }.first
        assertEquals("white_stork", state.birds.single { it.species.protected }.species.id)
    }

    @Test
    fun `a protected bird arrives on schedule`() {
        val state = Simulation.start(spec())
        val (withHeron, _) = state.runUntil(100) { it.birds.any { b -> b.species.protected } }
        val heron = withHeron.birds.single { it.species.protected }
        assertTrue(heron.species.protected)
        assertTrue(heron.y < pt.tiago.ducknight.core.fixed.fx(120))
    }

    @Test
    fun `photographing a protected bird costs five hundred points and does not count`() {
        var state = Simulation.start(spec())
        state = state.runUntil(100) { it.birds.any { b -> b.species.protected } }.first
        val heron = state.birds.single { it.species.protected }
        val before = state.birdsPhotographed
        val result = state.step(heron.aim())
        assertEquals(-500, result.state.score)
        assertEquals(before, result.state.birdsPhotographed)
        assertEquals(1, result.state.extras?.protectedPhotoCount)
        val photo = result.events.filterIsInstance<GameEvent.PhotoResult>().single().photo
        assertEquals(PhotoOutcome.PROTECTED, photo.outcome)
    }

    @Test
    fun `a target photo passes a protect level`() {
        var state = Simulation.start(spec(birds = 1, protected = 0, need = 1))
        state = state.step(state.birds.first().aim()).state
        val (finished, events) = state.runUntil(1_500) { it.phase == RoundPhase.FINISHED }
        val summary = events.filterIsInstance<GameEvent.RoundEnded>().single().result
        assertTrue(summary.passed)
        assertEquals(0, summary.protectedPhotos)
        assertEquals(RoundPhase.FINISHED, finished.phase)
    }

    @Test
    fun `two protected photos fail the round even with the target`() {
        var state = Simulation.start(spec(birds = 1, protected = 2, need = 1))
        state = state.runUntil(400) { it.birds.count { b -> b.species.protected } >= 2 }.first
        val herons = state.birds.filter { it.species.protected }
        state = state.step(herons.first().aim()).state
        state = state.step(herons.last().aim()).state
        assertEquals(2, state.extras?.protectedPhotoCount)
        val target = state.birds.first { !it.species.protected }
        state = state.step(target.aim()).state
        val (_, events) = state.runUntil(2_000) { it.phase == RoundPhase.FINISHED }
        val summary = events.filterIsInstance<GameEvent.RoundEnded>().single().result
        assertTrue(!summary.passed)
    }

    @Test
    fun `a protected bird crosses and leaves without a result`() {
        val state = Simulation.start(
            spec(birds = 1, protected = 1, need = 1).copy(timeoutTicks = 1_000),
        )
        val (left, _) = state.runUntil(2_000) { s -> s.birds.none { it.species.protected } }
        assertTrue(left.results.none { it.speciesId == "heron" })
    }

    @Test
    fun `the target count is independent of protected birds`() {
        val state = Simulation.start(spec(birds = 3, protected = 2, need = 2))
        assertEquals(3, state.spec.birds)
        assertEquals(2, state.extras?.rules?.protectedBirds)
    }

    @Test
    fun `protected birds do not consume the target spawn index`() {
        var state = Simulation.start(spec(birds = 3, protected = 1, need = 2))
        state = state.runUntil(100) { it.birds.any { b -> b.species.protected } }.first
        assertEquals(1, state.nextBirdIndex)
        assertEquals(2, state.birds.size)
    }

    @Test
    fun `a mixed photo keeps the target and applies the penalty`() {
        val state = Simulation.start(spec(birds = 1, protected = 0, need = 1))
        val target = state.birds.first()
        val heron = pt.tiago.ducknight.core.species.SpeciesCatalog.byId("heron")
        val protectedBird = pt.tiago.ducknight.core.flight.Flight.spawn(
            id = 1000, species = heron, pattern = pt.tiago.ducknight.core.species.FlightPattern.GLIDE,
            spec = state.spec, birdIndex = 0, spawnRngState = 1L,
        ).bird.copy(x = target.x + pt.tiago.ducknight.core.fixed.fx(2), y = target.y)
        val mixed = state.copy(birds = listOf(target, protectedBird))
        val result = mixed.step(target.aim())
        val photo = result.events.filterIsInstance<GameEvent.PhotoResult>().single().photo
        assertEquals(1, photo.protectedCount)
        assertEquals(250, photo.penalty)
        assertEquals(0, photo.groupSize - 1)
        assertTrue(result.state.birdsPhotographed == 1)
    }

    @Test
    fun `a protect level without protected birds behaves like classic`() {
        val state = Simulation.start(spec(birds = 1, protected = 0, need = 1))
        assertEquals(1, state.birds.size)
        assertTrue(!state.birds.single().species.protected)
    }
}
