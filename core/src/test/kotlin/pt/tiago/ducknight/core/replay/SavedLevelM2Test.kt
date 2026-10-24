package pt.tiago.ducknight.core.replay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.tiago.ducknight.core.engine.InputAction
import pt.tiago.ducknight.core.hash.StateHasher
import pt.tiago.ducknight.core.level.GameOptions
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.level.Modifier
import pt.tiago.ducknight.core.level.TypeRules
import pt.tiago.ducknight.core.weather.CoverBox

class SavedLevelM2Test {
    @Test
    fun `an M2 level and its rules survive the save round trip`() {
        val spec = LevelSpec.film(
            seed = 5L, birds = 3, filmFrames = 6, need = 2,
            modifiers = setOf(Modifier.FOG, Modifier.GOLDEN),
            rules = TypeRules(filmFrames = 6, coverBoxes = listOf(CoverBox(1, 2, 3, 4))),
        )
        val log = InputLog().append(1, InputAction.Snap(2, 3))
        val saved = SavedLevel(spec, GameOptions(tolerancePercent = 120), 3, 42L, log)
        val decoded = SavedLevelCodec.decode(SavedLevelCodec.encode(saved))!!
        assertEquals(saved, decoded)
        assertEquals(spec.rules, decoded.spec.rules)
        assertEquals(spec.modifiers, decoded.spec.modifiers)
    }

    @Test
    fun `a double save restores the pair state`() {
        val spec = LevelSpec.double(seed = 6L, birds = 2, need = 1)
        val log = InputLog().append(1, InputAction.Snap(1, 1))
        val state = Replay.run(spec, GameOptions(), log, 50)
        val saved = SavedLevel(spec, GameOptions(), state.tick, StateHasher.hash(state), log)
        val outcome = ResumeService.restore(saved)
        assertTrue(outcome is ResumeOutcome.Restored)
        assertEquals(state, (outcome as ResumeOutcome.Restored).state)
    }

    @Test
    fun `a corrupt M2 save is rejected`() {
        val spec = LevelSpec.flock(seed = 7L, flocks = 1, flockSize = 3, need = 2)
        val saved = SavedLevel(spec, GameOptions(), 10, 123L, InputLog())
        assertTrue(ResumeService.restore(saved) is ResumeOutcome.HashMismatch)
    }

    @Test
    fun `the save codec rejects malformed json`() {
        assertEquals(null, SavedLevelCodec.decode("{"))
    }
}
