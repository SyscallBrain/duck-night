package pt.tiago.ducknight.core.hash

import org.junit.Assert.assertEquals
import org.junit.Test
import pt.tiago.ducknight.core.engine.InputAction
import pt.tiago.ducknight.core.engine.Simulation
import pt.tiago.ducknight.core.fixed.toUnits
import pt.tiago.ducknight.core.level.Difficulty
import pt.tiago.ducknight.core.level.GameOptions
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.replay.InputLog
import pt.tiago.ducknight.core.replay.Replay

/**
 * Locked hashes for whole input sequences. They are the M1 determinism goldens; M2 extends the
 * set to every level type and modifier.
 */
class DeterminismGoldenTest {
    private fun snapAtStart(log: InputLog, spec: LevelSpec, tick: Int, offset: Int = 0): InputLog {
        val state = Simulation.start(spec)
        val bird = state.birds.single()
        return log.append(tick, InputAction.Snap(toUnits(bird.x) + offset, toUnits(bird.y)))
    }

    @Test
    fun `medium classic with one photo`() {
        val spec = LevelSpec.classic(seed = 1001L, birds = 3, need = 2)
        val log = snapAtStart(InputLog(), spec, 1)
        val hash = Replay.runWithHash(spec, GameOptions(), log, 500).hash
        assertEquals(7853525910975058253L, hash)
    }

    @Test
    fun `easy classic with three misses`() {
        val spec = LevelSpec.classic(seed = 1002L, birds = 2, need = 1, difficulty = Difficulty.EASY)
        val log = InputLog()
            .append(1, InputAction.Snap(1, 1))
            .append(2, InputAction.Snap(1, 1))
            .append(3, InputAction.Snap(1, 1))
        val hash = Replay.runWithHash(spec, GameOptions(), log, 600).hash
        assertEquals(-5253250503316114777L, hash)
    }

    @Test
    fun `hard classic without input`() {
        val spec = LevelSpec.classic(seed = 1003L, birds = 1, need = 1, difficulty = Difficulty.HARD)
        val hash = Replay.runWithHash(spec, GameOptions(), InputLog(), 200).hash
        assertEquals(3361737993718443095L, hash)
    }

    @Test
    fun `easy classic with aim assist`() {
        val spec = LevelSpec.classic(seed = 1004L, birds = 2, need = 1, difficulty = Difficulty.EASY)
        val log = snapAtStart(InputLog(), spec, 1, offset = 32)
        val hash = Replay.runWithHash(spec, GameOptions(aimAssist = true, tolerancePercent = 110), log, 500).hash
        assertEquals(-8366522710933936736L, hash)
    }

    @Test
    fun `medium classic with a full round of misses`() {
        val spec = LevelSpec.classic(seed = 1005L, birds = 3, need = 2)
        var log = InputLog()
        for (tick in listOf(1, 2, 3, 120, 121, 122, 240, 241, 242)) {
            log = log.append(tick, InputAction.Snap(1, 1))
        }
        val hash = Replay.runWithHash(spec, GameOptions(), log, 900).hash
        assertEquals(690076314621674069L, hash)
    }
}
