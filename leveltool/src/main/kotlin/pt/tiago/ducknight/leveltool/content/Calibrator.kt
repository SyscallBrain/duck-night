package pt.tiago.ducknight.leveltool.content

import pt.tiago.ducknight.core.level.LevelType
import pt.tiago.ducknight.core.content.*

import pt.tiago.ducknight.core.bot.BotProfiles
import pt.tiago.ducknight.core.bot.Calibration
import pt.tiago.ducknight.core.level.Difficulty
import pt.tiago.ducknight.core.random.SplitMix64

object Calibrator {
    fun profileFor(difficulty: Difficulty) = when (difficulty) {
        Difficulty.EASY -> BotProfiles.EASY
        Difficulty.MEDIUM -> BotProfiles.MEDIUM
        Difficulty.HARD -> BotProfiles.HARD
    }

    fun seedFor(levelId: String, difficulty: Difficulty): Long {
        var hash = 0x9E3779B97F4A7C15UL.toLong()
        for (char in levelId) hash = SplitMix64.mix(hash xor char.code.toLong())
        return SplitMix64.mix(hash xor difficulty.ordinal.toLong())
    }

    fun calibrate(store: ContentStore, level: LevelFile, runs: Int = 400): List<CalibrationFile> {
        val results = mutableListOf<CalibrationFile>()
        for (difficulty in Difficulty.entries) {
            results += calibrateVariant(store, level.spec(difficulty), difficulty, runs)
        }
        return results
    }

    fun calibrateVariant(
        store: ContentStore,
        spec: pt.tiago.ducknight.core.level.LevelSpec,
        difficulty: Difficulty,
        runs: Int = 400,
    ): CalibrationFile {
        val report = Calibration.run(
            spec = spec,
            profile = profileFor(difficulty),
            runs = runs,
            seed = seedFor(spec.id, difficulty),
            maxTicks = 8_000,
        )
        val file = CalibrationFile(
            levelId = spec.id,
            difficulty = difficulty.name,
            profileId = report.profileId,
            runs = report.runs,
            successes = report.successes,
            successRate = report.successRate,
            meanScore = report.meanScore,
            meanPhotos = report.meanPhotos,
            p35 = report.p35,
            p70 = report.p70,
        )
        JsonFiles.writeValue(store.calibrationPath(spec.id, difficulty), file)
        return file
    }

    fun calibrate(store: ContentStore, levelId: String, runs: Int = 400): List<CalibrationFile> {
        store.ensureScaffold(store.loadLevel(levelId).place)
        return calibrate(store, store.loadLevel(levelId), runs)
    }

    /** The normal target band, kept for callers without a level. */
    fun bandFor(difficulty: Difficulty): ClosedFloatingPointRange<Double> = when (difficulty) {
        Difficulty.EASY -> 0.90..0.98
        Difficulty.MEDIUM -> 0.72..0.88
        Difficulty.HARD -> 0.50..0.72
    }

    /**
     * The target band for one variant: the tutorials (place 1, levels 1-10)
     * are nearly always passed, levels 45-49 and the boss are harder than the rest of the place.
     */
    fun bandFor(difficulty: Difficulty, place: Int, ordinal: Int, type: LevelType): ClosedFloatingPointRange<Double> =
        when {
            type == LevelType.BOSS -> when (difficulty) {
                Difficulty.EASY -> 0.75..0.92
                Difficulty.MEDIUM -> 0.50..0.75
                Difficulty.HARD -> 0.30..0.55
            }
            place == 1 && ordinal <= 10 -> when (difficulty) {
                Difficulty.EASY -> 0.98..1.0
                Difficulty.MEDIUM -> 0.95..1.0
                Difficulty.HARD -> 0.85..1.0
            }
            ordinal in 45..49 -> when (difficulty) {
                Difficulty.EASY -> 0.85..0.95
                Difficulty.MEDIUM -> 0.62..0.80
                Difficulty.HARD -> 0.40..0.62
            }
            else -> bandFor(difficulty)
        }

    fun bandFor(spec: pt.tiago.ducknight.core.level.LevelSpec): ClosedFloatingPointRange<Double> =
        bandFor(spec.difficulty, spec.place, spec.ordinal, spec.type)

    fun inBand(difficulty: Difficulty, rate: Double): Boolean = rate in bandFor(difficulty)
}
