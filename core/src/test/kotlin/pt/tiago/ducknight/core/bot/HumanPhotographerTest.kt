package pt.tiago.ducknight.core.bot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.tiago.ducknight.core.engine.InputAction
import pt.tiago.ducknight.core.engine.Simulation
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.level.Modifier
import pt.tiago.ducknight.core.level.TypeRules
import pt.tiago.ducknight.core.weather.FogBank

class HumanPhotographerTest {
    private val spec = LevelSpec.classic(seed = 301L, birds = 2, need = 1)

    @Test
    fun `a human does not snap before the reaction time`() {
        val state = Simulation.start(spec)
        val bot = HumanPhotographer(BotProfiles.PERFECT, seed = 1L)
        var current = state
        repeat(9) {
            assertTrue(bot.actions(current).isEmpty())
            current = Simulation.step(current, emptyList()).state
        }
        var snapTick = -1
        repeat(10) {
            val actions = bot.actions(current)
            if (actions.any { it is InputAction.Snap }) snapTick = current.tick
            current = Simulation.step(current, actions).state
        }
        assertTrue("snap at $snapTick", snapTick >= 9)
    }

    @Test
    fun `the perfect model never photographs a protected bird when no target is visible`() {
        val protect = LevelSpec.protect(seed = 404L, birds = 1, protectedBirds = 2, need = 1)
        var state = Simulation.start(protect)
        var guard = 0
        while (state.birds.none { it.species.protected } && guard < 300) {
            state = Simulation.step(state, emptyList()).state
            guard++
        }
        assertTrue("no protected bird spawned", state.birds.any { it.species.protected })
        state = state.copy(birds = state.birds.filter { it.species.protected })
        val bot = HumanPhotographer(BotProfiles.PERFECT, seed = 5L)
        var current = state
        repeat(30) {
            val actions = bot.actions(current)
            assertTrue("perfect bot shot a protected bird", actions.none { it is InputAction.Snap })
            current = Simulation.step(current, actions).state
        }
    }

    @Test
    fun `a hidden bird does not exist for the bot`() {
        val hidden = spec.copy(
            modifiers = setOf(Modifier.FOG),
            rules = TypeRules(fogBanks = listOf(FogBank(190, 60))),
        )
        val state = Simulation.start(hidden)
        val bot = HumanPhotographer(BotProfiles.PERFECT, seed = 1L)
        assertTrue(bot.actions(state).isEmpty())
    }

    @Test
    fun `the perfect model has no aim error`() {
        val state = Simulation.start(spec)
        val bot = HumanPhotographer(BotProfiles.PERFECT, seed = 7L)
        var current = state
        var aim: InputAction.Snap? = null
        var targetX = 0
        var targetY = 0
        repeat(20) {
            val actions = bot.actions(current)
            val shot = actions.filterIsInstance<InputAction.Snap>().firstOrNull()
            if (shot != null) {
                aim = shot
                targetX = pt.tiago.ducknight.core.fixed.toUnits(current.birds.first().x)
                targetY = pt.tiago.ducknight.core.fixed.toUnits(current.birds.first().y)
            }
            current = Simulation.step(current, actions).state
        }
        val shot = aim!!
        // The perfect model predicts the whole reaction movement; at the snap the prediction
        // should be within one frame of the real bird.
        assertTrue(kotlin.math.abs(shot.x - targetX) <= 10)
        assertTrue(kotlin.math.abs(shot.y - targetY) <= 10)
    }

    @Test
    fun `the bot pulls the manual lever after the model delay`() {
        val manual = spec.copy(modifiers = setOf(Modifier.MANUAL_ADVANCE), rules = TypeRules())
        var state = Simulation.start(manual)
        val bot = HumanPhotographer(BotProfiles.HARD, seed = 2L)
        var actions = bot.actions(state)
        var guard = 0
        while (actions.none { it is InputAction.Snap } && guard < 40) {
            state = Simulation.step(state, actions).state
            actions = bot.actions(state)
            guard++
        }
        state = Simulation.step(state, actions).state
        assertEquals(true, state.extras?.needsAdvance)
        var advanced = false
        guard = 0
        while (!advanced && guard < 40) {
            val next = bot.actions(state)
            advanced = next.any { it is InputAction.Advance }
            state = Simulation.step(state, next).state
            guard++
        }
        assertTrue(advanced)
        assertEquals(false, state.extras?.needsAdvance)
    }

    @Test
    fun `the same seed gives the same bot decision`() {
        val state = Simulation.start(spec)
        val a = HumanPhotographer(BotProfiles.EASY, 99L)
        val b = HumanPhotographer(BotProfiles.EASY, 99L)
        assertEquals(a.actions(state), b.actions(state))
        var current = state
        repeat(30) { current = Simulation.step(current, a.actions(current)).state }
        var currentB = state
        repeat(30) { currentB = Simulation.step(currentB, b.actions(currentB)).state }
        assertEquals(current, currentB)
    }

    @Test
    fun `a finished round has no actions`() {
        val run = BotRunner.run(
            LevelSpec.classic(seed = 302L, birds = 1, need = 1),
            profile = BotProfiles.PERFECT,
            seed = 3L,
        )
        val bot = HumanPhotographer(BotProfiles.PERFECT, 3L)
        assertTrue(bot.actions(run.state).isEmpty())
    }
}
