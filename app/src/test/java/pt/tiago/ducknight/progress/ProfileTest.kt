package pt.tiago.ducknight.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.tiago.ducknight.core.engine.RoundResult

class ProfileTest {
    private fun summary(
        stars: Int = 2,
        score: Int = 5_000,
        photographed: Int = 1,
        bossPages: Int = 0,
    ) = RoundResult(
        passed = true,
        stars = stars,
        photographed = photographed,
        fled = 0,
        score = score,
        need = 1,
        birds = 1,
        photosTaken = 1,
        bestQuality = 80,
        okPhotos = 0,
        goodPhotos = 1,
        perfectPhotos = 0,
        groupPhotos = 0,
        durationTicks = 100,
        bossPages = bossPages,
    )

    private fun photo(species: String = "mallard", quality: Int = 80) = PhotoRecord(
        speciesId = species,
        quality = quality,
        wingPhase = 2048,
        relativeX = 0,
        relativeY = 0,
        place = 1,
        theme = "LAGOON",
        timestamp = 1L,
    )

    @Test
    fun `a result adds stars, best score and attempts`() {
        var profile = Profile()
        profile = profile.recordResult("p01l001", 1, 1, "EASY", 2, 5_000, summary(), listOf(photo()), listOf("mallard"), "TAP")
        assertEquals(2, profile.stars("p01l001", "EASY"))
        assertEquals(5_000, profile.progress("p01l001", "EASY").bestScore)
        assertEquals(1, profile.progress("p01l001", "EASY").attempts)
        assertEquals(1, profile.birds.getValue("mallard").photographed)
        assertEquals(80, profile.birds.getValue("mallard").bestQuality)
    }

    @Test
    fun `the next level unlocks only after one star`() {
        var profile = Profile()
        assertTrue(!profile.isUnlocked("p01l002", 2, "EASY"))
        profile = profile.recordResult("p01l001", 1, 1, "EASY", 1, 1_000, summary(stars = 1), listOf(photo()), listOf("mallard"), "TAP")
        assertTrue(profile.isUnlocked("p01l002", 2, "EASY"))
        assertTrue(!profile.isUnlocked("p01l002", 2, "HARD"))
    }

    @Test
    fun `the title follows total stars`() {
        var profile = Profile()
        assertEquals(PhotographerTitle.APPRENTICE, profile.title())
        profile = profile.copy(
            levels = mapOf(
                "a" to LevelProgress(stars = 3),
                "b" to LevelProgress(stars = 3),
                "c" to LevelProgress(stars = 3),
            ),
        )
        assertEquals(PhotographerTitle.APPRENTICE, profile.title())
        profile = profile.copy(levels = profile.levels + ("d" to LevelProgress(stars = 60)))
        assertEquals(PhotographerTitle.AMATEUR, profile.title())
    }

    @Test
    fun `achievements are earned from the statistics`() {
        val profile = Profile(
            stats = Stats(birdsPhotographed = 10, goodPhotos = 1),
            birds = (1..10).associate { "bird$it" to BirdRecord(photographed = 1) },
        )
        val earned = profile.earnedAchievements()
        assertTrue("first_good" in earned)
        assertTrue("birds_10" in earned)
    }

    @Test
    fun `a boss result fills the boss record`() {
        var profile = Profile()
        profile = profile.recordResult(
            "p01l050", 50, 1, "EASY", 3, 6_000,
            summary(stars = 3, score = 6_000, bossPages = 3),
            listOf(photo("great_duck", 92), photo("great_duck", 88), photo("great_duck", 80)),
            listOf("great_duck"), "TAP",
        )
        assertEquals(3, profile.bosses.getValue("p01l050").bestPhotos.size)
        assertTrue("boss_p01" in profile.achievements)
    }

    @Test
    fun `a place opens after its previous boss`() {
        var profile = Profile()
        assertTrue(profile.isPlaceUnlocked(1, "EASY"))
        assertTrue(!profile.isPlaceUnlocked(2, "EASY"))
        profile = profile.recordResult(
            "p01l050", 50, 1, "EASY", 3, 6_000,
            summary(stars = 3, score = 6_000, bossPages = 3),
            listOf(photo("great_duck", 90)), listOf("great_duck"), "TAP",
        )
        assertTrue(profile.isPlaceUnlocked(2, "EASY"))
        assertTrue(!profile.isPlaceUnlocked(3, "EASY"))
    }

    @Test
    fun `the achievements and bird book survive the campaign reset`() {
        val profile = Profile(
            levels = mapOf("p01l001:EASY" to LevelProgress(stars = 3)),
            birds = mapOf("mallard" to BirdRecord(photographed = 1, bestQuality = 80)),
            achievements = setOf("first_photo"),
            stats = Stats(levelsPlayed = 3),
        )
        val reset = profile.resetCampaign()
        assertTrue(reset.levels.isEmpty())
        assertEquals(1, reset.birds.getValue("mallard").photographed)
        assertTrue("first_photo" in reset.achievements)
    }

    @Test
    fun `a fair or arcade round counts for the statistics but not for the campaign`() {
        val plates = summary(stars = 3, photographed = 0).copy(platesBroken = 100)
        val profile = Profile().recordPlay(plates, emptyList(), emptyList(), "TAP")
        assertEquals(1, profile.stats.levelsPlayed)
        assertEquals(100, profile.stats.platesBroken)
        assertEquals(0, profile.totalStars)
        assertTrue(profile.levels.isEmpty())
        assertTrue("clays_100" in profile.achievements)
    }

    @Test
    fun `there are 45 achievements with unique ids`() {
        assertEquals(45, AchievementRule.all.size)
        assertEquals(45, AchievementRule.all.map { it.id }.toSet().size)
    }

    @Test
    fun `the round extras drive the group, streak and protect achievements`() {
        var profile = Profile()
        repeat(20) {
            profile = profile.recordResult(
                "p02l0%02d".format(it + 1), it + 1, 2, "EASY", 1, 1_000, summary(), emptyList(), emptyList(), "TAP",
                RoundExtras(bestGroup = 5, goodStreak = 10, protectLevel = true),
            )
        }
        assertTrue("group_5" in profile.achievements)
        assertTrue("good_streak_10" in profile.achievements)
        assertTrue("protect_20" in profile.achievements)
        assertTrue("ten_levels" in profile.achievements)
    }

    @Test
    fun `arcade records keep the best run per game and difficulty`() {
        var profile = Profile().recordArcadeRun("A", "MEDIUM", 10, 40_000)
        profile = profile.recordArcadeRun("A", "MEDIUM", 4, 90_000)
        assertEquals(10, profile.records.arcadeRounds.getValue("A:MEDIUM"))
        assertEquals(90_000, profile.records.arcadeScores.getValue("A:MEDIUM"))
        assertTrue("arcade_a_10" in profile.achievements)
        assertTrue("arcade_b_10" !in profile.achievements)
    }

    @Test
    fun `the Tejo unlocks with the boss of place 6`() {
        var profile = Profile().recordResult("p01l050", 50, 1, "EASY", 3, 6_000, summary(), emptyList(), emptyList(), "TAP")
        assertTrue("tejo" !in profile.dogs)
        profile = profile.recordResult("p06l050", 50, 6, "EASY", 1, 6_000, summary(), emptyList(), emptyList(), "TAP")
        assertTrue("tejo" in profile.dogs)
    }
}
