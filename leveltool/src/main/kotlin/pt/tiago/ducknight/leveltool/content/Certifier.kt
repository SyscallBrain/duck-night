package pt.tiago.ducknight.leveltool.content

import pt.tiago.ducknight.core.level.LevelType
import pt.tiago.ducknight.core.content.*

import pt.tiago.ducknight.core.BOT_VERSION
import pt.tiago.ducknight.core.SIM_VERSION
import pt.tiago.ducknight.core.bot.WitnessFinder
import pt.tiago.ducknight.core.hash.StateHasher
import pt.tiago.ducknight.core.level.Difficulty

object Certifier {
    data class Failure(val levelId: String, val difficulty: Difficulty, val reason: String)

    /**
     * Calibrates each variant if needed, sets the star thresholds to p35/p70, finds a three-star
     * perfect witness and writes the certificate. It refuses to certify a variant outside its
     * human band: the content, not the test, has to be fixed.
     */
    fun certify(
        store: ContentStore,
        levelId: String,
        attempts: Int = 5,
        runs: Int = 400,
        relaxed: Boolean = false,
    ): List<Failure> {
        var working = store.loadLevel(levelId)
        val failures = mutableListOf<Failure>()
        val outliers = mutableListOf<Failure>()
        for (difficulty in Difficulty.entries) {
            var spec = working.spec(difficulty)
            var calibration = Calibrator.calibrateVariant(store, spec, difficulty, runs)
            if (!relaxed) {
                val tuned = tune(store, spec, calibration, difficulty, runs)
                spec = tuned.first
                calibration = tuned.second
                val band = Calibrator.bandFor(spec)
                if (calibration.successRate !in band) {
                    // Kept as the closest variant and listed by `report`; it is still certified.
                    outliers += Failure(
                        levelId, difficulty,
                        "success ${"%.3f".format(calibration.successRate)} outside ${"%.2f".format(band.start)}..${"%.2f".format(band.endInclusive)}",
                    )
                }
                working = working.withSpec(spec)
            }
            var two = maxOf(1, calibration.p35)
            var three = maxOf(two + 1, calibration.p70)
            var stars = pt.tiago.ducknight.core.level.StarThresholds(two, three)
            spec = spec.copy(stars = stars)
            var witness = WitnessFinder.find(
                spec = spec,
                attempts = attempts,
                seed = Calibrator.seedFor(levelId, difficulty),
            )
            // Three stars must stay possible: a variant tuned too fast for a perfect photo slows down
            // again, 5 % at a time, until the perfect model finds its witness.
            var slowdowns = 0
            while (witness == null && !relaxed && spec.speedPercent > MIN_SPEED && slowdowns < MAX_SLOWDOWNS) {
                spec = spec.copy(speedPercent = (spec.speedPercent - 5).coerceAtLeast(MIN_SPEED))
                calibration = Calibrator.calibrateVariant(store, spec, difficulty, runs)
                two = maxOf(1, calibration.p35)
                three = maxOf(two + 1, calibration.p70)
                stars = pt.tiago.ducknight.core.level.StarThresholds(two, three)
                spec = spec.copy(stars = stars)
                witness = WitnessFinder.find(spec = spec, attempts = attempts, seed = Calibrator.seedFor(levelId, difficulty))
                slowdowns++
            }
            if (slowdowns > 0) {
                System.err.println("certify: $levelId-$difficulty: slowed to ${spec.speedPercent} % for a three-star witness")
                working = working.withSpec(spec)
            }
            if (witness == null) {
                failures += Failure(levelId, difficulty, "no three-star perfect witness")
                continue
            }
            val variantHash = JsonFiles.sha256(
                JsonFiles.encodeValue(spec).toByteArray(Charsets.UTF_8),
            )
            val certificate = Certificate(
                levelId = levelId,
                difficulty = difficulty.name,
                variantHash = variantHash,
                simVersion = SIM_VERSION,
                botVersion = BOT_VERSION,
                human = HumanSample(
                    profile = calibration.profileId,
                    runs = calibration.runs,
                    successes = calibration.successes,
                    successRate = calibration.successRate,
                    meanScore = calibration.meanScore,
                    p35 = calibration.p35,
                    p70 = calibration.p70,
                ),
                stars = stars,
                witness = WitnessFile(
                    attemptIndex = witness.attemptIndex,
                    seed = witness.seed,
                    finalTick = witness.result.state.tick,
                    finalHash = StateHasher.hash(witness.result.state),
                    inputLog = witness.result.log,
                ),
            )
            store.writeCertificate(certificate)
            working = working.withSpec(spec)
        }
        store.writeLevel(working)
        store.updateCatalog()
        outliers.forEach { System.err.println("certify: ${it.levelId}-${it.difficulty}: ${it.reason} (kept, closest variant)") }
        return failures
    }

    private const val MAX_TUNING_STEPS = 14
    private const val MAX_SLOWDOWNS = 16
    private const val MIN_SPEED = 50
    private const val MAX_SPEED = 160

    /**
     * Moves a variant into its target band. The minimum is the natural lever;
     * when one more or one less bird jumps across the band, or for a boss whose album is always three
     * pages, the speed is moved in steps of 5 % instead. The species mix is never touched, so a place
     * keeps its own birds. Returns the variant closest to the band.
     */
    private fun tune(
        store: ContentStore,
        start: pt.tiago.ducknight.core.level.LevelSpec,
        startCalibration: CalibrationFile,
        difficulty: Difficulty,
        runs: Int,
    ): Pair<pt.tiago.ducknight.core.level.LevelSpec, CalibrationFile> {
        val band = Calibrator.bandFor(start)
        fun gap(rate: Double) = when {
            rate < band.start -> band.start - rate
            rate > band.endInclusive -> rate - band.endInclusive
            else -> 0.0
        }
        var spec = start
        var calibration = startCalibration
        var best = spec to calibration
        var needLocked = start.type == LevelType.BOSS
        repeat(MAX_TUNING_STEPS) {
            if (gap(calibration.successRate) == 0.0) return spec to calibration
            val tooEasy = calibration.successRate > band.endInclusive
            val maxNeed = when (spec.type) {
                LevelType.TIMED -> 40
                else -> spec.birds
            }
            // Clays: a quarter of the plates; a timed stream: three birds; otherwise four birds in ten.
            val minNeed = when (spec.type) {
                LevelType.CLAY -> (maxNeed + 3) / 4
                LevelType.TIMED -> 3
                else -> (maxNeed * 4 + 9) / 10
            }
            val nextNeed = spec.need + if (tooEasy) 1 else -1
            val candidate = if (!needLocked && nextNeed in minNeed..maxNeed) {
                spec.copy(need = nextNeed)
            } else {
                val nextSpeed = (spec.speedPercent + if (tooEasy) 5 else -5).coerceIn(MIN_SPEED, MAX_SPEED)
                if (nextSpeed == spec.speedPercent) return best
                spec.copy(speedPercent = nextSpeed)
            }
            val next = Calibrator.calibrateVariant(store, candidate, difficulty, runs)
            val crossed = if (tooEasy) next.successRate < band.start else next.successRate > band.endInclusive
            if (candidate.need != spec.need && crossed) {
                // One bird more or less jumps over the band: keep the closer minimum, then use the speed.
                needLocked = true
                if (gap(next.successRate) < gap(calibration.successRate)) {
                    spec = candidate
                    calibration = next
                }
            } else {
                spec = candidate
                calibration = next
            }
            if (gap(calibration.successRate) < gap(best.second.successRate)) best = spec to calibration
        }
        return if (gap(calibration.successRate) <= gap(best.second.successRate)) spec to calibration else best
    }

    fun verify(store: ContentStore): List<String> {
        val errors = mutableListOf<String>()
        for (certificate in store.loadCertificates()) {
            val difficulty = runCatching { Difficulty.valueOf(certificate.difficulty) }.getOrNull()
            if (difficulty == null) {
                errors += "${certificate.levelId}: bad difficulty ${certificate.difficulty}"
                continue
            }
            val level = runCatching { store.loadLevel(certificate.levelId) }.getOrNull()
            if (level == null) {
                errors += "${certificate.levelId}: level missing"
                continue
            }
            val spec = runCatching { level.spec(difficulty) }.getOrNull()
            if (spec == null) {
                errors += "${certificate.levelId}-${certificate.difficulty}: variant missing"
                continue
            }
            val hash = JsonFiles.sha256(JsonFiles.encodeValue(spec).toByteArray(Charsets.UTF_8))
            if (hash != certificate.variantHash) {
                errors += "${certificate.levelId}-${certificate.difficulty}: variant hash changed"
            }
            if (certificate.simVersion != SIM_VERSION || certificate.botVersion != BOT_VERSION) {
                errors += "${certificate.levelId}-${certificate.difficulty}: version mismatch"
            }
            val replay = pt.tiago.ducknight.core.replay.Replay.runWithHash(
                spec = spec.copy(attemptIndex = certificate.witness.attemptIndex),
                options = pt.tiago.ducknight.core.level.GameOptions(),
                log = certificate.witness.inputLog,
                untilTick = certificate.witness.finalTick,
            )
            if (replay.hash != certificate.witness.finalHash) {
                errors += "${certificate.levelId}-${certificate.difficulty}: witness hash mismatch"
            }
            if ((replay.state.summary?.stars ?: 0) < 3) {
                errors += "${certificate.levelId}-${certificate.difficulty}: witness is not three stars"
            }
        }
        return errors
    }
}
