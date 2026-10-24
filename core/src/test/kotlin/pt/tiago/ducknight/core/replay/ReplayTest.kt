package pt.tiago.ducknight.core.replay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.tiago.ducknight.core.engine.GameState
import pt.tiago.ducknight.core.engine.InputAction
import pt.tiago.ducknight.core.engine.RoundPhase
import pt.tiago.ducknight.core.engine.Simulation
import pt.tiago.ducknight.core.fixed.toUnits
import pt.tiago.ducknight.core.hash.StateHasher
import pt.tiago.ducknight.core.level.GameOptions
import pt.tiago.ducknight.core.level.LevelSpec

class ReplayTest {
    private val spec = LevelSpec.classic(seed = 21L, birds = 3, need = 2)

    private fun aim(state: GameState): InputAction.Snap {
        val bird = state.birds.first()
        return InputAction.Snap(toUnits(bird.x), toUnits(bird.y))
    }

    @Test
    fun `replay to tick zero returns the initial state`() {
        val replay = Replay.run(spec, GameOptions(), InputLog(), 0)
        assertEquals(Simulation.start(spec), replay)
    }

    @Test
    fun `replay reproduces a direct run tick by tick`() {
        val ticks = 137
        val log = InputLog()
        var direct = Simulation.start(spec)
        val first = aim(direct)
        log.append(1, first)
        direct = Simulation.step(direct, log.actionsAt(1)).state
        for (tick in 2..ticks) {
            val action = if (tick == 40) InputAction.Snap(1, 1) else null
            if (action != null) log.append(tick, action)
            direct = Simulation.step(direct, log.actionsAt(tick)).state
        }
        val replayed = Replay.run(spec, GameOptions(), log, ticks)
        assertEquals(direct, replayed)
        assertEquals(StateHasher.hash(direct), StateHasher.hash(replayed))
    }

    @Test
    fun `a snap at a tick only happens at that tick`() {
        val state = Simulation.start(spec)
        val log = InputLog().append(5, aim(state))
        val atFour = Replay.run(spec, GameOptions(), log, 4)
        assertEquals(0, atFour.photosTaken)
        val atFive = Replay.run(spec, GameOptions(), log, 5)
        assertEquals(1, atFive.photosTaken)
    }

    @Test
    fun `pause and resume do not change the replayed hash`() {
        val log = InputLog()
            .append(1, InputAction.Pause)
            .append(2, InputAction.Resume)
        val withPause = Replay.run(spec, GameOptions(), log, 10)
        val plain = Replay.run(spec, GameOptions(), InputLog(), 10)
        assertEquals(StateHasher.hash(plain), StateHasher.hash(withPause))
    }

    @Test
    fun `replay after the round ends stays finished`() {
        var state = Simulation.start(LevelSpec.classic(seed = 3L, birds = 1, need = 1))
        state = Simulation.step(state, listOf(aim(state))).state
        val log = InputLog().append(1, aim(Simulation.start(LevelSpec.classic(seed = 3L, birds = 1, need = 1))))
        val finished = Replay.run(LevelSpec.classic(seed = 3L, birds = 1, need = 1), GameOptions(), log, 20_000)
        assertEquals(RoundPhase.FINISHED, finished.phase)
        val later = Replay.run(LevelSpec.classic(seed = 3L, birds = 1, need = 1), GameOptions(), log, 30_000)
        assertEquals(StateHasher.hash(finished), StateHasher.hash(later))
    }

    @Test
    fun `a save restores the exact state and hash`() {
        val log = InputLog().append(1, aim(Simulation.start(spec)))
        val state = Replay.run(spec, GameOptions(), log, 50)
        val saved = SavedLevel(spec, GameOptions(), state.tick, StateHasher.hash(state), log)
        val outcome = ResumeService.restore(saved)
        assertTrue(outcome is ResumeOutcome.Restored)
        val restored = (outcome as ResumeOutcome.Restored).state
        assertEquals(state, restored)
        assertEquals(saved.stateHash, StateHasher.hash(restored))
    }

    @Test
    fun `a save with a wrong hash is rejected`() {
        val state = Simulation.start(spec)
        val saved = SavedLevel(spec, GameOptions(), 10, stateHash = 42L, log = InputLog())
        val outcome = ResumeService.restore(saved)
        assertTrue(outcome is ResumeOutcome.HashMismatch)
        val mismatch = outcome as ResumeOutcome.HashMismatch
        assertEquals(42L, mismatch.expected)
        assertNotEquals(42L, mismatch.actual)
    }

    @Test
    fun `a save with an impossible tick is rejected`() {
        val saved = SavedLevel(spec, GameOptions(), tick = -1, stateHash = 0, log = InputLog())
        assertTrue(ResumeService.restore(saved) is ResumeOutcome.Invalid)
    }

    @Test
    fun `the save keeps the accessibility options`() {
        val options = GameOptions(tolerancePercent = 120, biggerBirds = true, aimAssist = true)
        val log = InputLog()
        val state = Replay.run(spec, options, log, 10)
        val saved = SavedLevel(spec, options, state.tick, StateHasher.hash(state), log)
        val restored = (ResumeService.restore(saved) as ResumeOutcome.Restored).state
        assertEquals(options, restored.options)
    }

    @Test
    fun `an empty log still replays the flight`() {
        val state = Replay.run(spec, GameOptions(), InputLog(), 100)
        assertEquals(100, state.tick)
        assertEquals(Simulation.start(spec).birds.single().id, state.birds.singleOrNull()?.id)
    }

    @Test
    fun `a log with a later snapshot does not affect an earlier replay`() {
        val start = Simulation.start(spec)
        val snap = aim(start)
        val log = InputLog().append(100, snap)
        val atFifty = Replay.run(spec, GameOptions(), log, 50)
        assertEquals(0, atFifty.photosTaken)
        val atHundred = Replay.run(spec, GameOptions(), log, 100)
        assertEquals(1, atHundred.photosTaken)
    }
}
