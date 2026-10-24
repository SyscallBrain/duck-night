package pt.tiago.ducknight.core.hash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import pt.tiago.ducknight.core.engine.BossState
import pt.tiago.ducknight.core.engine.GameEvent
import pt.tiago.ducknight.core.engine.Simulation
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.level.Modifier
import pt.tiago.ducknight.core.level.TypeRules
import pt.tiago.ducknight.core.replay.InputLog

class StateHasherM2Test {
    private fun state(modifiers: Set<Modifier> = setOf(Modifier.WIND), rules: TypeRules = TypeRules()) =
        Simulation.start(LevelSpec.classic(seed = 5L, birds = 2, need = 1).copy(modifiers = modifiers, rules = rules))

    @Test
    fun `M2 extras change the hash`() {
        val withExtras = state()
        val without = withExtras.copy(extras = null)
        assertNotEquals(StateHasher.hash(withExtras), StateHasher.hash(without))
    }

    @Test
    fun `film left is part of the hash`() {
        val film = Simulation.start(LevelSpec.film(seed = 1L, birds = 2, filmFrames = 4, need = 1))
        assertNotEquals(StateHasher.hash(film), StateHasher.hash(film.copy(extras = film.extras?.copy(filmLeft = 2))))
    }

    @Test
    fun `the boss album is part of the hash`() {
        val boss = Simulation.start(LevelSpec.boss(seed = 1L))
        assertNotEquals(
            StateHasher.hash(boss),
            StateHasher.hash(boss.copy(extras = boss.extras?.copy(boss = BossState(phase = 2, pages = 1)))),
        )
    }

    @Test
    fun `the fog banks are part of the hash`() {
        val state = state(setOf(Modifier.FOG))
        assertNotEquals(
            StateHasher.hash(state),
            StateHasher.hash(state.copy(extras = state.extras?.copy(fogBanks = emptyList()))),
        )
    }

    @Test
    fun `the protected count is part of the hash`() {
        val state = Simulation.start(LevelSpec.protect(seed = 1L, birds = 1, protectedBirds = 1, need = 1))
        assertNotEquals(
            StateHasher.hash(state),
            StateHasher.hash(state.copy(extras = state.extras?.copy(protectedPhotoCount = 1))),
        )
    }

    @Test
    fun `the plate state is part of the hash`() {
        val clay = Simulation.start(LevelSpec.clay(seed = 1L, pairs = 1, need = 1))
        val broken = clay.copy(extras = clay.extras?.copy(clays = clay.extras!!.clays.map { it.copy(status = pt.tiago.ducknight.core.fair.ClayStatus.BROKEN) }))
        assertNotEquals(StateHasher.hash(clay), StateHasher.hash(broken))
    }

    @Test
    fun `the summary extension is part of the hash`() {
        val finished = Simulation.start(LevelSpec.clay(seed = 1L, pairs = 1, need = 0))
            .let { s -> s.runUntil(200) { it.phase == pt.tiago.ducknight.core.engine.RoundPhase.FINISHED }.first }
        val summary = finished.summary!!
        assertNotEquals(
            StateHasher.hash(finished),
            StateHasher.hash(finished.copy(summary = summary.copy(platesBroken = 1))),
        )
    }

    @Test
    fun `M2 states remain deterministic`() {
        val a = Simulation.start(LevelSpec.flock(seed = 9L, flocks = 1, flockSize = 3, need = 2))
        val b = Simulation.start(LevelSpec.flock(seed = 9L, flocks = 1, flockSize = 3, need = 2))
        assertEquals(StateHasher.hash(a), StateHasher.hash(b))
    }

    @Test
    fun `a replay with an M2 type matches the direct run`() {
        val spec = LevelSpec.film(seed = 4L, birds = 2, filmFrames = 4, need = 1)
        val log = InputLog().append(1, pt.tiago.ducknight.core.engine.InputAction.Snap(1, 1))
        var direct = Simulation.start(spec)
        direct = Simulation.step(direct, log.actionsAt(1)).state
        repeat(200) { direct = Simulation.step(direct, emptyList()).state }
        val replay = pt.tiago.ducknight.core.replay.Replay.run(spec, pt.tiago.ducknight.core.level.GameOptions(), log, 201)
        assertEquals(StateHasher.hash(direct), StateHasher.hash(replay))
    }
}

private fun pt.tiago.ducknight.core.engine.GameState.runUntil(
    max: Int,
    predicate: (pt.tiago.ducknight.core.engine.GameState) -> Boolean,
): Pair<pt.tiago.ducknight.core.engine.GameState, List<GameEvent>> {
    var state = this
    val events = mutableListOf<GameEvent>()
    var guard = 0
    while (!predicate(state) && guard < max) {
        val result = Simulation.step(state, emptyList())
        state = result.state
        events += result.events
        guard++
    }
    return state to events
}
