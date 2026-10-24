package pt.tiago.ducknight.core.bot

import pt.tiago.ducknight.core.engine.GameState
import pt.tiago.ducknight.core.engine.RoundResult
import pt.tiago.ducknight.core.engine.Simulation
import pt.tiago.ducknight.core.level.GameOptions
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.random.SplitMix64

data class BotRun(
    val profile: BotProfile,
    val seed: Long,
    val state: GameState,
    val ticks: Int,
    /** Every action the model took, so a witness can be replayed by the certificate verifier. */
    val log: pt.tiago.ducknight.core.replay.InputLog = pt.tiago.ducknight.core.replay.InputLog(),
) {
    val summary: RoundResult? get() = state.summary
    val passed: Boolean get() = summary?.passed == true
    val stars: Int get() = summary?.stars ?: 0
}

/** Runs one model until the round ends or the tick budget is exhausted. */
object BotRunner {
    fun run(
        spec: LevelSpec,
        options: GameOptions = GameOptions(),
        profile: BotProfile = BotProfiles.MEDIUM,
        seed: Long = 0L,
        maxTicks: Int = 20_000,
    ): BotRun {
        var state = Simulation.start(spec, options)
        val photographer = HumanPhotographer(profile, seed)
        var log = pt.tiago.ducknight.core.replay.InputLog()
        var guard = 0
        while (state.phase != pt.tiago.ducknight.core.engine.RoundPhase.FINISHED && guard < maxTicks) {
            val actions = photographer.actions(state)
            for (action in actions) log = log.append(state.tick + 1, action)
            state = Simulation.step(state, actions).state
            guard++
        }
        return BotRun(profile, seed, state, guard, log)
    }
}

/** Calibration sample, measured on the real engine. */
data class CalibrationReport(
    val levelId: String,
    val profileId: String,
    val runs: Int,
    val successes: Int,
    val successRate: Double,
    val meanScore: Double,
    val p35: Int,
    val p70: Int,
    val meanPhotos: Double,
    val failures: Int,
) {
    /** Three-star threshold for a variant: 70th percentile of successful scores. */
    val threeStarThreshold: Int get() = p70

    /** Two-star threshold for a variant: 35th percentile of successful scores. */
    val twoStarThreshold: Int get() = p35
}

object Calibration {
    fun run(
        spec: LevelSpec,
        profile: BotProfile,
        runs: Int,
        seed: Long = 0xC0FFEE,
        maxTicks: Int = 6_000,
    ): CalibrationReport {
        require(runs > 0) { "At least one calibration run is required" }
        val scores = mutableListOf<Int>()
        var successes = 0
        var photos = 0
        val botSeed = SplitMix64(seed)
        for (attempt in 0 until runs) {
            val attemptSpec = spec.copy(attemptIndex = attempt)
            val result = BotRunner.run(
                attemptSpec,
                profile = profile,
                seed = botSeed.nextLong(),
                maxTicks = maxTicks,
            )
            if (result.passed) {
                successes++
                scores += result.summary?.score ?: 0
            }
            photos += result.summary?.photosTaken ?: 0
        }
        scores.sort()
        val meanScore = if (scores.isEmpty()) 0.0 else scores.average()
        return CalibrationReport(
            levelId = spec.id,
            profileId = profile.id,
            runs = runs,
            successes = successes,
            successRate = successes.toDouble() / runs,
            meanScore = meanScore,
            p35 = quantile(scores, 35),
            p70 = quantile(scores, 70),
            meanPhotos = photos.toDouble() / runs,
            failures = runs - successes,
        )
    }

    fun quantile(sorted: List<Int>, percentile: Int): Int {
        if (sorted.isEmpty()) return 0
        val index = ((percentile.toLong() * sorted.size + 99) / 100 - 1).toInt().coerceIn(0, sorted.size - 1)
        return sorted[index]
    }
}

data class Witness(
    val attemptIndex: Int,
    val seed: Long,
    val result: BotRun,
) {
    val score: Int get() = result.summary?.score ?: 0
    val stars: Int get() = result.stars
}

/** The perfect model searches a three-star witness. */
object WitnessFinder {
    fun find(
        spec: LevelSpec,
        attempts: Int = 5,
        seed: Long = 0x5EED,
        maxTicks: Int = 8_000,
    ): Witness? {
        val botSeed = SplitMix64(seed)
        for (attempt in 0 until attempts) {
            val attemptSpec = spec.copy(attemptIndex = attempt)
            val runSeed = botSeed.nextLong()
            val result = BotRunner.run(
                attemptSpec,
                profile = BotProfiles.PERFECT,
                seed = runSeed,
                maxTicks = maxTicks,
            )
            if (result.stars == 3) {
                return Witness(attempt, runSeed, result)
            }
        }
        return null
    }
}
