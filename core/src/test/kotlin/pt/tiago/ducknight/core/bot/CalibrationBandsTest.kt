package pt.tiago.ducknight.core.bot

import org.junit.Assert.assertTrue
import org.junit.Test
import pt.tiago.ducknight.core.level.Difficulty
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.level.SpeciesWeight
import pt.tiago.ducknight.core.level.StarThresholds

/**
 * The calibration bands must be attainable on a test place.
 *
 * The fixture is a normal level of ten birds; the minimum, the photos per bird (two for the hard
 * case, as in some FILM levels) and the share of fast scoters change per model (BOT_VERSION 2).
 */
class CalibrationBandsTest {
    private data class Case(
        val profile: BotProfile,
        val difficulty: Difficulty,
        val fastWeight: Int,
        val need: Int,
        val photos: Int,
        val minimum: Double,
        val maximum: Double,
    )

    private val cases = listOf(
        Case(BotProfiles.EASY, Difficulty.EASY, 10, 9, 3, 0.90, 0.98),
        Case(BotProfiles.MEDIUM, Difficulty.MEDIUM, 30, 9, 3, 0.72, 0.88),
        Case(BotProfiles.HARD, Difficulty.HARD, 65, 9, 2, 0.50, 0.72),
    )

    @Test
    fun `the three human models land inside their bands on the test place`() {
        for (case in cases) {
            val spec = LevelSpec.classic(
                seed = 9_001L,
                birds = 10,
                need = case.need,
                difficulty = case.difficulty,
                id = "calibration-classic",
                stars = StarThresholds(4_000, 6_000),
            ).copy(
                photosPerBird = case.photos,
                species = listOf(
                    SpeciesWeight("mallard", 100 - case.fastWeight),
                    SpeciesWeight("scoter", case.fastWeight),
                ),
            )
            val report = Calibration.run(spec, case.profile, runs = 100, seed = 0xCA11B)
            println(
                "BAND ${case.profile.id} rate=${"%.3f".format(report.successRate)} " +
                    "mean=${"%.1f".format(report.meanScore)} p35=${report.p35} p70=${report.p70}",
            )
            assertTrue(
                "${case.profile.id} ${report.successRate} outside ${case.minimum}..${case.maximum}",
                report.successRate >= case.minimum && report.successRate <= case.maximum,
            )
            assertTrue(report.p70 >= report.p35)
        }
    }
}
