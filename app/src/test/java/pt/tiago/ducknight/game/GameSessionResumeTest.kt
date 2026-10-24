package pt.tiago.ducknight.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.tiago.ducknight.core.hash.StateHasher
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.level.StarThresholds
import pt.tiago.ducknight.data.SavedLevelStore
import java.nio.file.Files

class GameSessionResumeTest {
    @Test
    fun `a session restores at the same tick and paused`() {
        val dir = Files.createTempDirectory("duck-session-resume").toFile()
        val store = SavedLevelStore(dir)
        val spec = LevelSpec.classic(
            seed = 11L, birds = 1, need = 1, stars = StarThresholds(1, 2),
        )
        val original = GameSession(spec, store = store)
        val bird = original.state.birds.single()
        original.queueSnap(
            pt.tiago.ducknight.core.fixed.toUnits(bird.x),
            pt.tiago.ducknight.core.fixed.toUnits(bird.y),
        )
        original.tick()
        original.save()
        val saved = store.load()
        assertTrue(saved != null)
        val restored = GameSession(spec, store = store, restored = saved)
        assertEquals(original.state.tick, restored.state.tick)
        assertEquals(StateHasher.hash(original.state), StateHasher.hash(restored.state))
        assertTrue(restored.paused)
    }

    @Test
    fun `a tampered save is discarded`() {
        val dir = Files.createTempDirectory("duck-session-bad").toFile()
        val store = SavedLevelStore(dir)
        val spec = LevelSpec.classic(seed = 12L, birds = 1, need = 1)
        val saved = pt.tiago.ducknight.core.replay.SavedLevel(
            spec = spec,
            options = pt.tiago.ducknight.core.level.GameOptions(),
            tick = 5,
            stateHash = 42L,
            log = pt.tiago.ducknight.core.replay.InputLog(),
        )
        store.save(saved)
        val session = GameSession(spec, store = store, restored = saved)
        assertEquals(0, session.state.tick)
        assertEquals(false, store.hasSaved())
    }
}
