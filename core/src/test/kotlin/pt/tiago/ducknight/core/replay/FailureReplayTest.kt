package pt.tiago.ducknight.core.replay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.tiago.ducknight.core.engine.InputAction
import pt.tiago.ducknight.core.engine.RoundPhase
import pt.tiago.ducknight.core.engine.Simulation
import pt.tiago.ducknight.core.fixed.toUnits
import pt.tiago.ducknight.core.level.GameOptions
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.photo.PhotoResolver

class FailureReplayTest {
    private val options = GameOptions()

    @Test
    fun `twenty known photos keep the exact viewfinder`() {
        for (index in 0 until 20) {
            val spec = LevelSpec.classic(seed = 1_000L + index, birds = 10, need = 1)
            val log = InputLog.of(
                InputLogEntry(5, InputAction.Snap(10 + index, 20 + index)),
                InputLogEntry(20, InputAction.Snap(200 - index, 100 + index)),
            )
            val replay = FailureReplay.build(
                spec = spec,
                options = options,
                log = log,
                untilTick = 40,
            )
            assertEquals(
                "case $index photo count",
                2,
                replay.photos.size,
            )
            assertEquals(listOf(10 + index, 200 - index), replay.photos.map { it.aimX })
            assertEquals(listOf(20 + index, 100 + index), replay.photos.map { it.aimY })
            val frame = PhotoResolver.frame(spec, options)
            assertTrue(replay.photos.all { it.frame == frame })
            assertEquals(5, replay.photos.first().tick)
            assertEquals(20, replay.photos.last().tick)
        }
    }

    @Test
    fun `the replay starts three seconds before the last fled bird`() {
        val spec = LevelSpec.classic(seed = 99L, birds = 1, need = 1).copy(timeoutTicks = 180)
        var state = Simulation.start(spec, options)
        var guard = 0
        while (state.phase == RoundPhase.RUNNING && guard < 3_000) {
            state = Simulation.step(state, emptyList()).state
            guard++
        }
        val fledTick = state.results.lastOrNull { !it.photographed }?.tick ?: state.tick
        val replay = FailureReplay.build(spec, options, InputLog(), state.tick)
        assertEquals(fledTick, replay.endTick)
        assertTrue("start ${replay.startTick}", replay.startTick >= 0)
        assertTrue("window ${replay.startTick}..${replay.endTick}", replay.endTick - replay.startTick <= 180)
        assertTrue(replay.focusBirdId >= 0)
        assertTrue(replay.trace.isNotEmpty())
        assertEquals(replay.focusBirdId, replay.trace.first().birdId)
    }

    @Test
    fun `the selected photo ends the replay at its own tick`() {
        val spec = LevelSpec.classic(seed = 5L, birds = 1, need = 1)
        val initial = Simulation.start(spec, options)
        val bird = initial.birds.single()
        val log = InputLog.of(
            InputLogEntry(1, InputAction.Snap(toUnits(bird.x), toUnits(bird.y))),
        )
        val direct = Simulation.step(initial, log.actionsAt(1)).state
        val resolved = direct.results.firstOrNull { it.photographed }
        assertTrue("the test snap must hit the bird", resolved != null)
        val replay = FailureReplay.build(spec, options, log, untilTick = 30, focusBirdId = bird.id)
        assertEquals(resolved!!.tick, replay.endTick)
        assertEquals(resolved.tick, replay.photos.single().tick)
    }

    @Test
    fun `a selected bird is the focus even if another bird fled`() {
        val spec = LevelSpec.classic(seed = 7L, birds = 1, need = 1).copy(timeoutTicks = 180)
        var state = Simulation.start(spec, options)
        var guard = 0
        while (state.phase == RoundPhase.RUNNING && guard < 3_000) {
            state = Simulation.step(state, emptyList()).state
            guard++
        }
        val selected = FailureReplay.build(spec, options, InputLog(), state.tick, focusBirdId = 42)
        assertEquals(42, selected.focusBirdId)
        assertTrue(selected.trace.isEmpty())
    }
}
