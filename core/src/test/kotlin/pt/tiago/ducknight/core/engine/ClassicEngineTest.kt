package pt.tiago.ducknight.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.tiago.ducknight.core.fixed.fx
import pt.tiago.ducknight.core.fixed.toUnits
import pt.tiago.ducknight.core.flight.BirdStatus
import pt.tiago.ducknight.core.flight.Flight
import pt.tiago.ducknight.core.hash.StateHasher
import pt.tiago.ducknight.core.level.Difficulty
import pt.tiago.ducknight.core.level.GameOptions
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.level.StarThresholds
import pt.tiago.ducknight.core.photo.PhotoOutcome

class ClassicEngineTest {
    private fun spec(
        seed: Long = 1L,
        birds: Int = 1,
        need: Int = 1,
        difficulty: Difficulty = Difficulty.MEDIUM,
        stars: StarThresholds = StarThresholds(100, 200),
    ) = LevelSpec.classic(seed = seed, birds = birds, need = need, difficulty = difficulty, stars = stars)

    private fun start(spec: LevelSpec, options: GameOptions = GameOptions()): GameState =
        Simulation.start(spec, options)

    private fun snapAt(birdId: Int, state: GameState): InputAction.Snap {
        val bird = state.birds.first { it.id == birdId }
        return InputAction.Snap(toUnits(bird.x), toUnits(bird.y))
    }

    private fun step(state: GameState, vararg inputs: InputAction): StepResult =
        Simulation.step(state, inputs.toList())

    private fun runUntilFinished(state: GameState, maxTicks: Int = 20_000): Pair<GameState, List<GameEvent>> {
        var current = state
        val events = mutableListOf<GameEvent>()
        var guard = 0
        while (current.phase != RoundPhase.FINISHED && guard < maxTicks) {
            val result = Simulation.step(current, emptyList())
            current = result.state
            events += result.events
            guard++
        }
        return current to events
    }

    @Test
    fun `start spawns the first bird`() {
        val state = start(spec())
        assertEquals(RoundPhase.RUNNING, state.phase)
        assertEquals(1, state.birds.size)
        assertEquals(0, state.birds.single().id)
        assertEquals(BirdStatus.FLYING, state.birds.single().status)
        assertEquals(1, state.nextBirdIndex)
        assertEquals(0, state.tick)
    }

    @Test
    fun `start is deterministic`() {
        val a = start(spec(seed = 99L, birds = 3, need = 2))
        val b = start(spec(seed = 99L, birds = 3, need = 2))
        assertEquals(a, b)
        assertEquals(StateHasher.hash(a), StateHasher.hash(b))
    }

    @Test
    fun `different seeds spawn different birds`() {
        val a = start(spec(seed = 1L, birds = 3, need = 2))
        val b = start(spec(seed = 2L, birds = 3, need = 2))
        assertNotEquals(a.birds.single(), b.birds.single())
    }

    @Test
    fun `a centered snap photographs the bird`() {
        val state = start(spec(birds = 3, need = 2))
        val bird = state.birds.single()
        val result = step(state, snapAt(bird.id, state))
        assertEquals(1, result.state.birdsPhotographed)
        assertEquals(1, result.state.photosTaken)
        assertTrue(result.state.score > 0)
        assertTrue(result.state.birds.single().resolved)
        assertEquals(BirdStatus.STARTLED, result.state.birds.single().status)
    }

    @Test
    fun `a snap emits the documented event sequence`() {
        val state = start(spec())
        val result = step(state, snapAt(0, state))
        assertEquals(
            listOf("SnapTaken", "PhotoResult", "BirdStartled", "BirdResolved"),
            result.events.map { it::class.simpleName },
        )
        val photo = result.events.filterIsInstance<GameEvent.PhotoResult>().single().photo
        assertEquals(PhotoOutcome.GOOD, photo.outcome)
    }

    @Test
    fun `a photo consumes one frame of film`() {
        val state = start(spec(birds = 3, need = 2))
        val result = step(state, snapAt(0, state))
        assertEquals(1, result.state.photosTaken)
        assertEquals(1, result.state.photosUsed)
    }

    @Test
    fun `a miss consumes film and records no bird`() {
        val state = start(spec(birds = 3, need = 2))
        val result = step(state, InputAction.Snap(1, 1))
        assertEquals(0, result.state.birdsPhotographed)
        assertEquals(1, result.state.photosUsed)
        assertTrue(result.state.score == 0)
        assertEquals(PhotoOutcome.MISS, (result.events.filterIsInstance<GameEvent.PhotoResult>().single()).photo.outcome)
    }

    @Test
    fun `three misses make the bird flee after 0 point 45 seconds`() {
        var state = start(spec(birds = 3, need = 2, difficulty = Difficulty.MEDIUM))
        repeat(3) { state = step(state, InputAction.Snap(1, 1)).state }
        assertEquals(3, state.photosUsed)
        assertTrue(state.birds.single().escapeAtTick > state.tick)
        val escapeAt = state.birds.single().escapeAtTick
        var events = emptyList<GameEvent>()
        while (state.tick < escapeAt) {
            val result = step(state)
            state = result.state
            events += result.events
        }
        assertTrue(events.any { it is GameEvent.BirdFled })
        assertEquals(1, state.birdsFled)
        assertEquals(0, state.birdsPhotographed)
    }

    @Test
    fun `a photographed bird flees half a second after the flash`() {
        var state = start(spec(birds = 3, need = 2))
        state = step(state, snapAt(0, state)).state
        repeat(28) { state = step(state).state }
        assertEquals(BirdStatus.STARTLED, state.birds.single().status)
        state = step(state).state
        assertEquals(BirdStatus.FLEEING, state.birds.single().status)
        assertTrue(state.birds.single().vy < 0)
    }

    @Test
    fun `a bird that times out flees without a photo`() {
        var state = start(spec(birds = 3, need = 2, difficulty = Difficulty.HARD))
        val timeout = state.spec.timeoutTicks
        var events = emptyList<GameEvent>()
        while (state.tick < timeout) {
            val result = step(state)
            state = result.state
            events += result.events
        }
        assertEquals(1, state.birdsFled)
        assertEquals(0, state.birdsPhotographed)
        assertTrue(events.any { it is GameEvent.BirdFled })
        assertTrue(events.any { it is GameEvent.BirdResolved && !it.result.photographed })
    }

    @Test
    fun `the next bird arrives only after the previous one leaves`() {
        var state = start(spec(birds = 2, need = 1))
        state = step(state, snapAt(0, state)).state
        assertEquals(1, state.nextBirdIndex)
        repeat(200) { state = step(state).state }
        assertEquals(2, state.nextBirdIndex)
        assertEquals(1, state.birds.size)
        assertEquals(1, state.birds.single().id)
    }

    @Test
    fun `each bird gets a fresh set of photos`() {
        var state = start(spec(birds = 2, need = 1))
        repeat(3) { state = step(state, InputAction.Snap(1, 1)).state }
        assertEquals(3, state.photosUsed)
        repeat(250) { state = step(state).state }
        assertEquals(1, state.birds.single().id)
        assertEquals(0, state.photosUsed)
    }

    @Test
    fun `a round with enough photos passes with at least one star`() {
        var state = start(spec(birds = 1, need = 1, stars = StarThresholds(100, 200)))
        state = step(state, snapAt(0, state)).state
        val (finished, events) = runUntilFinished(state)
        val summary = events.filterIsInstance<GameEvent.RoundEnded>().single().result
        assertTrue(summary.passed)
        assertTrue(summary.stars >= 1)
        assertEquals(1, summary.photographed)
        assertEquals(0, summary.fled)
        assertEquals(RoundPhase.FINISHED, finished.phase)
    }

    @Test
    fun `a round with no photos fails without stars`() {
        val (finished, events) = runUntilFinished(start(spec(birds = 1, need = 1)))
        val summary = events.filterIsInstance<GameEvent.RoundEnded>().single().result
        assertTrue(!summary.passed)
        assertEquals(0, summary.stars)
        assertEquals(0, summary.photographed)
        assertEquals(1, summary.fled)
        assertEquals(RoundPhase.FINISHED, finished.phase)
    }

    @Test
    fun `score thresholds add the second and third star`() {
        var state = start(spec(birds = 1, need = 1, stars = StarThresholds(1, 2)))
        state = step(state, snapAt(0, state)).state
        val (_, events) = runUntilFinished(state)
        val summary = events.filterIsInstance<GameEvent.RoundEnded>().single().result
        assertEquals(3, summary.stars)
    }

    @Test
    fun `high thresholds leave only the pass star`() {
        var state = start(spec(birds = 1, need = 1, stars = StarThresholds(999_999, 1_000_000)))
        state = step(state, snapAt(0, state)).state
        val (_, events) = runUntilFinished(state)
        val summary = events.filterIsInstance<GameEvent.RoundEnded>().single().result
        assertEquals(1, summary.stars)
    }

    @Test
    fun `a failed round gives no stars even with a high score`() {
        val (_, events) = runUntilFinished(start(spec(birds = 2, need = 2, stars = StarThresholds(1, 2))))
        val summary = events.filterIsInstance<GameEvent.RoundEnded>().single().result
        assertEquals(0, summary.stars)
        assertTrue(!summary.passed)
    }

    @Test
    fun `the round ends only once`() {
        var state = start(spec(birds = 1, need = 1))
        state = step(state, snapAt(0, state)).state
        val (finished, events) = runUntilFinished(state)
        assertTrue(events.count { it is GameEvent.RoundEnded } == 1)
        val again = step(finished)
        assertEquals(0, again.events.size)
        assertEquals(finished, again.state)
    }

    @Test
    fun `a snap between birds is ignored`() {
        var state = start(spec(birds = 1, need = 1))
        state = step(state, snapAt(0, state)).state
        val photos = state.photosTaken
        val result = step(state, InputAction.Snap(1, 1))
        assertEquals(photos, result.state.photosTaken)
        assertTrue(result.events.none { it is GameEvent.PhotoResult })
    }

    @Test
    fun `pause and resume do not change the simulation`() {
        val state = start(spec(birds = 3, need = 2))
        val paused = step(state, InputAction.Pause)
        val plain = step(state)
        assertEquals(plain.state, paused.state)
        assertEquals(plain.events.size, paused.events.size)
        val resumed = Simulation.step(paused.state, listOf(InputAction.Resume))
        assertEquals(Simulation.step(plain.state, emptyList()).state, resumed.state)
    }

    @Test
    fun `advance is inert until the film mechanic arrives`() {
        val state = start(spec(birds = 3, need = 2))
        assertEquals(step(state).state, step(state, InputAction.Advance).state)
    }

    @Test
    fun `two identical input sequences produce identical hashes`() {
        val spec = spec(seed = 44L, birds = 3, need = 2)
        fun play(): Long {
            var state = start(spec)
            state = step(state, snapAt(0, state)).state
            repeat(400) { state = step(state, InputAction.Snap(1, 1)).state }
            return StateHasher.hash(state)
        }
        assertEquals(play(), play())
    }

    @Test
    fun `a different input gives a different hash`() {
        val spec = spec(seed = 44L, birds = 3, need = 2)
        val state = start(spec)
        val a = step(state, snapAt(0, state)).state
        val b = step(state, InputAction.Snap(1, 1)).state
        assertNotEquals(StateHasher.hash(a), StateHasher.hash(b))
    }

    @Test
    fun `aim assist turns a near miss into a photo only when enabled`() {
        val spec = spec(birds = 3, need = 2)
        val state = start(spec)
        val bird = state.birds.single()
        val offAim = InputAction.Snap(toUnits(bird.x) + 32, toUnits(bird.y))
        val off = step(state, offAim)
        assertTrue(off.state.birdsPhotographed == 0)
        val assistedState = Simulation.start(spec, GameOptions(aimAssist = true))
        val on = step(assistedState, offAim)
        assertTrue(on.state.birdsPhotographed == 1)
        val event = on.events.filterIsInstance<GameEvent.PhotoResult>().single().photo
        assertEquals(-10, event.assistDx)
    }

    @Test
    fun `the score summary keeps the best quality`() {
        var state = start(spec(birds = 2, need = 1))
        state = step(state, snapAt(0, state)).state
        state = runUntilFinished(state).first
        val summary = state.summary!!
        assertTrue(summary.bestQuality in 0..100)
        assertTrue(summary.goodPhotos + summary.okPhotos <= summary.photosTaken)
    }

    @Test
    fun `the first bird is resolved when it leaves the screen`() {
        var state = start(spec(birds = 1, need = 1))
        state = step(state, snapAt(0, state)).state
        val (finished, _) = runUntilFinished(state)
        assertEquals(1, finished.results.size)
        assertEquals(0, finished.birds.size)
    }

    @Test
    fun `bird ids follow the spawn order`() {
        var state = start(spec(birds = 3, need = 1))
        val ids = mutableListOf<Int>()
        while (state.phase != RoundPhase.FINISHED) {
            state.birds.forEach { if (it.id !in ids) ids += it.id }
            state = Simulation.step(state, emptyList()).state
        }
        assertEquals(listOf(0, 1, 2), ids.distinct().sorted())
    }
}
