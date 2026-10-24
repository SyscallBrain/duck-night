package pt.tiago.ducknight.core.replay

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.tiago.ducknight.core.engine.InputAction
import pt.tiago.ducknight.core.level.Difficulty
import pt.tiago.ducknight.core.level.GameOptions
import pt.tiago.ducknight.core.level.LevelSpec

class InputLogTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun `an empty log has no actions`() {
        val log = InputLog()
        assertTrue(log.entries.isEmpty())
        assertTrue(log.actionsAt(1).isEmpty())
    }

    @Test
    fun `appending keeps the log ordered by tick`() {
        val log = InputLog()
            .append(5, InputAction.Snap(10, 20))
            .append(1, InputAction.Pause)
            .append(3, InputAction.Resume)
        assertEquals(listOf(1, 3, 5), log.entries.map { it.tick })
    }

    @Test
    fun `actions on the same tick keep their insertion order`() {
        val log = InputLog()
            .append(4, InputAction.Pause)
            .append(4, InputAction.Snap(1, 2))
            .append(4, InputAction.Resume)
        assertEquals(
            listOf(InputAction.Pause, InputAction.Snap(1, 2), InputAction.Resume),
            log.actionsAt(4),
        )
    }

    @Test
    fun `actionsAt returns only the requested tick`() {
        val log = InputLog()
            .append(1, InputAction.Snap(1, 2))
            .append(2, InputAction.Snap(3, 4))
        assertEquals(listOf(InputAction.Snap(1, 2)), log.actionsAt(1))
        assertEquals(listOf(InputAction.Snap(3, 4)), log.actionsAt(2))
        assertTrue(log.actionsAt(3).isEmpty())
    }

    @Test
    fun `negative ticks are rejected`() {
        assertTrue(runCatching { InputLog().append(-1, InputAction.Pause) }.isFailure)
    }

    @Test
    fun `the log serializes and deserializes every action`() {
        val log = InputLog()
            .append(1, InputAction.Snap(10, 20))
            .append(2, InputAction.Advance)
            .append(3, InputAction.AimAssist(2, -3))
            .append(4, InputAction.Pause)
            .append(5, InputAction.Resume)
        val text = json.encodeToString(InputLog.serializer(), log)
        val decoded = json.decodeFromString(InputLog.serializer(), text)
        assertEquals(log, decoded)
        assertTrue(text.contains("\"snap\""))
        assertTrue(text.contains("\"advance\""))
        assertTrue(text.contains("\"aim_assist\""))
    }

    @Test
    fun `encoding is stable`() {
        val log = InputLog().append(1, InputAction.Snap(10, 20))
        assertEquals(
            json.encodeToString(InputLog.serializer(), log),
            json.encodeToString(InputLog.serializer(), log),
        )
    }

    @Test
    fun `the log is built from an ordered list`() {
        val log = InputLog.of(
            InputLogEntry(8, InputAction.Pause),
            InputLogEntry(2, InputAction.Snap(1, 1)),
            InputLogEntry(4, InputAction.Resume),
        )
        assertEquals(listOf(2, 4, 8), log.entries.map { it.tick })
    }

    @Test
    fun `the log keeps a snapshot of the spec it belongs to`() {
        val saved = SavedLevel(
            spec = LevelSpec.classic(seed = 7L, difficulty = Difficulty.EASY),
            options = GameOptions(tolerancePercent = 110),
            tick = 10,
            stateHash = 1234L,
            log = InputLog().append(1, InputAction.Snap(2, 3)),
        )
        val text = json.encodeToString(SavedLevel.serializer(), saved)
        val decoded = json.decodeFromString(SavedLevel.serializer(), text)
        assertEquals(saved, decoded)
        assertEquals(110, decoded.options.tolerancePercent)
        assertEquals(Difficulty.EASY, decoded.spec.difficulty)
    }
}
