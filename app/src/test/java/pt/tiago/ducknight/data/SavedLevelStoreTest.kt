package pt.tiago.ducknight.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pt.tiago.ducknight.core.engine.InputAction
import pt.tiago.ducknight.core.level.GameOptions
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.replay.InputLog
import pt.tiago.ducknight.core.replay.SavedLevel
import java.nio.file.Files

class SavedLevelStoreTest {
    @Test
    fun `a saved level round trips`() {
        val dir = Files.createTempDirectory("duck-save").toFile()
        val store = SavedLevelStore(dir)
        val saved = SavedLevel(
            spec = LevelSpec.classic(seed = 3L, birds = 2, need = 1),
            options = GameOptions(tolerancePercent = 110),
            tick = 42,
            stateHash = 99L,
            log = InputLog().append(1, InputAction.Snap(2, 3)),
        )
        store.save(saved)
        assertEquals(saved, store.load())
        store.clear()
        assertNull(store.load())
    }

    @Test
    fun `the backup recovers a broken primary`() {
        val dir = Files.createTempDirectory("duck-save-bak").toFile()
        val store = SavedLevelStore(dir)
        val saved = SavedLevel(
            spec = LevelSpec.classic(seed = 4L, birds = 2, need = 1),
            options = GameOptions(),
            tick = 10,
            stateHash = 5L,
            log = InputLog(),
        )
        store.save(saved)
        dir.resolve("saved-level.json").writeText("{broken")
        assertEquals(saved, store.load())
    }

    @Test
    fun `a missing save is null`() {
        val dir = Files.createTempDirectory("duck-save-empty").toFile()
        assertNull(SavedLevelStore(dir).load())
    }

    @Test
    fun `a corrupt pair of files is discarded`() {
        val dir = Files.createTempDirectory("duck-save-corrupt").toFile()
        dir.resolve("saved-level.json").writeText("{")
        dir.resolve("saved-level.bak.json").writeText("}")
        val store = SavedLevelStore(dir)
        assertNull(store.load())
        assertEquals(false, store.hasSaved())
    }
}
