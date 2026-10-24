package pt.tiago.ducknight.core.bot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.tiago.ducknight.core.hash.StateHasher
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.level.StarThresholds

class BotRunnerTest {
    private val spec = LevelSpec.classic(
        seed = 401L, birds = 4, need = 2, stars = StarThresholds(1, 2),
    )

    @Test
    fun `the same seed gives the same run`() {
        val a = BotRunner.run(spec, profile = BotProfiles.MEDIUM, seed = 5L)
        val b = BotRunner.run(spec, profile = BotProfiles.MEDIUM, seed = 5L)
        assertEquals(StateHasher.hash(a.state), StateHasher.hash(b.state))
        assertEquals(a.summary, b.summary)
    }

    @Test
    fun `a different seed changes the run`() {
        val a = BotRunner.run(spec, profile = BotProfiles.MEDIUM, seed = 5L)
        val b = BotRunner.run(spec, profile = BotProfiles.MEDIUM, seed = 6L)
        assertNotEquals(StateHasher.hash(a.state), StateHasher.hash(b.state))
    }

    @Test
    fun `the perfect model completes a simple level`() {
        val run = BotRunner.run(spec, profile = BotProfiles.PERFECT, seed = 7L)
        assertTrue(run.summary?.passed == true)
        assertTrue(run.stars >= 1)
    }

    @Test
    fun `the run stops when the round ends`() {
        val run = BotRunner.run(spec, profile = BotProfiles.PERFECT, seed = 7L)
        assertEquals(pt.tiago.ducknight.core.engine.RoundPhase.FINISHED, run.state.phase)
        assertTrue(run.ticks < 20_000)
    }

    @Test
    fun `a bot run carries its profile and seed`() {
        val run = BotRunner.run(spec, profile = BotProfiles.EASY, seed = 8L)
        assertEquals("EASY_PLAYER", run.profile.id)
        assertEquals(8L, run.seed)
    }
}

class CalibrationTest {
    @Test
    fun `quantiles use the sorted sample`() {
        val sorted = listOf(10, 20, 30, 40, 50).sorted()
        assertEquals(20, Calibration.quantile(sorted, 35))
        assertEquals(40, Calibration.quantile(sorted, 70))
    }

    @Test
    fun `an empty sample has zero thresholds`() {
        assertEquals(0, Calibration.quantile(emptyList(), 35))
        assertEquals(0, Calibration.quantile(emptyList(), 70))
    }

    @Test
    fun `calibration returns a report for every requested run`() {
        val spec = LevelSpec.classic(
            seed = 402L, birds = 2, need = 1, stars = StarThresholds(1, 2),
        )
        val report = Calibration.run(spec, BotProfiles.PERFECT, runs = 5, seed = 9L)
        assertEquals(5, report.runs)
        assertEquals(5, report.successes)
        assertEquals(1.0, report.successRate, 0.0001)
        assertEquals(0, report.failures)
        assertTrue(report.p70 >= report.p35)
    }

    @Test
    fun `every human model can be measured`() {
        val spec = LevelSpec.classic(
            seed = 403L, birds = 1, need = 1, stars = StarThresholds(1, 2),
        )
        for (profile in BotProfiles.humans) {
            val report = Calibration.run(spec, profile, runs = 3, seed = 10L)
            assertEquals(3, report.runs)
            assertEquals(profile.id, report.profileId)
        }
    }

    @Test
    fun `the witness finder returns a three star run on a gentle level`() {
        val spec = LevelSpec.classic(
            seed = 404L, birds = 1, need = 1, stars = StarThresholds(1, 2),
        )
        val witness = WitnessFinder.find(spec, attempts = 5)
        assertTrue(witness != null)
        assertEquals(3, witness!!.stars)
    }

    @Test
    fun `the perfect model finds a three star witness on a normal classic level`() {
        val spec = LevelSpec.classic(
            seed = 406L, birds = 10, need = 6, stars = StarThresholds(4_000, 6_000),
        )
        val witness = WitnessFinder.find(spec, attempts = 5)
        assertTrue(witness != null)
        assertEquals(3, witness!!.stars)
        assertTrue(witness.score >= 6_000)
    }

    @Test
    fun `the witness can be replayed exactly`() {
        val spec = LevelSpec.classic(
            seed = 405L, birds = 1, need = 1, stars = StarThresholds(1, 2),
        )
        val witness = WitnessFinder.find(spec, attempts = 5)
        assertTrue(witness != null)
        val again = BotRunner.run(
            spec.copy(attemptIndex = witness!!.attemptIndex),
            profile = BotProfiles.PERFECT,
            seed = witness.seed,
        )
        assertEquals(StateHasher.hash(witness.result.state), StateHasher.hash(again.state))
    }
}
