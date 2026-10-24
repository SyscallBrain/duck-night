package pt.tiago.ducknight.progress

/**
 * One achievement: earned once [progress] reaches [target]. The same rule drives the unlock and the
 * progress shown in the profile, so the two can never disagree.
 */
class AchievementRule(
    val id: String,
    val target: Int,
    /** For the boss achievements: the place whose boss level counts. */
    val place: Int? = null,
    val progress: (Profile) -> Int,
) {
    companion object {
        private val difficulties = listOf("EASY", "MEDIUM", "HARD")

        private fun bossDone(profile: Profile, place: Int, difficulty: String? = null): Boolean {
            val id = "p%02dl050".format(place)
            return (difficulty?.let { listOf(it) } ?: difficulties).any { profile.stars(id, it) > 0 }
        }

        private fun speciesPhotographed(profile: Profile): Int = profile.birds.count { it.value.photographed > 0 }

        private fun levelsWithStars(profile: Profile): Int = profile.levels.count { it.value.stars > 0 }

        private fun flag(value: Boolean): Int = if (value) 1 else 0

        val all: List<AchievementRule> = buildList {
            add(AchievementRule("first_photo", 1) { it.stats.birdsPhotographed })
            add(AchievementRule("first_good", 1) { it.stats.goodPhotos })
            add(AchievementRule("first_perfect", 1) { it.stats.perfectPhotos })
            add(AchievementRule("first_group", 1) { it.stats.groupPhotos })
            add(AchievementRule("group_3", 3) { it.stats.bestGroup })
            add(AchievementRule("group_5", 5) { it.stats.bestGroup })
            add(AchievementRule("no_flee", 1) { it.stats.noFleeRounds })
            add(AchievementRule("good_streak_10", 10) { it.stats.longestGoodStreak })
            for (place in 1..12) {
                add(AchievementRule("boss_p%02d".format(place), 1, place) { flag(bossDone(it, place)) })
            }
            add(AchievementRule("hard_places", 12) { profile -> (1..12).count { bossDone(profile, it, "HARD") } })
            add(AchievementRule("birds_10", 10, progress = ::speciesPhotographed))
            add(AchievementRule("birds_20", 20, progress = ::speciesPhotographed))
            add(AchievementRule("birds_30", 30, progress = ::speciesPhotographed))
            add(AchievementRule("book_complete", 48) { profile -> profile.birds.count { it.value.seen > 0 } })
            add(AchievementRule("perfect_25", 25) { it.stats.perfectPhotos })
            add(AchievementRule("perfect_100", 100) { it.stats.perfectPhotos })
            add(AchievementRule("clays_100", 100) { it.stats.platesBroken })
            add(AchievementRule("clays_500", 500) { it.stats.platesBroken })
            add(AchievementRule("gallery_40", 40) { it.records.galleryTargets })
            for (game in listOf("A", "B", "C")) {
                add(AchievementRule("arcade_${game.lowercase()}_10", 10) { profile ->
                    difficulties.maxOf { profile.records.arcadeRounds["$game:$it"] ?: 0 }
                })
            }
            add(AchievementRule("daily_7", 7) { it.dailyHistory.size })
            add(AchievementRule("daily_30", 30) { it.dailyHistory.size })
            add(AchievementRule("gyro_level", 1) { it.stats.gyroLevels })
            add(AchievementRule("trigger_level", 1) { it.stats.triggerLevels })
            add(AchievementRule("protect_20", 20) { it.stats.bestProtectStreak })
            add(AchievementRule("golden_p01", 1) { flag(it.stats.goldenVillage) })
            add(AchievementRule("duel_sweep", 1) { it.stats.duelSweeps })
            add(AchievementRule("ten_levels", 10, progress = ::levelsWithStars))
            add(AchievementRule("levels_100", 100, progress = ::levelsWithStars))
            add(AchievementRule("stars_100", 100) { it.totalStars })
            add(AchievementRule("stars_1000", 1_000) { it.totalStars })
            add(AchievementRule("three_stars_50", 50) { profile -> profile.levels.count { it.value.stars == 3 } })
        }
    }
}
