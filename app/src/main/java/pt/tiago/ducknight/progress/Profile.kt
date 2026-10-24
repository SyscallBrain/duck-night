package pt.tiago.ducknight.progress

import kotlinx.serialization.Serializable
import pt.tiago.ducknight.core.level.Difficulty

@Serializable
data class LevelProgress(
    val stars: Int = 0,
    val bestScore: Int = 0,
    val attempts: Int = 0,
)

@Serializable
data class PhotoRecord(
    val speciesId: String,
    val quality: Int,
    val wingPhase: Int,
    val relativeX: Int,
    val relativeY: Int,
    val place: Int,
    val theme: String,
    val timestamp: Long,
)

@Serializable
data class BirdRecord(
    val seen: Int = 0,
    val photographed: Int = 0,
    val bestQuality: Int = 0,
    val bestPhoto: PhotoRecord? = null,
)

@Serializable
data class BossRecord(
    val bestPhotos: List<PhotoRecord> = emptyList(),
    val bestScore: Int = 0,
)

@Serializable
data class DailyRecord(val date: String, val score: Int, val stars: Int)

@Serializable
data class Stats(
    val levelsPlayed: Int = 0,
    val photos: Int = 0,
    val birdsPhotographed: Int = 0,
    val goodPhotos: Int = 0,
    val perfectPhotos: Int = 0,
    val groupPhotos: Int = 0,
    val platesBroken: Int = 0,
    val playTicks: Int = 0,
    val gyroLevels: Int = 0,
    val triggerLevels: Int = 0,
    val bestQuality: Int = 0,
    val longestGoodStreak: Int = 0,
    val bestGroup: Int = 0,
    val noFleeRounds: Int = 0,
    val protectStreak: Int = 0,
    val bestProtectStreak: Int = 0,
    val duelSweeps: Int = 0,
    val goldenVillage: Boolean = false,
)

/** Best results of the modes outside the campaign, keyed by "GAME:DIFFICULTY" or by difficulty. */
@Serializable
data class Records(
    val arcadeRounds: Map<String, Int> = emptyMap(),
    val arcadeScores: Map<String, Int> = emptyMap(),
    val clayScores: Map<String, Int> = emptyMap(),
    val galleryScores: Map<String, Int> = emptyMap(),
    val galleryTargets: Int = 0,
)

@Serializable
data class Profile(
    val version: Int = 1,
    val levels: Map<String, LevelProgress> = emptyMap(),
    val birds: Map<String, BirdRecord> = emptyMap(),
    val bosses: Map<String, BossRecord> = emptyMap(),
    val achievements: Set<String> = emptySet(),
    val dogs: Set<String> = setOf("farrusco"),
    val selectedDog: String = "farrusco",
    val cameras: Set<String> = setOf("box"),
    val selectedCamera: String = "box",
    val viewfinders: Set<String> = setOf("corners"),
    val selectedViewfinder: String = "corners",
    val dailyHistory: List<DailyRecord> = emptyList(),
    val stats: Stats = Stats(),
    val records: Records = Records(),
) {
    val totalStars: Int get() = levels.values.sumOf { it.stars }

    fun levelKey(levelId: String, difficulty: String): String = "$levelId:$difficulty"

    fun progress(levelId: String, difficulty: String): LevelProgress =
        levels[levelKey(levelId, difficulty)] ?: LevelProgress()

    fun stars(levelId: String, difficulty: String): Int = progress(levelId, difficulty).stars

    fun isUnlocked(levelId: String, ordinal: Int, difficulty: String): Boolean {
        if (ordinal <= 1) return true
        // Place 1 currently owns every level; the previous level unlocks the next one.
        val previousId = levelId.dropLast(3) + "%03d".format(ordinal - 1)
        return stars(previousId, difficulty) >= 1
    }

    fun title(): PhotographerTitle = PhotographerTitle.forStars(totalStars)

    fun isPlaceUnlocked(place: Int, difficulty: String): Boolean {
        if (place <= 1) return true
        val previousBoss = "p%02dl050".format(place - 1)
        return stars(previousBoss, difficulty) >= 1
    }

    /**
     * A round outside the campaign (fair, arcade): it counts for the statistics, the bird book and
     * the achievements, but never for level stars or unlocks.
     */
    fun recordPlay(
        summary: pt.tiago.ducknight.core.engine.RoundResult,
        photos: List<PhotoRecord>,
        seenSpecies: List<String>,
        aimMode: String,
        round: RoundExtras = RoundExtras(),
    ): Profile = withPlay(summary, photos, seenSpecies, aimMode, round).withAchievements()

    private fun withPlay(
        summary: pt.tiago.ducknight.core.engine.RoundResult,
        photos: List<PhotoRecord>,
        seenSpecies: List<String>,
        aimMode: String,
        round: RoundExtras,
    ): Profile {
        val cleanRound = summary.platesBroken == 0 && summary.birds >= 10 && summary.fled == 0
        var next = copy(
            stats = stats.copy(
                levelsPlayed = stats.levelsPlayed + 1,
                photos = stats.photos + summary.photosTaken,
                birdsPhotographed = stats.birdsPhotographed + summary.photographed,
                goodPhotos = stats.goodPhotos + summary.goodPhotos,
                perfectPhotos = stats.perfectPhotos + summary.perfectPhotos,
                groupPhotos = stats.groupPhotos + summary.groupPhotos,
                platesBroken = stats.platesBroken + summary.platesBroken,
                playTicks = stats.playTicks + summary.durationTicks,
                gyroLevels = stats.gyroLevels + if (aimMode == "GYRO") 1 else 0,
                triggerLevels = stats.triggerLevels + if (aimMode == "AIM_TRIGGER") 1 else 0,
                bestQuality = maxOf(stats.bestQuality, summary.bestQuality),
                longestGoodStreak = maxOf(stats.longestGoodStreak, round.goodStreak),
                bestGroup = maxOf(stats.bestGroup, round.bestGroup),
                noFleeRounds = stats.noFleeRounds + if (cleanRound) 1 else 0,
                protectStreak = when {
                    !round.protectLevel -> stats.protectStreak
                    summary.protectedPhotos == 0 -> stats.protectStreak + 1
                    else -> 0
                },
                goldenVillage = stats.goldenVillage || photos.any { it.speciesId == "golden" && it.place == 1 },
            ),
        )
        next = next.copy(stats = next.stats.copy(bestProtectStreak = maxOf(next.stats.bestProtectStreak, next.stats.protectStreak)))
        for (species in seenSpecies.distinct()) {
            val record = next.birds[species] ?: BirdRecord()
            next = next.copy(birds = next.birds + (species to record.copy(seen = record.seen + 1)))
        }
        for (photo in photos) {
            val record = next.birds[photo.speciesId] ?: BirdRecord()
            val best = record.bestPhoto
            val better = best == null || photo.quality > best.quality
            next = next.copy(
                birds = next.birds + (photo.speciesId to record.copy(
                    photographed = record.photographed + 1,
                    bestQuality = maxOf(record.bestQuality, photo.quality),
                    bestPhoto = if (better) photo else best,
                )),
            )
        }
        return next
    }

    fun recordResult(
        levelId: String,
        ordinal: Int,
        place: Int,
        difficulty: String,
        stars: Int,
        score: Int,
        summary: pt.tiago.ducknight.core.engine.RoundResult,
        photos: List<PhotoRecord>,
        seenSpecies: List<String>,
        aimMode: String,
        round: RoundExtras = RoundExtras(),
    ): Profile {
        val key = levelKey(levelId, difficulty)
        val old = levels[key] ?: LevelProgress()
        var next = copy(
            levels = levels + (key to old.copy(
                stars = maxOf(old.stars, stars),
                bestScore = maxOf(old.bestScore, score),
                attempts = old.attempts + 1,
            )),
        ).withPlay(summary, photos, seenSpecies, aimMode, round)
        if (summary.bossPages > 0) {
            val bossKey = levelId
            val boss = next.bosses[bossKey] ?: BossRecord()
            val best = (boss.bestPhotos + photos).sortedByDescending { it.quality }.take(3)
            next = next.copy(bosses = next.bosses + (bossKey to boss.copy(
                bestPhotos = best,
                bestScore = maxOf(boss.bestScore, score),
            )))
        }
        next = next.copy(
            dogs = next.dogs + buildSet {
                add("farrusco")
                if (next.stats.perfectPhotos >= 25) add("pantufa")
                if (Difficulty.entries.any { next.stars("p06l050", it.name) > 0 }) add("tejo")
            },
            cameras = next.cameras + buildSet {
                add("box")
                if (next.title().index >= PhotographerTitle.THIRD.index) add("reflex")
                if (next.stats.groupPhotos >= 50) add("instant")
                if ((1..12).all { next.stars("p%02dl050".format(it), "HARD") > 0 }) add("telephoto")
            },
            viewfinders = next.viewfinders + buildSet {
                add("corners")
                if (next.title().index >= PhotographerTitle.FIRST.index) add("reticle")
                if (next.title().index >= PhotographerTitle.NATURE.index) add("circle")
                if (next.title().index >= PhotographerTitle.MASTER.index) add("thirds")
            },
        )
        return next.withAchievements()
    }

    /** Achievements are never taken back, even after the campaign is reset. */
    fun withAchievements(): Profile = copy(achievements = achievements + earnedAchievements())

    fun earnedAchievements(): Set<String> =
        AchievementRule.all.filter { it.progress(this) >= it.target }.map { it.id }.toSet()

    fun recordArcadeRun(game: String, difficulty: String, rounds: Int, score: Int): Profile {
        val key = "$game:$difficulty"
        return copy(
            records = records.copy(
                arcadeRounds = records.arcadeRounds + (key to maxOf(records.arcadeRounds[key] ?: 0, rounds)),
                arcadeScores = records.arcadeScores + (key to maxOf(records.arcadeScores[key] ?: 0, score)),
            ),
        ).withAchievements()
    }

    fun recordClay(difficulty: String, score: Int): Profile = copy(
        records = records.copy(clayScores = records.clayScores + (difficulty to maxOf(records.clayScores[difficulty] ?: 0, score))),
    ).withAchievements()

    fun recordGallery(difficulty: String, score: Int, targets: Int): Profile = copy(
        records = records.copy(
            galleryScores = records.galleryScores + (difficulty to maxOf(records.galleryScores[difficulty] ?: 0, score)),
            galleryTargets = maxOf(records.galleryTargets, targets),
        ),
    ).withAchievements()

    fun recordDuelSweep(): Profile = copy(stats = stats.copy(duelSweeps = stats.duelSweeps + 1)).withAchievements()

    fun resetCampaign(): Profile = copy(
        levels = emptyMap(),
        bosses = emptyMap(),
        dailyHistory = emptyList(),
        stats = Stats(),
    )

    /** Repairs values from an imported or older backup. */
    fun sanitized(): Profile {
        val cleanLevels = levels.mapValues { (_, value) ->
            value.copy(
                stars = value.stars.coerceIn(0, 3),
                bestScore = value.bestScore.coerceAtLeast(0),
                attempts = value.attempts.coerceAtLeast(0),
            )
        }
        val cleanBirds = birds.mapValues { (_, value) ->
            value.copy(
                seen = value.seen.coerceAtLeast(0),
                photographed = value.photographed.coerceAtLeast(0),
                bestQuality = value.bestQuality.coerceIn(0, 100),
            )
        }
        val cleanDogs = dogs + "farrusco"
        val cleanCameras = cameras + "box"
        val cleanViewfinders = viewfinders + "corners"
        return copy(
            version = 1,
            levels = cleanLevels,
            birds = cleanBirds,
            bosses = bosses.mapValues { (_, value) ->
                value.copy(bestPhotos = value.bestPhotos.take(3), bestScore = value.bestScore.coerceAtLeast(0))
            },
            dogs = cleanDogs,
            selectedDog = selectedDog.takeIf { it in cleanDogs } ?: "farrusco",
            cameras = cleanCameras,
            selectedCamera = selectedCamera.takeIf { it in cleanCameras } ?: "box",
            viewfinders = cleanViewfinders,
            selectedViewfinder = selectedViewfinder.takeIf { it in cleanViewfinders } ?: "corners",
            dailyHistory = dailyHistory.takeLast(270),
        )
    }
}

@Serializable
enum class PhotographerTitle(val threshold: Int) {
    APPRENTICE(0),
    AMATEUR(60),
    THIRD(200),
    SECOND(450),
    FIRST(800),
    FIELD(1_300),
    NATURE(2_000),
    REPORTER(2_900),
    MASTER(3_900),
    LEGEND(5_000),
    ;

    val index: Int get() = ordinal

    companion object {
        fun forStars(stars: Int): PhotographerTitle =
            entries.last { stars >= it.threshold }
    }
}

/** What a finished round adds beyond its summary, tracked by the session. */
data class RoundExtras(
    val bestGroup: Int = 0,
    val goodStreak: Int = 0,
    val protectLevel: Boolean = false,
)
