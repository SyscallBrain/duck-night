package pt.tiago.ducknight.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.tiago.ducknight.core.engine.InputAction
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.level.StarThresholds

class GameSessionTest {
    private fun spec() = LevelSpec.classic(
        seed = 7L, birds = 1, need = 1, stars = StarThresholds(1, 2),
    )

    @Test
    fun `a tap is queued and applied on the next tick`() {
        val session = GameSession(spec())
        val bird = session.state.birds.single()
        session.queueSnap(
            pt.tiago.ducknight.core.fixed.toUnits(bird.x),
            pt.tiago.ducknight.core.fixed.toUnits(bird.y),
        )
        assertEquals(0, session.state.photosTaken)
        session.tick()
        assertEquals(1, session.state.photosTaken)
        assertEquals(1, session.state.birdsPhotographed)
    }

    @Test
    fun `pause stops the ticks`() {
        val session = GameSession(spec())
        session.pause()
        val tick = session.state.tick
        session.tick()
        assertEquals(tick, session.state.tick)
        session.resume()
        session.tick()
        assertTrue(session.state.tick > tick)
    }

    @Test
    fun `advance is queued for manual film levels`() {
        val spec = spec().copy(
            modifiers = setOf(pt.tiago.ducknight.core.level.Modifier.MANUAL_ADVANCE),
            rules = pt.tiago.ducknight.core.level.TypeRules(),
        )
        val session = GameSession(spec)
        session.queueSnap(1, 1)
        session.tick()
        assertTrue(session.state.extras?.needsAdvance == true)
        session.queueAdvance()
        session.tick()
        assertEquals(false, session.state.extras?.needsAdvance)
    }

    @Test
    fun `restart resets the round`() {
        val session = GameSession(spec())
        session.queueSnap(1, 1)
        session.tick()
        session.restart()
        assertEquals(0, session.state.tick)
        assertEquals(0, session.state.photosTaken)
        assertEquals(false, session.paused)
    }

    @Test
    fun `the queued actions are part of the replay log`() {
        val session = GameSession(spec())
        session.queueSnap(1, 2)
        session.tick()
        // The session keeps its log private; a fresh replay of the same action must match.
        val replay = pt.tiago.ducknight.core.replay.Replay.run(
            spec = spec(),
            options = pt.tiago.ducknight.core.level.GameOptions(),
            log = pt.tiago.ducknight.core.replay.InputLog().append(1, InputAction.Snap(1, 2)),
            untilTick = 1,
        )
        assertEquals(session.state.photosTaken, replay.photosTaken)
    }
}
